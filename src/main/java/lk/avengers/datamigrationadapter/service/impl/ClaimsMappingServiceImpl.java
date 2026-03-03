package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ClaimEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrAllClaimsEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrAllClaimsEntityRepository;
import lk.avengers.datamigrationadapter.service.ClaimsMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final static String PAID = "Paid";
    private final static String OUTSTANDING = "Outstanding";
    private final static String REJECTED = "Rejected";
    private final static String CLOSED = "Closed";

    private final static String DEATH = "death";

    private final static String SURRENDERED = "Surrended"; // spelling mistake is known
    private final static String DECEASED  = "Deceased";
    private final static String IN_FORCE  = "In Force";
    private final static String UNPAID  = "*Unpaid";

    private final static String ULV  = "ULV";


    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> mapClaimsData() {

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        try {
            List<MigrAllClaimsEntity> claimEntityList = new ArrayList<>();

            policyList.forEach(policyNo -> {
                String policyStatus = getStatus(policyNo);
                if (!isEligiblePolicyStatus(policyStatus)) {
                    log.info("Skipping policy {} due to status {}", policyNo, policyStatus);
                    return;
                }
                List<PaidClaimEntity> paidClaimEntityList =
                        paidClaimReportRepository.findByPolicyNo(policyNo);

                List<OutstandingClaimEntity> outstandingClaimEntityList =
                        outstandingClaimRepository.findByPolicyNumber(policyNo);

                List<RejectedClaimEntity> rejectedClaimEntityList =
                        rejectedClaimRepository.findByPolicy(policyNo.replaceFirst("/", ""));

                List<ClosedClaimReportEntity> closedClaimReportEntityList =
                        closedClaimsReportRepository.findByPolicyNo(policyNo);

                // ================= PAID =================
                paidClaimEntityList.forEach(paid -> {

                    boolean isPaid = paid.getTrnStatus().equalsIgnoreCase(PAID);
                    boolean isUnpaid = paid.getTrnStatus().equalsIgnoreCase(UNPAID);
                    boolean isInForce = paid.getPolicyStatus().equalsIgnoreCase(IN_FORCE);
                    boolean isSurrendered = paid.getPolicyStatus().equalsIgnoreCase(SURRENDERED);
                    boolean isDeceased = paid.getPolicyStatus().equalsIgnoreCase(DECEASED);
                    boolean isULV = policyNo.contains(ULV);

                    if ((isPaid || isUnpaid) && ((isInForce && isULV) || isSurrendered || isDeceased)) {
                        claimEntityList.add(
                                MigrAllClaimsEntity.builder()
                                        .clPolicyNo(policyNo)
                                        .clClaimNo(paid.getClaimOfficeNumber())
                                        .clClaimType(paid.getClaimType())
                                        .clDateofEvent(paid.getOccurredOn())
                                        .clDateofIntimation(paid.getDeclaredOn())
                                        .clPatientAdmitted(paid.getClaimedLifeAssured())
                                        .clCauseofDeath(paid.getClaimType().equalsIgnoreCase(DEATH) ? paid.getCauseOfClaim() : "")
                                        .clNatureofIllnuss(paid.getCauseOfClaim())
                                        .clTotalClaimAmount(BigDecimal.valueOf(paid.getOriginalClaimAmt()))
                                        .clTotalSettledAmount(BigDecimal.valueOf(paid.getTrnAmountCy()))
                                        .clClaimStatus(getClaimStatusCode(PAID))
                                        .clNameOftheHospital(paid.getPlaceOfClaim())
                                        .clDateofPayment(paid.getTrnDate())
                                        .clComments(paid.getClaimDescription())
                                        .clPolicyYear(paid.getUnderwritingYear())
                                        .build()
                        );
                    }
                });

                // ================= OUTSTANDING =================
                outstandingClaimEntityList.forEach(out -> claimEntityList.add(
                        MigrAllClaimsEntity.builder()
                                .clPolicyNo(policyNo)
                                .clClaimNo(out.getClaimOfficeNumber())
                                .clClaimType(out.getClaimType())
                                .clDateofEvent(out.getOccurredOn())
                                .clDateofIntimation(out.getDeclaredOn())
                                .clPatientAdmitted(out.getClaimedLifeAssured())
                                .clCauseofDeath(out.getClaimType().equalsIgnoreCase(DEATH) ? out.getCauseOfClaim() : "")
                                .clNatureofIllnuss(out.getCauseOfClaim())
                                .clTotalClaimAmount(BigDecimal.valueOf(out.getOriginalClaimAmount()))
                                .clTotalSettledAmount(BigDecimal.valueOf(out.getPaidAmount()))
                                .clClaimStatus(getClaimStatusCode(OUTSTANDING))
                                .clNameOftheHospital(out.getPlaceOfClaim())
                                .clComments(out.getClaimDescription())
                                .clPolicyYear(out.getUnderwritingYear())
                                .build()
                ));

                // ================= REJECTED =================
                rejectedClaimEntityList.forEach(rej -> {
                    Optional<DeclaredClaimEntity> intimation = declaredClaimReportRepository.
                            findFirstByPolicyNoAndClaimOfficeNumberOrderByIdDesc(policyNo, rej.getClaimNo());
                    String causeOfDeath = "";
                    String placeOfClaim = "";
                    if (intimation.isPresent()) {
                        causeOfDeath = intimation.get().getCauseOfClaims();
                        placeOfClaim = intimation.get().getPlaceOfClaims();
                    }
                    claimEntityList.add(
                            MigrAllClaimsEntity.builder()
                                    .clPolicyNo(policyNo)
                                    .clClaimNo(rej.getClaimNo())
                                    .clClaimType(rej.getClaimType())
                                    .clDateofEvent(rej.getOccurredDate())
                                    .clCauseofDeath(rej.getClaimType().equalsIgnoreCase(DEATH) ? causeOfDeath : "")
                                    .clDateofIntimation(rej.getDeclaredDate())
                                    .clPatientAdmitted(rej.getClaimantName())
                                    .clNatureofIllnuss(causeOfDeath)
                                    .clTotalClaimAmount(BigDecimal.valueOf(rej.getClaimedAmount()))
                                    .clTotalSettledAmount(BigDecimal.ZERO)
                                    .clClaimStatus(getClaimStatusCode(REJECTED))
                                    .clNameOftheHospital(placeOfClaim)
                                    .clComments(rej.getReason())
                                    .clPolicyYear(getPolicyYear(policyNo))
                                    .build()

                    );
                });

                // ================= CLOSED =================
                closedClaimReportEntityList.forEach(closed -> {
                    Optional<DeclaredClaimEntity> intimation = declaredClaimReportRepository.
                            findFirstByPolicyNoAndClaimOfficeNumberOrderByIdDesc(policyNo, closed.getClaimNo());
                    String causeOfDeath = "";
                    String placeOfClaim = "";
                    if(intimation.isPresent()){
                        causeOfDeath = intimation.get().getCauseOfClaims();
                        placeOfClaim = intimation.get().getPlaceOfClaims();
                    }
                    claimEntityList.add(
                            MigrAllClaimsEntity.builder()
                                    .clPolicyNo(policyNo)
                                    .clClaimNo(closed.getClaimNo())
                                    .clClaimType(closed.getTypeOfClaim())
                                    .clCauseofDeath(closed.getTypeOfClaim().equalsIgnoreCase(DEATH) ? causeOfDeath : "")
                                    .clDateofEvent(closed.getOccurrenceDate())
                                    .clDateofIntimation(closed.getDeclarationDate())
                                    .clPatientAdmitted(closed.getPolicyHolder())
                                    .clTotalClaimAmount(BigDecimal.valueOf(closed.getClaimAmount()))
                                    .clTotalSettledAmount(BigDecimal.ZERO)
                                    .clClaimStatus(getClaimStatusCode(CLOSED))
                                    .clNameOftheHospital(placeOfClaim)
                                    .clComments(closed.getRemarks())
                                    .clPolicyYear(getPolicyYear(policyNo))
                                    .build()
                    );
                });

            });

            migrAllClaimsEntityRepository.truncate();
            migrAllClaimsEntityRepository.saveAll(claimEntityList);

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .data(null)
                            .message(claimEntityList.size() + " claim data were mapped successfully")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    CommonResponseDTO.builder()
                            .data(null)
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build()
            );
        }
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

    private Integer getPolicyYear(String policyNo) {
        String productCode =
                (policyNo != null && policyNo.length() >= 3)
                        ? policyNo.replace("/", "").substring(0, 3)
                        : null;
        Integer number = (policyNo != null && policyNo.length() >= 3)
                ? Integer.valueOf(policyNo.replace("/", "").substring(3))
                : null;
        LocalDate inception = mainDataReportRepository
                .findFirstByProductCodeAndPolicyNo(productCode, number)
                .map(MainDataReportEntity::getInception)
                .or(() -> mainDataALHReportRepository
                        .findFirstByProductCodeAndPolicyNo(productCode, number)
                        .map(MainDataALHReportEntity::getInception))
                .or(() -> acpPolicyRepository
                        .findFirstByProductCodeAndPolicyNo(productCode, String.valueOf(number))
                        .map(ACPPolicyEntity::getInception))
                .orElse(null);

        if (inception == null) {
            return null;
        }
        return inception.getYear();
    }

    private String getStatus(String policyNo){
        String productCode =
                (policyNo != null && policyNo.length() >= 3)
                        ? policyNo.replace("/", "").substring(0, 3)
                        : null;
        Integer number = (policyNo != null && policyNo.length() >= 3)
                ? Integer.valueOf(policyNo.replace("/", "").substring(3))
                : null;
        return mainDataReportRepository
                .findFirstByProductCodeAndPolicyNo(productCode, number)
                .map(MainDataReportEntity::getStatus)
                .or(() -> mainDataALHReportRepository
                        .findFirstByProductCodeAndPolicyNo(productCode, number)
                        .map(MainDataALHReportEntity::getStatus))
                .or(() -> acpPolicyRepository
                        .findFirstByProductCodeAndPolicyNo(productCode, String.valueOf(number))
                        .map(ACPPolicyEntity::getStatus))
                .orElse(null);
    }

    private boolean isEligiblePolicyStatus(String status) {

        if (status == null) {
            return false;
        }

        return status.toLowerCase().contains("In Force".toLowerCase())
                || "Lapsed".equalsIgnoreCase(status);
    }
}


//
//if(!declaredClaimEntityList.isEmpty()){
//        declaredClaimEntityList.forEach(declaredClaimEntity -> {
//String claimNo = declaredClaimEntity.getClaimOfficeNumber();
//                        if(claimEntityList.stream().noneMatch(claimEntity -> claimEntity.getClaimNo().equalsIgnoreCase(claimNo))){
//        claimEntityList.add(
//        ClaimEntity.builder()
//                                            .policyNo(policyNo)
//                                            .claimNo(declaredClaimEntity.getClaimOfficeNumber())
//        .claimType(declaredClaimEntity.getClaimType())
//        .dateOfEvent(declaredClaimEntity.getOccurredDate())
//        .dateOfIntimation(declaredClaimEntity.getDeclaredDate())
//        .patientAdmitted(declaredClaimEntity.getClaimedLifeAssured())
//        .causeOfDeath(declaredClaimEntity.getCauseOfClaims())
//        .natureOfIllness(declaredClaimEntity.getCauseOfClaims())
//        .totalClaimAmount(BigDecimal.valueOf(declaredClaimEntity.getOriginalClaimAmt()))
//        .totalSettledAmount(BigDecimal.valueOf(declaredClaimEntity.getOriginalClaimAmt()))
//        .claimStatus(DECLARED)
//                                            .nameOfTheHospital(declaredClaimEntity.getPlaceOfClaims())
//        .dateOfPayment(declaredClaimEntity.getClaimClosedDate())
//        .comments(declaredClaimEntity.getClaimDescription())
//        .policyYear(declaredClaimEntity.getUnderwritingYear())
//        .build()
//                            );
//                                    }
//                                    });
//                                    }
