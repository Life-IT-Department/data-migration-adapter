package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.OutstandingClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PaidClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.RejectedClaimEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.OutstandingClaimRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PaidClaimReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.RejectedClaimRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.ClaimEntityRepository;
import lk.avengers.datamigrationadapter.service.ClaimsMappingService;
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

    private final static List<String> policyList = List.of("ASP/888636", "SCL/1044031", "ULT/348672", "ULE/402719", "ULE/121830", "ULE/274795",
            "ASP/1082429", "SCL/1098458", "ULP/349597", "ASP/1052257", "ULI/756262", "UPR/378844", "UPS/279539", "ULI/809491", "ULP/406538",
            "ULE/400218", "ULE/398701", "ULE/197178", "ULV/714360", "ULV/700310", "UPS/428565", "ASP/1104355", "SCL/1104348", "ULP/198457",
            "ULI/828517", "SCL/900886", "SCL/1013747", "ULI/441816", "ULC/863654", "ULA/242859", "SCL/953141", "SCL/953703", "SCL/960278",
            "SCL/911347", "SCL/955195", "SCL/964197", "SCL/911537", "SCL/955690", "SCL/936039", "SCL/904391", "SCL/922831", "SCL/949701",
            "SCL/925552", "SCL/943654", "SCL/957498", "SCL/913475", "SCL/950410", "SCL/908277", "SCL/914200", "SCL/924779", "SCL/931139",
            "SCL/944652", "ULT/361790", "ULT/362384", "ULT/365627", "ULT/372227", "ULT/371856", "ULT/366419", "ULT/371518", "ULT/373738",
            "ULT/366641", "ULV/896787", "ULV/901215", "ULV/894709", "ULE/360909", "ULE/351064", "ULE/366096", "ULE/373381", "ULE/373878",
            "ULE/358945", "SCL/963207", "SCL/964239", "SCL/924845", "SCL/945246", "SCL/897066", "SCL/908210", "SCL/919498", "SCL/926923",
            "ULT/326371", "ULT/323410", "ULT/323436", "ULT/326355", "ULT/322214", "ULT/325084", "ULT/322370", "ULT/326835", "ULT/326298",
            "ULT/327387", "ULT/323527", "ULT/325217", "ULT/325464", "ULT/325498", "ULT/326959", "ULT/325704", "ULT/326124", "SCL/838805",
            "SCL/834408", "SCL/873695", "SCL/846311", "SCL/893719");

    private final static String PAID = "Paid";
    private final static String OUTSTANDING = "Outstanding";
    private final static String REJECTED = "Rejected";

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> mapClaimsData(){
        try{
            List<ClaimEntity> claimEntityList = new ArrayList<>();
            policyList.forEach(policyNo -> {

                List<PaidClaimEntity> paidClaimEntityList = paidClaimReportRepository.findByPolicyNo(policyNo);
                List<OutstandingClaimEntity> outstandingClaimEntityList = outstandingClaimRepository.findByPolicyNumber(policyNo);
                List<RejectedClaimEntity> rejectedClaimEntityList = rejectedClaimRepository.findByPolicy(policyNo.replaceFirst("/", ""));

                if(!paidClaimEntityList.isEmpty()){
                    paidClaimEntityList.forEach(paidClaimEntity -> {
                        claimEntityList.add(
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

                        );
                    });
                }

                if(!outstandingClaimEntityList.isEmpty()){
                    outstandingClaimEntityList.forEach(outstandingClaimEntity -> {
                        claimEntityList.add(
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
                        );
                    });
                }

                if(!rejectedClaimEntityList.isEmpty()){
                    rejectedClaimEntityList.forEach(rejectedClaimEntity -> {
                        claimEntityList.add(
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

                        );
                    });
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
