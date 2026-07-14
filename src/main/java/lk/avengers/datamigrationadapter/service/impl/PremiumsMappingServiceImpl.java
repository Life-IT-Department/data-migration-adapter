package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsDue;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsPaid;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PremiumExtraFields;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPremiumsDueRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPremiumsPaidRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PremiumsExtraRepository;
import lk.avengers.datamigrationadapter.service.PremiumsMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final PremiumsExtraRepository premiumsExtraRepository;
    private final PolicyListRepository policyListRepository;
    private final ContactDetailRepository contactDetailRepository;

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


    Map<String, List<PremiumDetailsEntity>> premiumMap;
    Map<String, MainDataALHReportEntity> alhMap;

    @Override
    @Transactional(readOnly = true, transactionManager = "reportPlatformTransactionManager")
    public ResponseEntity<CommonResponseDTO> mapPremiumsPaid() {

        log.info("MIGRATE PREMIUMS PAID & DUE STARTED");

        int batchSize = 500;

        List<MigrPremiumsPaid> batchPaid = new ArrayList<>(batchSize);
        List<MigrPremiumsDue> batchDue = new ArrayList<>(batchSize);

        try {
            log.info("Truncating premium history tables");
            migrPremiumsPaidRepository.truncate();
            migrPremiumsDueRepository.truncate();
            log.info("Tables were truncated");

            // 🔹 Read policies
            List<String> policyList = mainExcelReader.readPolicyNumbers();

            // 🔹 Extract product codes & numbers
            List<PolicyNoDto> extractedPolicies = policyList.stream()
                    .map(this::getProductCodeAndPolicyNo)
                    .toList();

            Set<String> productCodes = extractedPolicies.stream()
                    .map(PolicyNoDto::productCode)
                    .collect(Collectors.toSet());

            Set<Integer> policyNos = extractedPolicies.stream()
                    .map(PolicyNoDto::policyNo)
                    .collect(Collectors.toSet());

            // 🔹 Load supporting data
            premiumMap =
                    premiumDetailsRepository.findFiltered(productCodes, policyNos)
                            .stream()
                            .collect(Collectors.groupingBy(e -> e.getProductCode() + "/" + e.getPolicyNo()));

            for(String policyNo : policyList){
                PolicyNoDto dto = getProductCodeAndPolicyNo(policyNo);
                String key = dto.productCode() + "/" + dto.policyNo();
                List<PremiumDetailsEntity> premiums =
                        premiumMap.get(key);

                if(premiums != null && !premiums.isEmpty()){
                    for (PremiumDetailsEntity premium : premiums) {

                        LocalDate paidDate = premium.getPaymentDate();
                        BigDecimal paidAmount = premium.getModalPremiumAmount() == null
                                ? BigDecimal.ZERO
                                : BigDecimal.valueOf(premium.getModalPremiumAmount());

                        // 🔹 Paid
                        batchPaid.add(MigrPremiumsPaid.builder()
                                .policyNo(policyNo)
                                .paymentDate(paidDate)
                                .paidAmount(paidAmount)
                                .receiptStatus(VLD)
                                .paymentType(PREM)
                                .paymentMode("Transfer")
                                .build());
                        // 🔹 Due
                        batchDue.add(MigrPremiumsDue.builder()
                                .policyNo(policyNo)
                                .dueDate(premium.getPremiumDueDate())
                                .period(getPeriod(premium.getFrequency()))
                                .term(premium.getTerm())
                                .dueAmount(BigDecimal.valueOf(premium.getModalPremiumAmount()))
                                .paidUp(true)
                                .paidUpDate(premium.getPaymentDate())
                                .dueStatus(VLD)
                                .rowCreatedOn(LocalDate.now())
                                .build());

                        // 🔥 Flush batches
                        if (batchPaid.size() >= batchSize) {
                            saveAndFlush(batchPaid, migrPremiumsPaidRepository);
                            batchPaid.clear();
                        }

                        if (batchDue.size() >= batchSize) {
                            saveAndFlush(batchDue, migrPremiumsDueRepository);
                            batchDue.clear();
                        }
                    }
                }

            }

            // 🔹 Flush remaining
            if (!batchPaid.isEmpty()) saveAndFlush(batchPaid, migrPremiumsPaidRepository);
            if (!batchDue.isEmpty()) saveAndFlush(batchDue, migrPremiumsDueRepository);

            log.info("MIGRATION COMPLETED");

            return ResponseEntity.ok(CommonResponseDTO.builder()
                    .message("Premiums paid & due processed successfully")
                    .status(HttpStatus.OK.toString())
                    .build());

        } catch (Exception e) {
            log.error("ERROR DURING MIGRATION", e);
            return ResponseEntity.internalServerError().body(CommonResponseDTO.builder()
                    .message(e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                    .build());
        }
    }

    @Override
    public ResponseEntity<CommonResponseDTO> mapPremiumsExtra() {

        log.info("Reading policy number list");
        // 🔹 Read policies
        List<String> policyList = mainExcelReader.readPolicyNumbers();

        log.info("Extracting product codes & number list");
        // 🔹 Extract product codes & numbers
        List<PolicyNoDto> extractedPolicies = policyList.stream()
                .map(this::getProductCodeAndPolicyNo)
                .toList();

        Set<String> productCodes = extractedPolicies.stream()
                .map(PolicyNoDto::productCode)
                .collect(Collectors.toSet());

        Set<Integer> policyNos = extractedPolicies.stream()
                .map(PolicyNoDto::policyNo)
                .collect(Collectors.toSet());

        log.info("Loading premiums map");

        premiumMap =
                premiumDetailsRepository.findFiltered(productCodes, policyNos)
                        .stream()
                        .collect(Collectors.groupingBy(e -> e.getProductCode() + "/" + e.getPolicyNo()));

        Map<String, PolicyListEntity> policyMap =
                policyListRepository.findFiltered(policyList).stream()
                        .collect(Collectors.toMap(
                                PolicyListEntity::getContract,
                                Function.identity(),
                                (a,b) -> a
                        ));

        Map<String, ContactDetailEntity> contactMap =
                contactDetailRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProduct() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        log.info("Processing premium list");
        List<PremiumExtraFields> extraFieldsList = new ArrayList<>();
        try{
            for (PolicyNoDto policyNoDto : extractedPolicies) {
                String key = policyNoDto.productCode() + "/" + policyNoDto.policyNo();

                List<PremiumDetailsEntity> premiums =
                        premiumMap.getOrDefault(key, Collections.emptyList());

                if (premiums.isEmpty()) {
                    continue;
                }

                PremiumDetailsEntity last = premiums.stream()
                        .filter(p -> p.getPremiumDueDate() != null)
                        .max(Comparator.comparing(PremiumDetailsEntity::getPremiumDueDate))
                        .orElse(null);

                int paidCount = (int) premiums.stream()
                        .filter(p -> p.getPaymentDate() != null)
                        .count();

                ContactDetailEntity contact = contactMap.get(key);
                PolicyListEntity polEntity = policyMap.get(key);
                LocalDate paidUpTo = polEntity == null ? null : polEntity.getPaidUpTo();

                PremiumExtraFields extra = PremiumExtraFields.builder()
                        .policyNo(key)
                        .inceptionDate(last.getInceptionDate())
                        .premiumDueDate(contact.getNextPremiumDueDate() == null ? last.getInceptionDate().plusYears(1) : paidUpTo)
                        .paidCount(paidCount)
                        .period(getPeriod(last.getFrequency()))
                        .outstandingTotal(contact.getOutstanding())
                        .premiumsPaidTotal(contact.getTotalPremiumsPaid())
                        .modalPremium(contact.getModalPremiumWithoutHandlingFee())
                        .frequency(contact.getFrequency())
                        .term(contact.getTerm())
                        .policyStatus(contact.getPolicyStatus())
                        .lapsedDate(contact.getLapsedDate())
                        .build();

                extraFieldsList.add(extra);
            }
            saveInBatches(extraFieldsList, premiumsExtraRepository);

            return ResponseEntity.ok(CommonResponseDTO.builder()
                    .message("Premiums extra fields processed successfully")
                    .status(HttpStatus.OK.toString())
                    .build());
        } catch (Exception e) {
            log.error("ERROR DURING MIGRATION", e);
            return ResponseEntity.internalServerError().body(CommonResponseDTO.builder()
                    .message(e.getMessage())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                    .build());
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

    private <T> void saveAndFlush(List<T> list, JpaRepository<T, ?> repo) {
        log.info("Saving premiums of the batch with size: {}", list.size());
        repo.saveAll(list);
        repo.flush();
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
