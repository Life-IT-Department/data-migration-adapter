package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrAllClaimsEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrAllClaimsEntityRepository;
import lk.avengers.datamigrationadapter.service.ClaimsMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsMappingServiceImpl implements ClaimsMappingService {

    private final PaidClaimReportRepository paidClaimReportRepository;
    private final OutstandingClaimRepository outstandingClaimRepository;
    private final RejectedClaimRepository rejectedClaimRepository;
    private final ClosedClaimsReportRepository closedClaimsReportRepository;
    private final MigrAllClaimsEntityRepository migrAllClaimsEntityRepository;
    private final DeclaredClaimReportRepository declaredClaimReportRepository;

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;

    private final MainExcelReader mainExcelReader;
    private final SharedFunction sharedFunction;

    private final static String PAID = "Paid";
    private final static String OUTSTANDING = "Outstanding";
    private final static String REJECTED = "Rejected";
    private final static String CLOSED = "Closed";

    private final static String DEATH = "death";

    private final static String SURRENDERED = "Surrended"; // spelling mistake is known
    private final static String DECEASED  = "Deceased";
    private final static String IN_FORCE  = "In Force";
    private final static String UNPAID  = "*Unpaid";
    private final static String SURRENDER_CASH_VALUE  = "Surrender Cash Value";

    private final static String ULV  = "ULV";


    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> mapClaimsData() {

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        List<PolicyNumberResponseDTO> extractedPolicies = policyList.stream()
                .map(this::extractPolicyNumberHelper)
                .toList();

        Set<String> productCodes = extractedPolicies.stream()
                .map(PolicyNumberResponseDTO::getProductCode)
                .collect(Collectors.toSet());

        Set<Integer> policyNos = extractedPolicies.stream()
                .map(PolicyNumberResponseDTO::getPolicyNo)
                .collect(Collectors.toSet());

        Map<String, MainDataReportEntity> mainDataMap =
                mainDataReportRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, MainDataALHReportEntity> alhMap =
                mainDataALHReportRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, ACPPolicyEntity> acpMap =
                acpPolicyRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        // 3️⃣ Bulk fetch claims
        Map<String, List<PaidClaimEntity>> paidMap =
                paidClaimReportRepository.findByPolicyNoIn(policyList).stream()
                        .collect(Collectors.groupingBy(PaidClaimEntity::getPolicyNo));

        Map<String, List<OutstandingClaimEntity>> outMap =
                outstandingClaimRepository.findByPolicyNumberIn(policyList).stream()
                        .collect(Collectors.groupingBy(OutstandingClaimEntity::getPolicyNumber));

        Map<String, List<RejectedClaimEntity>> rejMap =
                rejectedClaimRepository.findByPolicyIn(policyList).stream()
                        .collect(Collectors.groupingBy(rej -> rej.getPolicy().replaceFirst("/", "")));

        Map<String, List<ClosedClaimReportEntity>> closedMap =
                closedClaimsReportRepository.findByPolicyNoIn(policyList).stream()
                        .collect(Collectors.groupingBy(ClosedClaimReportEntity::getPolicyNo));

        log.info("Preloading required data...");

        List<MigrAllClaimsEntity> claimEntityList = new ArrayList<>();

        for (String policyNo : policyList) {

            String key = sharedFunction.extractPolicyNumber(policyNo).getProductCode() + "/" + sharedFunction.extractPolicyNumber(policyNo).getPolicyNo();

            String policyStatus = Optional.ofNullable(mainDataMap.get(key))
                    .map(MainDataReportEntity::getStatus)
                    .or(() -> Optional.ofNullable(alhMap.get(key))
                            .map(MainDataALHReportEntity::getStatus))
                    .or(() -> Optional.ofNullable(acpMap.get(key))
                            .map(ACPPolicyEntity::getStatus))
                    .orElse(null);

            if (!sharedFunction.isEligiblePolicyStatus(policyStatus)) {
                log.info("Skipping policy {} due to status {}", policyNo, policyStatus);
                continue;
            }

            // Paid Claims
            claimEntityList.addAll(mapPaidClaims(paidMap.getOrDefault(policyNo, Collections.emptyList()), policyNo));

            // Outstanding Claims
            claimEntityList.addAll(mapOutstandingClaims(outMap.getOrDefault(policyNo, Collections.emptyList()), policyNo));

            // Rejected Claims
            claimEntityList.addAll(mapRejectedClaims(rejMap.getOrDefault(policyNo, Collections.emptyList()), policyNo,
                    mainDataMap, alhMap, acpMap));

            // Closed Claims
            claimEntityList.addAll(mapClosedClaims(closedMap.getOrDefault(policyNo, Collections.emptyList()), policyNo,
                    mainDataMap, alhMap, acpMap));
        }

        try {
            log.info("Truncating previous claims data...");
            migrAllClaimsEntityRepository.truncate();

            log.info("Saving {} claim records...", claimEntityList.size());
            saveInBatches(claimEntityList, migrAllClaimsEntityRepository);

            return ResponseEntity.ok(CommonResponseDTO.builder()
                    .data(null)
                    .message(claimEntityList.size() + " claim data were mapped successfully")
                    .status(HttpStatus.OK.toString())
                    .build());

        } catch (Exception e) {
            log.error("Error mapping claims data: ", e);
            return ResponseEntity.internalServerError()
                    .body(CommonResponseDTO.builder()
                            .data(null)
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build());
        }
    }

    // ====================== CLAIM MAPPING METHODS ======================

    private List<MigrAllClaimsEntity> mapPaidClaims(List<PaidClaimEntity> paidClaims, String policyNo) {
        List<MigrAllClaimsEntity> result = new ArrayList<>();

        for (PaidClaimEntity paid : paidClaims) {
            if (!isEligiblePaidClaim(policyNo, paid)) continue;

            boolean isPaid = PAID.equalsIgnoreCase(paid.getTrnStatus());
            boolean isUnpaid = UNPAID.equalsIgnoreCase(paid.getTrnStatus());

            if (!(isPaid || isUnpaid)) continue;

            Optional<MigrAllClaimsEntity> previous = result.stream()
                    .filter(pre -> pre.getClClaimNo().equalsIgnoreCase(paid.getClaimOfficeNumber()) &&
                            pre.getClClaimType().equalsIgnoreCase(paid.getClaimType()) &&
                            pre.getClClaimStatus().equalsIgnoreCase(getClaimStatusCode(PAID)))
                    .findFirst();

            if (previous.isPresent()) {
                MigrAllClaimsEntity prev = previous.get();
                prev.setClTotalSettledAmount(prev.getClTotalSettledAmount()
                        .add(BigDecimal.valueOf(paid.getTrnAmountCy())));
            } else {
                result.add(MigrAllClaimsEntity.builder()
                        .clPolicyNo(policyNo)
                        .clClaimNo(paid.getClaimOfficeNumber())
                        .clClaimType(paid.getClaimType())
                        .clDateofEvent(paid.getOccurredOn())
                        .clDateofIntimation(paid.getDeclaredOn())
                        .clPatientAdmitted(paid.getClaimedLifeAssured())
                        .clCauseofDeath(DEATH.equalsIgnoreCase(paid.getClaimType()) ? paid.getCauseOfClaim() : "")
                        .clNatureofIllnuss(paid.getCauseOfClaim())
                        .clTotalClaimAmount(BigDecimal.valueOf(paid.getOriginalClaimAmt()))
                        .clTotalSettledAmount(BigDecimal.valueOf(paid.getTrnAmountCy()))
                        .clClaimStatus(getClaimStatusCode(PAID))
                        .clNameOftheHospital(paid.getPlaceOfClaim())
                        .clDateofPayment(paid.getTrnDate())
                        .clComments(paid.getClaimDescription())
                        .clPolicyYear(paid.getUnderwritingYear())
                        .clChildId(paid.getChildIdentification())
                        .build());
            }
        }

        return result;
    }

    private List<MigrAllClaimsEntity> mapOutstandingClaims(List<OutstandingClaimEntity> claims, String policyNo) {
        return claims.stream()
                .map(out -> MigrAllClaimsEntity.builder()
                        .clPolicyNo(policyNo)
                        .clClaimNo(out.getClaimOfficeNumber())
                        .clClaimType(out.getClaimType())
                        .clDateofEvent(out.getOccurredOn())
                        .clDateofIntimation(out.getDeclaredOn())
                        .clPatientAdmitted(out.getClaimedLifeAssured())
                        .clCauseofDeath(DEATH.equalsIgnoreCase(out.getClaimType()) ? out.getCauseOfClaim() : "")
                        .clNatureofIllnuss(out.getCauseOfClaim())
                        .clTotalClaimAmount(BigDecimal.valueOf(out.getOriginalClaimAmount()))
                        .clTotalSettledAmount(BigDecimal.valueOf(out.getPaidAmount()))
                        .clClaimStatus(getClaimStatusCode(OUTSTANDING))
                        .clNameOftheHospital(out.getPlaceOfClaim())
                        .clComments(out.getClaimDescription())
                        .clPolicyYear(out.getUnderwritingYear())
                        .clChildId(out.getChildIdentification())
                        .build())
                .toList();
    }

    private List<MigrAllClaimsEntity> mapRejectedClaims(List<RejectedClaimEntity> claims,
                                                        String policyNo,
                                                        Map<String, MainDataReportEntity> mainDataMap,
                                                        Map<String, MainDataALHReportEntity> alhMap,
                                                        Map<String, ACPPolicyEntity> acpMap) {
        return claims.stream()
                .map(rej -> {
                    Optional<DeclaredClaimEntity> intimation =
                            declaredClaimReportRepository.findFirstByPolicyNoAndClaimOfficeNumberOrderByIdDesc(policyNo, rej.getClaimNo());

                    String causeOfDeath = intimation.map(DeclaredClaimEntity::getCauseOfClaims).orElse("");
                    String placeOfClaim = intimation.map(DeclaredClaimEntity::getPlaceOfClaims).orElse("");

                    return MigrAllClaimsEntity.builder()
                            .clPolicyNo(policyNo)
                            .clClaimNo(rej.getClaimNo())
                            .clClaimType(rej.getClaimType())
                            .clDateofEvent(rej.getOccurredDate())
                            .clDateofIntimation(rej.getDeclaredDate())
                            .clPatientAdmitted(rej.getClaimedLifeAssured())
                            .clCauseofDeath(DEATH.equalsIgnoreCase(rej.getClaimType()) ? causeOfDeath : "")
                            .clNatureofIllnuss(causeOfDeath)
                            .clTotalClaimAmount(BigDecimal.valueOf(rej.getClaimedAmount()))
                            .clTotalSettledAmount(BigDecimal.ZERO)
                            .clClaimStatus(getClaimStatusCode(REJECTED))
                            .clNameOftheHospital(placeOfClaim)
                            .clComments(rej.getReason())
                            .clPolicyYear(getPolicyYear(policyNo, mainDataMap, alhMap, acpMap))
                            .clChildId(rej.getChildIdentification())
                            .build();
                }).toList();
    }

    private List<MigrAllClaimsEntity> mapClosedClaims(List<ClosedClaimReportEntity> claims,
                                                      String policyNo,
                                                      Map<String, MainDataReportEntity> mainDataMap,
                                                      Map<String, MainDataALHReportEntity> alhMap,
                                                      Map<String, ACPPolicyEntity> acpMap) {
        return claims.stream()
                .map(closed -> {
                    Optional<DeclaredClaimEntity> intimation =
                            declaredClaimReportRepository.findFirstByPolicyNoAndClaimOfficeNumberOrderByIdDesc(policyNo, closed.getClaimNo());

                    String causeOfDeath = intimation.map(DeclaredClaimEntity::getCauseOfClaims).orElse("");
                    String placeOfClaim = intimation.map(DeclaredClaimEntity::getPlaceOfClaims).orElse("");

                    return MigrAllClaimsEntity.builder()
                            .clPolicyNo(policyNo)
                            .clClaimNo(closed.getClaimNo())
                            .clClaimType(closed.getTypeOfClaim())
                            .clDateofEvent(closed.getOccurrenceDate())
                            .clDateofIntimation(closed.getDeclarationDate())
                            .clPatientAdmitted(closed.getClaimedLifeAssured())
                            .clCauseofDeath(DEATH.equalsIgnoreCase(closed.getTypeOfClaim()) ? causeOfDeath : "")
                            .clNatureofIllnuss(causeOfDeath)
                            .clTotalClaimAmount(BigDecimal.valueOf(closed.getClaimAmount()))
                            .clTotalSettledAmount(BigDecimal.ZERO)
                            .clClaimStatus(getClaimStatusCode(CLOSED))
                            .clNameOftheHospital(placeOfClaim)
                            .clComments(closed.getRemarks())
                            .clPolicyYear(getPolicyYear(policyNo, mainDataMap, alhMap, acpMap))
                            .clChildId(closed.getChildIdentification())
                            .build();
                }).toList();
    }

    private String getClaimStatusCode(String claimStatus){
        switch (claimStatus) {
            case (PAID) -> {
                return "P";
            }
            case (REJECTED) -> {
                return "R";
            }
            case (CLOSED) -> {
                return "C";
            }
            case (OUTSTANDING) -> {
                return "O";
            }
            default -> {
                return "N";
            }
        }
    }

    private Integer getPolicyYear(String policyNo,
                                  Map<String, MainDataReportEntity> mainDataMap,
                                  Map<String, MainDataALHReportEntity> alhMap,
                                  Map<String, ACPPolicyEntity> acpMap) {

        PolicyNumberResponseDTO extracted = sharedFunction.extractPolicyNumber(policyNo);
        String key = extracted.getProductCode() + "/" + extracted.getPolicyNo();

        LocalDate inception = Optional.ofNullable(mainDataMap.get(key))
                .map(MainDataReportEntity::getInception)
                .or(() -> Optional.ofNullable(alhMap.get(key))
                        .map(MainDataALHReportEntity::getInception))
                .or(() -> Optional.ofNullable(acpMap.get(key))
                        .map(ACPPolicyEntity::getInception))
                .orElse(null);

        return inception != null ? inception.getYear() : null;
    }

    private boolean isEligiblePaidClaim(String policyNo, PaidClaimEntity paidClaimEntity){
        boolean isInForce = paidClaimEntity.getPolicyStatus().equalsIgnoreCase(IN_FORCE);
        boolean isSurrendered = paidClaimEntity.getPolicyStatus().equalsIgnoreCase(SURRENDERED);
        boolean isDeceased = paidClaimEntity.getPolicyStatus().equalsIgnoreCase(DECEASED);
        boolean isULV = policyNo.contains(ULV);
        boolean isSurrenderCashValue = paidClaimEntity.getClaimType().equalsIgnoreCase(SURRENDER_CASH_VALUE);

        return !isSurrenderCashValue ||
                (isULV ? (isInForce || isSurrendered || isDeceased)
                        : (isSurrendered || isDeceased));
    }

    private PolicyNumberResponseDTO extractPolicyNumberHelper (String policyRef) {
        return sharedFunction.extractPolicyNumber(policyRef);
    }

    private <T> void saveInBatches(List<T> list, JpaRepository<T, ?> repository) {
        int batchSize = 1000;
        int totalSize = list.size();
        int totalBatches = (int) Math.ceil((double) totalSize / batchSize);

        log.info("Starting batch save: totalRecords={}, batchSize={}, totalBatches={}, repository={}",
                totalSize, batchSize, totalBatches, repository.getClass().getSimpleName());

        for (int i = 0; i < list.size(); i += 1000) {
            int batchNumber = (i / batchSize) + 1;
            int end = Math.min(i + 1000, list.size());

            log.info("Saving batch {}/{} (records {} - {})",
                    batchNumber, totalBatches, i + 1, end);

            List<T> batch = list.subList(i, end);
            repository.saveAll(batch);
            repository.flush();
        }
        log.info("Completed batch save: totalRecords={}, repository={}",
                totalSize, repository.getClass().getSimpleName());
    }
}