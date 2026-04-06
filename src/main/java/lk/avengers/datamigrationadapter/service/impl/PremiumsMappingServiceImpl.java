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
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
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
    private final SharedFunction sharedFunction;

    private static final String IN_COMING = "In Coming";
    private static final String PREMIUM = "Premium";
    private static final String DOWN_PAYMENT = "Down Payment";
    private static final String PREM = "PREM";
    private static final String DEPO = "DEPO";
    private static final String VLD = "VLD";
    private static final String CNL = "CNL";
    private static final String NO = "NO";

    @Override
    public ResponseEntity<CommonResponseDTO> mapPremiumsPaid() {

        log.info("MIGRATE PREMIUMS PAID & DUE STARTED");

        try {
            List<MigrPremiumsPaid> premiumsPaidList = new ArrayList<>();
            List<MigrPremiumsDue> premiumsDueList = new ArrayList<>();

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

            List<PolicyNoDto> extractedPolicies = policyList.stream()
                    .map(this::getProductCodeAndPolicyNo)
                    .toList();

            Set<String> productCodes = extractedPolicies.stream()
                    .map(PolicyNoDto::productCode)
                    .collect(Collectors.toSet());

            Set<Integer> policyNos = extractedPolicies.stream()
                    .map(PolicyNoDto::policyNo)
                    .collect(Collectors.toSet());

            Set<String> policyNosWithSlash = new HashSet<>(policyList);
            Set<String> policyNosWithoutSlash = policyList.stream()
                    .map(p -> p.replace("/", ""))
                    .collect(Collectors.toSet());

            List<PremiumDetailsEntity> allPremiumDetails =
                    premiumDetailsRepository.findFiltered(productCodes, policyNos);

            List<CashFlowReportEntity> allCashFlows =
                    cashFlowReportRepository.findByPolicyNosAndPaymentTypeBulk(
                            policyNosWithSlash,
                            policyNosWithoutSlash,
                            IN_COMING
                    );

            Map<String, List<PremiumDetailsEntity>> premiumMap =
                    allPremiumDetails.stream()
                            .collect(Collectors.groupingBy(
                                    e -> e.getProductCode() + "/" + e.getPolicyNo()
                            ));

            Map<String, List<CashFlowReportEntity>> cashFlowMap =
                    allCashFlows.stream()
                            .collect(Collectors.groupingBy(c -> {
                                String policy = c.getPolicyNo();
                                return policy.contains("/") ? policy :
                                        policy.substring(0, 3) + "/" + policy.substring(3);
                            }));

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

            for (String policy : policyList) {
                String policyStatus = getStatus(policy, mainDataMap, alhMap, acpMap);
                if (!sharedFunction.isEligiblePolicyStatus(policyStatus)) {
                    log.info("Skipping policy {} due to status {}", policy, policyStatus);
                    continue;
                }
                PolicyNoDto dto = getProductCodeAndPolicyNo(policy);

                String key = dto.productCode() + "/" + dto.policyNo();

                List<PremiumDetailsEntity> premiumDetails =
                        premiumMap.getOrDefault(key, Collections.emptyList());

                List<CashFlowReportEntity> cashFlows =
                        cashFlowMap.getOrDefault(policy, Collections.emptyList());

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
                                            .receiptId(cashFlow.getReceiptNo())
                                            .chequeNo(
                                                    cashFlow.getCheckNo() == null
                                                            ? ""
                                                            : String.valueOf(cashFlow.getCheckNo())
                                            )
                                            .bank(bank)
                                            .paymentDate(paidDate)
                                            .paidAmount(paidAmount)
                                            .receiptStatus(
                                                    NO.equalsIgnoreCase(cashFlow.getReceiptCancellation())
                                                            ? VLD
                                                            : CNL
                                            )
                                            .paymentType(
                                                    cashFlow.getDescription().contains(PREMIUM) ?
                                                            PREM
                                                            : (cashFlow.getDescription().contains(DOWN_PAYMENT) ? DEPO : "")
                                            )
                                            .paymentMode(getPaymentMode(cashFlow.getPaymentMode()))
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
                                        .paidUp(true)
                                        .paidUpDate(premium.getPaymentDate())
                                        .dueStatus(VLD)
                                        .rowCreatedOn(LocalDate.now())
                                        .build()
                        );
                    }
                }
            }
            log.info("TRUNCATING PREMIUMS PAID & DUE TABLE");
            migrPremiumsPaidRepository.truncate();
            migrPremiumsDueRepository.truncate();

            log.info("SAVING PREMIUM PAID IN BATCHES...");
            saveInBatches(premiumsPaidList, migrPremiumsPaidRepository);

            log.info("SAVING PREMIUM DUE IN BATCHES...");
            saveInBatches(premiumsDueList, migrPremiumsDueRepository);

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

    private String getStatus(String policyNo,
                             Map<String, MainDataReportEntity> mainDataMap,
                             Map<String, MainDataALHReportEntity> alhMap,
                             Map<String, ACPPolicyEntity> acpMap) {

        if (policyNo == null || policyNo.length() < 3) {
            return null;
        }

        String cleaned = policyNo.replace("/", "");
        String productCode = cleaned.substring(0, 3);
        String numberPart = cleaned.substring(3);

        String key = productCode + "/" + Integer.parseInt(numberPart);

        MainDataReportEntity main = mainDataMap.get(key);
        if (main != null) {
            return main.getStatus();
        }

        MainDataALHReportEntity alh = alhMap.get(key);
        if (alh != null) {
            return alh.getStatus();
        }

        ACPPolicyEntity acp = acpMap.get(key);
        if (acp != null) {
            return acp.getStatus();
        }
        return null;
    }

    private String getPaymentMode(String paymentMode){
        return switch (paymentMode) {
            case "CASH" -> "Cash";
            case "CHECK" -> "Cheque";
            case "CLEARING" -> "Clearing";
            case "IN ACCOUNT" -> "Direct Deposit";
            case "TRANSFER" -> "Transfer";
            default -> "";
        };
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

record PolicyNoDto(String productCode, Integer policyNo){}
