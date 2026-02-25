package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
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

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;

    private final MainExcelReader mainExcelReader;

    private final static String PAID = "Paid";
    private final static String OUTSTANDING = "Outstanding";
    private final static String REJECTED = "Rejected";
    private final static String CLOSED = "Closed";

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> mapClaimsData() {

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        try {
            List<MigrAllClaimsEntity> claimEntityList = new ArrayList<>();

            policyList.forEach(policyNo -> {

                List<PaidClaimEntity> paidClaimEntityList =
                        paidClaimReportRepository.findByPolicyNo(policyNo);

                List<OutstandingClaimEntity> outstandingClaimEntityList =
                        outstandingClaimRepository.findByPolicyNumber(policyNo);

                List<RejectedClaimEntity> rejectedClaimEntityList =
                        rejectedClaimRepository.findByPolicy(policyNo.replaceFirst("/", ""));

                List<ClosedClaimReportEntity> closedClaimReportEntityList =
                        closedClaimsReportRepository.findByPolicyNo(policyNo);

                // ================= PAID =================
                paidClaimEntityList.forEach(paid -> claimEntityList.add(
                        MigrAllClaimsEntity.builder()
                                .clPolicyNo(policyNo)
                                .clClaimNo(paid.getClaimOfficeNumber())
                                .clClaimType(paid.getClaimType())
                                .clDateofEvent(paid.getOccurredOn())
                                .clDateofIntimation(paid.getDeclaredOn())
                                .clPatientAdmitted(paid.getClaimantName())
                                .clCauseofDeath(paid.getCauseOfClaim())
                                .clNatureofIllnuss(paid.getCauseOfClaim())
                                .clTotalClaimAmount(BigDecimal.valueOf(paid.getOriginalClaimAmt()))
                                .clTotalSettledAmount(BigDecimal.valueOf(paid.getTrnAmountCy()))
                                .clClaimStatus(getClaimStatusCode(PAID))
                                .clNameOftheHospital(paid.getPlaceOfClaim())
                                .clDateofPayment(paid.getTrnDate())
                                .clComments(paid.getClaimDescription())
                                .clPolicyYear(paid.getUnderwritingYear())
                                .build()
                ));

                // ================= OUTSTANDING =================
                outstandingClaimEntityList.forEach(out -> claimEntityList.add(
                        MigrAllClaimsEntity.builder()
                                .clPolicyNo(policyNo)
                                .clClaimNo(out.getClaimOfficeNumber())
                                .clClaimType(out.getClaimType())
                                .clDateofEvent(out.getOccurredOn())
                                .clDateofIntimation(out.getDeclaredOn())
                                .clPatientAdmitted(out.getClaimedLifeAssured())
                                .clCauseofDeath(out.getCauseOfClaim())
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
                rejectedClaimEntityList.forEach(rej -> claimEntityList.add(
                        MigrAllClaimsEntity.builder()
                                .clPolicyNo(policyNo)
                                .clClaimNo(rej.getClaimNo())
                                .clClaimType(rej.getClaimType())
                                .clDateofEvent(rej.getOccurredDate())
                                .clDateofIntimation(rej.getDeclaredDate())
                                .clPatientAdmitted(rej.getClaimantName())
                                .clCauseofDeath(rej.getReason())
                                .clNatureofIllnuss(rej.getReason())
                                .clTotalClaimAmount(BigDecimal.valueOf(rej.getClaimedAmount()))
                                .clClaimStatus(getClaimStatusCode(REJECTED))
                                .clDateofPayment(rej.getRejectedDate())
                                .clComments(rej.getRider())
                                .clPolicyYear(getPolicyYear(policyNo))
                                .build()
                ));

                // ================= CLOSED =================
                closedClaimReportEntityList.forEach(closed -> claimEntityList.add(
                        MigrAllClaimsEntity.builder()
                                .clPolicyNo(policyNo)
                                .clClaimNo(closed.getClaimNo())
                                .clClaimType(closed.getTypeOfClaim())
                                .clDateofEvent(closed.getOccurrenceDate())
                                .clDateofIntimation(closed.getDeclarationDate())
                                .clPatientAdmitted(closed.getPolicyHolder())
                                .clTotalClaimAmount(BigDecimal.valueOf(closed.getClaimAmount()))
                                .clTotalSettledAmount(BigDecimal.valueOf(closed.getClaimAmount()))
                                .clClaimStatus(getClaimStatusCode(CLOSED))
                                .clComments(closed.getRemarks())
                                .clPolicyYear(getPolicyYear(policyNo))
                                .build()
                ));

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
}
