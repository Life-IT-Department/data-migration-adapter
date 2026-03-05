package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsDue;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsPaid;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPremiumsDueRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPremiumsPaidRepository;
import lk.avengers.datamigrationadapter.service.PremiumsMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PremiumsMappingServiceImpl implements PremiumsMappingService {

    private final PremiumDetailsRepository premiumDetailsRepository;
    private final CashFlowReportRepository cashFlowReportRepository;
    private final BankPinRepository bankPinRepository;
    private final MigrPremiumsPaidRepository migrPremiumsPaidRepository;
    private final MigrPremiumsDueRepository migrPremiumsDueRepository;

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;

    private final MainExcelReader mainExcelReader;

    private static final String IN_COMING = "In Coming";

    @Override
    public ResponseEntity<CommonResponseDTO> mapPremiumsPaid() {

        log.info("MIGRATE PREMIUMS PAID & DUE STARTED");

        try {

            List<BankPinEntity> bankPins = bankPinRepository.findAll();
            log.info("{} BANK PIN MAPPINGS LOADED", bankPins.size());

            Map<Integer, String> bankPinMap = bankPins.stream()
                    .collect(Collectors.toMap(
                            BankPinEntity::getPin,
                            BankPinEntity::getBank,
                            (existing, replacement) -> existing
                    ));

            List<String> policyList = mainExcelReader.readPolicyNumbers();
            log.info("{} POLICY NUMBERS READ FROM THE EXCEL SHEET", policyList.size());

            List<MigrPremiumsPaid> premiumsPaidList = new ArrayList<>();
            List<MigrPremiumsDue> premiumsDueList = new ArrayList<>();

            for (String policy : policyList) {
                String policyStatus = getStatus(policy);
                if (!isEligiblePolicyStatus(policyStatus)) {
                    log.info("Skipping policy {} due to status {}", policy, policyStatus);
                    continue;
                }
                PolicyNoDto dto = getProductCodeAndPolicyNo(policy);

                List<PremiumDetailsEntity> premiumDetails =
                        premiumDetailsRepository.findByPolicyNoAndProductCode(
                                dto.policyNo(),
                                dto.productCode()
                        );

                List<CashFlowReportEntity> cashFlows =
                        cashFlowReportRepository.findByPolicyNosAndPaymentType(
                                policy,
                                policy.replace("/", ""),
                                IN_COMING
                        );

                if(!premiumDetails.isEmpty()){
                    for (PremiumDetailsEntity premium : premiumDetails) {

                        LocalDate paidDate = premium.getPaymentDate();
                        BigDecimal paidAmount = premium.getModalPremiumAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(premium.getModalPremiumAmount());

                        Optional<CashFlowReportEntity> cashFlowOpt = cashFlows.stream()
                                .filter(cf -> {

                                    if (!policy.equalsIgnoreCase(cf.getPolicyNo())) {
                                        return false;
                                    }
                                    if (cf.getCheckNo() != null) {
                                        return cf.getOperationDate() != null
                                                && paidDate != null
                                                && YearMonth.from(cf.getOperationDate())
                                                .equals(YearMonth.from(paidDate));
                                    }
                                    return Objects.equals(cf.getOperationDate(), paidDate);
                                })
                                .findFirst();

                        if (cashFlowOpt.isEmpty()) {
                            continue;
                        }

                        CashFlowReportEntity cashFlow = cashFlowOpt.get();

                        String bank =  cashFlow.getCheckNo() != null ? cashFlow.getDrawnBank() : bankPinMap.getOrDefault(
                                cashFlow.getAccPin(),
                                "IMS-Direct"
                        );

                        if(cashFlow.getReceiptNo() != 0){
                            premiumsPaidList.add(
                                    MigrPremiumsPaid.builder()
                                            .policyNo(policy)
                                            .receiptId(String.valueOf(cashFlow.getReceiptNo()))
                                            .chequeNo(
                                                    cashFlow.getCheckNo() == null
                                                            ? ""
                                                            : String.valueOf(cashFlow.getCheckNo())
                                            )
                                            .bank(bank)
                                            .paymentDate(paidDate)
                                            .paidAmount(paidAmount)
                                            .receiptStatus(
                                                    "NO".equalsIgnoreCase(cashFlow.getReceiptCancellation())
                                                            ? "Active"
                                                            : "Cancelled"
                                            )
                                            .paymentType(cashFlow.getPaymentType())
                                            .paymentMode(cashFlow.getPaymentMode())
                                            .build()
                            );
                        }

                        premiumsDueList.add(
                                MigrPremiumsDue.builder()
                                        .policyNo(policy)
                                        .dueDate(premium.getPremiumDueDate())
                                        .period(getPeriod(premium.getFrequency()))
                                        .term(premium.getTerm())
                                        .dueAmount(BigDecimal.valueOf(premium.getModalPremiumAmount()))
                                        .paidUpDate(premium.getPaymentDate())
                                        .build()
                        );
                    }
                }
            }
            log.info("TRUNCATING PREMIUMS PAID & DUE TABLE");
            migrPremiumsPaidRepository.truncate();
            migrPremiumsDueRepository.truncate();

            migrPremiumsPaidRepository.saveAll(premiumsPaidList);
            migrPremiumsDueRepository.saveAll(premiumsDueList);

            log.info("MIGRATE PREMIUMS PAID COMPLETED - {} records saved",
                    premiumsPaidList.size());
            log.info("MIGRATE PREMIUMS DUE COMPLETED - {} records saved",
                    premiumsDueList.size());

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message(premiumsPaidList.size() + " premium paid & due data were mapped successfully")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {

            log.error("ERROR DURING PREMIUMS PAID & DUE MIGRATION", e);

            return ResponseEntity.internalServerError().body(
                    CommonResponseDTO.builder()
                            .message(e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build()
            );
        }
    }

    private PolicyNoDto getProductCodeAndPolicyNo(String policyNo){
        String productCode =
                (policyNo != null && policyNo.length() >= 3)
                        ? policyNo.replace("/", "").substring(0, 3)
                        : null;
        Integer number = (policyNo != null && policyNo.length() >= 3)
                ? Integer.valueOf(policyNo.replace("/", "").substring(3))
                : null;

        return new PolicyNoDto(productCode, number);
    }

    private Integer getPeriod(Integer frequency) {
        if (frequency == null) {
            return null;
        }

        return switch (frequency) {
            case 1, 5 -> 12;
            case 2 -> 6;
            case 3 -> 3;
            case 4 -> 1;
            default -> 0;
        };
    }

    private boolean isEligiblePolicyStatus(String status) {
        if (status == null) {
            return false;
        }
        return status.toLowerCase().contains("In Force".toLowerCase())
                || "Lapsed".equalsIgnoreCase(status);
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
}


record PolicyNoDto(String productCode, Integer policyNo){}
