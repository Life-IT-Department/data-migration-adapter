package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BankPinEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsPaid;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.BankPinRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.CashFlowReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PremiumDetailsRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPremiumsPaidRepository;
import lk.avengers.datamigrationadapter.service.PremiumsPaidMappingService;
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
public class PremiumsPaidMappingServiceImpl implements PremiumsPaidMappingService {

    private final PremiumDetailsRepository premiumDetailsRepository;
    private final CashFlowReportRepository cashFlowReportRepository;
    private final BankPinRepository bankPinRepository;
    private final MigrPremiumsPaidRepository migrPremiumsPaidRepository;

    private final MainExcelReader mainExcelReader;

    private static final String IN_COMING = "In Coming";

    @Override
    public ResponseEntity<CommonResponseDTO> mapPremiumsPaid() {

        log.info("MIGRATE PREMIUMS PAID STARTED");

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

            for (String policy : policyList) {

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
                    }
                }
            }
            log.info("TRUNCATING PREMIUMS PAID TABLE");
            migrPremiumsPaidRepository.truncate();
            migrPremiumsPaidRepository.saveAll(premiumsPaidList);

            log.info("MIGRATE PREMIUMS PAID COMPLETED - {} records saved",
                    premiumsPaidList.size());

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message(premiumsPaidList.size() + " premium paid data were mapped successfully")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {

            log.error("ERROR DURING PREMIUMS PAID MIGRATION", e);

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
}


record PolicyNoDto(String productCode, Integer policyNo){}
