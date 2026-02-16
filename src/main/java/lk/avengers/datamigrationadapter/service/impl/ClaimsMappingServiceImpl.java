package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.DeclaredClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.OutstandingClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PaidClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.RejectedClaimEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.DeclaredClaimReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.OutstandingClaimRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PaidClaimReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.RejectedClaimRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.ClaimEntityRepository;
import lk.avengers.datamigrationadapter.service.ClaimsMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsMappingServiceImpl implements ClaimsMappingService {

    private final PaidClaimReportRepository paidClaimReportRepository;
    private final OutstandingClaimRepository outstandingClaimRepository;
    private final RejectedClaimRepository rejectedClaimRepository;
    private final ClaimEntityRepository claimEntityRepository;
    private final DeclaredClaimReportRepository declaredClaimReportRepository;

    private final MainExcelReader mainExcelReader;

    private final static String PAID = "Paid";
    private final static String OUTSTANDING = "Outstanding";
    private final static String REJECTED = "Rejected";
    private final static String DECLARED = "Declared";
    private final static String CLOSED = "Closed";

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> mapClaimsData(){

        List<String> policyList = mainExcelReader.readPolicyNumbers();
        try{
            List<ClaimEntity> claimEntityList = new ArrayList<>();
            policyList.forEach(policyNo -> {

                List<PaidClaimEntity> paidClaimEntityList = paidClaimReportRepository.findByPolicyNo(policyNo);
                List<OutstandingClaimEntity> outstandingClaimEntityList = outstandingClaimRepository.findByPolicyNumber(policyNo);
                List<RejectedClaimEntity> rejectedClaimEntityList = rejectedClaimRepository.findByPolicy(policyNo.replaceFirst("/", ""));
                List<DeclaredClaimEntity> declaredClaimEntityList = declaredClaimReportRepository.findByPolicyNo(policyNo);

                if(!paidClaimEntityList.isEmpty()){
                    paidClaimEntityList.forEach(paidClaimEntity -> claimEntityList.add(
                            ClaimEntity.builder()
                                    .policyNo(policyNo)
                                    .claimNo(paidClaimEntity.getClaimOfficeNumber())
                                    .claimType(paidClaimEntity.getClaimType())
                                    .dateOfEvent(paidClaimEntity.getOccurredOn())
                                    .dateOfIntimation(paidClaimEntity.getDeclaredOn())
                                    .patientAdmitted(paidClaimEntity.getClaimantName())
                                    .causeOfDeath(paidClaimEntity.getCauseOfClaim())
                                    .natureOfIllness(paidClaimEntity.getCauseOfClaim())
                                    .totalClaimAmount(BigDecimal.valueOf(paidClaimEntity.getOriginalClaimAmt()))
                                    .totalSettledAmount(BigDecimal.valueOf(paidClaimEntity.getTrnAmountCy()))
                                    .claimStatus(PAID)
                                    .nameOfTheHospital(paidClaimEntity.getPlaceOfClaim())
                                    .dateOfPayment(paidClaimEntity.getTrnDate())
                                    .comments(paidClaimEntity.getClaimDescription())
                                    .policyYear(paidClaimEntity.getUnderwritingYear())
                                    .totalPreviousClaims(paidClaimEntity.getNoOfPreviousClaims())
                                    .totalAmountPreviousClaims(BigDecimal.valueOf(paidClaimEntity.getPreviousClaimsTotalSettlement()))
                                    .build()

                    ));
                }

                if(!outstandingClaimEntityList.isEmpty()){
                    outstandingClaimEntityList.forEach(outstandingClaimEntity -> claimEntityList.add(
                            ClaimEntity.builder()
                                    .policyNo(policyNo)
                                    .claimNo(outstandingClaimEntity.getClaimOfficeNumber())
                                    .claimType(outstandingClaimEntity.getClaimType())
                                    .dateOfEvent(outstandingClaimEntity.getOccurredOn())
                                    .dateOfIntimation(outstandingClaimEntity.getDeclaredOn())
                                    .patientAdmitted(outstandingClaimEntity.getClaimedLifeAssured())
                                    .causeOfDeath(outstandingClaimEntity.getCauseOfClaim())
                                    .natureOfIllness(outstandingClaimEntity.getCauseOfClaim())
                                    .totalClaimAmount(BigDecimal.valueOf(outstandingClaimEntity.getOriginalClaimAmount()))
                                    .totalSettledAmount(BigDecimal.valueOf(outstandingClaimEntity.getPaidAmount()))
                                    .claimStatus(OUTSTANDING)
                                    .nameOfTheHospital(outstandingClaimEntity.getPlaceOfClaim())
                                    .comments(outstandingClaimEntity.getClaimDescription())
                                    .policyYear(outstandingClaimEntity.getUnderwritingYear())
                                    .build()
                    ));
                }

                if(!rejectedClaimEntityList.isEmpty()){
                    rejectedClaimEntityList.forEach(rejectedClaimEntity -> claimEntityList.add(
                            ClaimEntity.builder()
                                    .policyNo(policyNo)
                                    .claimType(rejectedClaimEntity.getClaimType())
                                    .dateOfEvent(rejectedClaimEntity.getOccurredDate())
                                    .dateOfIntimation(rejectedClaimEntity.getDeclaredDate())
                                    .patientAdmitted(rejectedClaimEntity.getClaimantName())
                                    .causeOfDeath(rejectedClaimEntity.getReason())
                                    .natureOfIllness(rejectedClaimEntity.getReason())
                                    .totalClaimAmount(BigDecimal.valueOf(rejectedClaimEntity.getClaimedAmount()))
                                    .claimStatus(REJECTED)
                                    .dateOfPayment(rejectedClaimEntity.getRejectedDate())
                                    .comments(rejectedClaimEntity.getRider())
                                    .rider(rejectedClaimEntity.getRider())
                                    .build()

                    ));
                }

                if(!declaredClaimEntityList.isEmpty()){
                    declaredClaimEntityList.forEach(declaredClaimEntity -> claimEntityList.add(
                            ClaimEntity.builder()
                                    .policyNo(policyNo)
                                    .claimNo(declaredClaimEntity.getClaimOfficeNumber())
                                    .claimType(declaredClaimEntity.getClaimType())
                                    .dateOfEvent(declaredClaimEntity.getOccurredDate())
                                    .dateOfIntimation(declaredClaimEntity.getDeclaredDate())
                                    .patientAdmitted(declaredClaimEntity.getClaimedLifeAssured())
                                    .causeOfDeath(declaredClaimEntity.getCauseOfClaims())
                                    .natureOfIllness(declaredClaimEntity.getCauseOfClaims())
                                    .totalClaimAmount(BigDecimal.valueOf(declaredClaimEntity.getOriginalClaimAmt()))
                                    .totalSettledAmount(BigDecimal.valueOf(declaredClaimEntity.getOriginalClaimAmt()))
                                    .claimStatus(DECLARED)
                                    .nameOfTheHospital(declaredClaimEntity.getPlaceOfClaims())
                                    .dateOfPayment(declaredClaimEntity.getClaimClosedDate())
                                    .comments(declaredClaimEntity.getClaimDescription())
                                    .policyYear(declaredClaimEntity.getUnderwritingYear())
                                    .build()
                    ));
                }
            });
            claimEntityRepository.saveAll(claimEntityList);
            return ResponseEntity.ok(CommonResponseDTO.builder()
                    .data(null)
                    .message(claimEntityList.size() + " claim data were mapped successfully")
                    .status(HttpStatus.OK.toString())
                    .build());

        } catch (Exception e) {
            CommonResponseDTO.builder()
                    .data(null)
                    .message(e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                    .build();

            return ResponseEntity.internalServerError().body(CommonResponseDTO.builder()
                    .data(null)
                    .message(e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                    .build());
        }
    }
}
