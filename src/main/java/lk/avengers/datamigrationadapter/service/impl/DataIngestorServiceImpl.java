package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;
import lk.avengers.datamigrationadapter.dto.request.ACPPolicyRequestDTO;
import lk.avengers.datamigrationadapter.dto.request.PolicyListRequestDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ACPPolicyEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PolicyListEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ACPPolicyRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PolicyListRepository;
import lk.avengers.datamigrationadapter.service.DataIngestorService;
import lk.avengers.datamigrationadapter.service.ExcelDataExtractorEnhancedService;
import lk.avengers.datamigrationadapter.service.ExcelDataExtractorService;
import lk.avengers.datamigrationadapter.util.MemoryMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestorServiceImpl implements DataIngestorService {

    @Value("${acp.data.file}")
    private String acpDataFilePath;

    @Value("${policy.list.file}")
    private String policyListFilePath;

    @Value("${contact.detail.file}")
    private String contactDetailFilePath;

    private final ExcelDataExtractorService excelDataExtractorService;
    private final ACPPolicyRepository acpPolicyRepository;
    private final PolicyListRepository policyListRepository;
    private final ExcelDataExtractorEnhancedService excelDataExtractorEnhancedService;

    // Constants
    private static final int BATCH_SIZE = 1000;
    private static final String YYYYMMDD_PATTERN = "\\d{8}";
    private static final Set<String> INVALID_DATE_VALUES = Set.of(
            "?", "-", "N/A", "NA", "NULL", "NONE", "#N/A", ""
    );
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    // ==================== Main Processing Methods ====================

    @Override
    @Transactional("reportPlatformTransactionManager")
    public void ProcessPolicyListData(String uuid) {
        log.info("UUID: {} - Starting Policy List data processing", uuid);

        try {
            // Extract data from Excel
            ExcelDataResponseDTO extractedExcelFile = extractExcelData(uuid, policyListFilePath, 0, 1, 3);
            if (extractedExcelFile == null) return;

            // Process and clean data
            List<Map<String, Object>> cleanedData = cleanAndValidateData(uuid, extractedExcelFile);
            log.info("UUID: {} - Cleaned {} records ready for processing", uuid, cleanedData.size());

            // Delete existing data
            log.info("UUID: {} - Deleting existing Policy List data", uuid);
            policyListRepository.truncateTable();
            log.info("UUID: {} - Existing Policy List data deleted successfully", uuid);

            // Map and save in batches
            List<PolicyListEntity> entities = cleanedData.stream()
                    .map(rowData -> mapToPolicyListRequestDTO(rowData, uuid))
                    .map(dto -> dto.mapData(PolicyListEntity.class))
                    .toList();

            processBatch(uuid, entities, "Policy List records", policyListRepository::saveAll, BATCH_SIZE);

            log.info("UUID: {} - Policy List data processing completed successfully", uuid);

        } catch (Exception e) {
            log.error("UUID: {} - Failed to process Policy List data: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to process Policy List data", e);
        }
    }

    @Override
    @Transactional("reportPlatformTransactionManager")
    public void ProcessACPData(String uuid) {
        log.info("UUID: {} - Starting ACP data processing", uuid);

        try {
            // Extract data from Excel
            ExcelDataResponseDTO extractedExcelFile = extractExcelData(uuid, acpDataFilePath, 0, 0, 1);
            if (extractedExcelFile == null) return;

            // Process and clean data
            List<Map<String, Object>> cleanedData = cleanAndValidateData(uuid, extractedExcelFile);
            log.info("UUID: {} - Cleaned {} records ready for processing", uuid, cleanedData.size());

            // Delete existing data
            log.info("UUID: {} - Deleting existing ACP policy data", uuid);
            acpPolicyRepository.truncateTable();
            log.info("UUID: {} - Existing ACP policy data deleted successfully", uuid);

            // Map and save in batches
            List<ACPPolicyEntity> entities = cleanedData.stream()
                    .map(rowData -> mapToACPPolicyRequestDTO(rowData, uuid))
                    .map(dto -> dto.mapData(ACPPolicyEntity.class))
                    .toList();

            processBatch(uuid, entities, "ACP policies", acpPolicyRepository::saveAll, BATCH_SIZE);

            log.info("UUID: {} - ACP data processing completed successfully", uuid);

        } catch (Exception e) {
            log.error("UUID: {} - Failed to process ACP data: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to process ACP data", e);
        }
    }

    @Override
    public void ProcessContactDetailData(String uuid) {
        log.info("UUID: {} - Starting Contact Detail data processing", uuid);

        try{
            ExcelDataResponseDTO extractedExcelFile = extractExcelData(uuid, contactDetailFilePath, 0, 1, 3);
            if (extractedExcelFile == null) return;
            log.info("UUID: {} - File extraction successful", uuid);

            // Process and clean data
            List<Map<String, Object>> cleanedData = cleanAndValidateData(uuid, extractedExcelFile);
            log.info("UUID: {} - Cleaned {} records ready for processing", uuid, cleanedData.size());

            // Force GC if memory usage is high (> 75%)
            MemoryMonitor.forceGcIfNeeded(75.0);

        } catch (Exception e) {
            log.error("UUID: {} - Failed to process Policy List data: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to process Policy List data", e);
        } finally {
            // Log final memory state
            MemoryMonitor.logMemoryUsage("After Processing Complete");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Extract Excel data with specified parameters
     */
    private ExcelDataResponseDTO extractExcelData(String uuid, String filePath, int sheetIndex,
                                                  int headerRow, int dataRow) {
        ExcelExtractorRequestDTO request = ExcelExtractorRequestDTO.builder()
                .filePath(filePath)
                .sheetIndex(sheetIndex)
                .headerRow(headerRow)
                .dataRow(dataRow)
                .build();

        ExcelDataResponseDTO result = excelDataExtractorEnhancedService.extractExcelFileFromPath(request);

        if (result == null || result.getExtractedData().isEmpty()) {
            log.warn("UUID: {} - No data extracted from file: {}", uuid, filePath);
            return null;
        }

        log.info("UUID: {} - Successfully extracted {} rows from Excel", uuid, result.getExtractedData().size());
        return result;
    }

    /**
     * Clean and validate extracted data
     */
    private List<Map<String, Object>> cleanAndValidateData(String uuid, ExcelDataResponseDTO extractedData) {
        // Filter valid headers
        List<String> validHeaders = extractedData.getHeaders().stream()
                .filter(header -> header != null && !header.trim().isEmpty())
                .collect(Collectors.toList());

        log.info("UUID: {} - Found {} valid headers out of {} total columns",
                uuid, validHeaders.size(), extractedData.getHeaders().size());

        // Clean data
        return extractedData.getExtractedData().stream()
                .map(rowData -> filterValidColumns(rowData, validHeaders))
                .toList();
    }

    /**
     * Filter row data to only include valid column headers
     */
    private Map<String, Object> filterValidColumns(Map<String, Object> rowData, List<String> validHeaders) {
        Map<String, Object> cleanedRow = new LinkedHashMap<>();
        for (String header : validHeaders) {
            cleanedRow.put(header, rowData.getOrDefault(header, null));
        }
        return cleanedRow;
    }

    /**
     * Generic batch processor for saving entities
     */
    private <T> void processBatch(String uuid, List<T> entities, String entityName,
                                  Consumer<List<T>> batchSaver, int batchSize) {
        int totalRecords = entities.size();
        int processedRecords = 0;

        log.info("UUID: {} - Starting batch save for {} {}", uuid, totalRecords, entityName);

        for (int i = 0; i < totalRecords; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalRecords);
            List<T> batch = entities.subList(i, endIndex);

            batchSaver.accept(batch);

            processedRecords += batch.size();

            if (processedRecords % (batchSize * 5) == 0 || processedRecords == totalRecords) {
                log.info("UUID: {} - Progress: {}/{} {} saved ({} %)",
                        uuid, processedRecords, totalRecords, entityName,
                        String.format("%.1f", (processedRecords * 100.0 / totalRecords)));
            }
        }

        log.info("UUID: {} - Successfully saved all {} {}", uuid, processedRecords, entityName);
    }

    // ==================== DTO Mapping Methods ====================

    private PolicyListRequestDTO mapToPolicyListRequestDTO(Map<String, Object> data, String uuid) {
        try {
            PolicyListRequestDTO dto = new PolicyListRequestDTO();
            dto.setAgent(getString(data, "Agent"));
            dto.setUnitHead(getString(data, "Unit Head"));
            dto.setSalesBranch(getString(data, "Sales Branch"));
            dto.setCompanyBranch(getString(data, "Company Branch"));
            dto.setPolicyBranch(getString(data, "Policy Branch"));
            dto.setIntroducer(getString(data, "Introducer"));
            dto.setSupervisor(getString(data, "Supervisor"));
            dto.setContract(getString(data, "Contract"));
            dto.setCurrency(getString(data, "C/Y"));
            dto.setCustomerName(getString(data, "Customer Name"));
            dto.setInception(getDateFromInteger(data, "Inception"));
            dto.setFrequencyMode(getString(data, "Frequency Mode"));
            dto.setStatus(getString(data, "Status"));
            dto.setAnnualPremium(getBigDecimal(data, "Annual Premium"));
            dto.setPaidUpTo(getDateFromInteger(data, "Paid Up To"));
            dto.setUnappropriateBalance(getBigDecimal(data, "Unappopriate Balance"));
            dto.setRequestUuid(uuid);
            return dto;
        } catch (Exception e) {
            log.error("UUID: {} - Error mapping Policy List row data: {}", uuid, e.getMessage());
            throw new RuntimeException("Failed to map Excel data to Policy List DTO", e);
        }
    }

    private ACPPolicyRequestDTO mapToACPPolicyRequestDTO(Map<String, Object> data, String uuid) {
        try {
            ACPPolicyRequestDTO dto = new ACPPolicyRequestDTO();

            // Identification & Policy Metadata
            dto.setMasterPolicyNo(getString(data, "MASTER POLICY NO"));
            dto.setPolicyNo(getString(data, "POLICY NO"));
            dto.setCertificateNo(getString(data, "CERTIFICATE NO"));
            dto.setProposalNo(getString(data, "PROPOSAL NO"));
            dto.setProductCode(getString(data, "Product Code"));
            dto.setPlanNo(getString(data, "PLAN NO"));
            dto.setInception(getDateFromInteger(data, "INCEPTION"));
            dto.setExpiry(getDateFromInteger(data, "EXPIRY"));
            dto.setIssueDate(getDateFromInteger(data, "Issue Date"));
            dto.setTerm(getInteger(data, "TERM"));
            dto.setCy(getString(data, "C/Y"));
            dto.setPremiumPaymentTerm(getString(data, "Premium Payment Term"));
            dto.setStatus(getString(data, "STATUS"));
            dto.setDate(getDateFromInteger(data, "DATE"));
            dto.setOperationDate(getDateFromInteger(data, "Operation Date"));
            dto.setReason(getString(data, "Reason"));

            // Sales & Branch Info
            dto.setSalesBranchCode(getString(data, "Sales Branch Code"));
            dto.setSalesBranchName(getString(data, "Sales Branch Name"));
            dto.setCompanyBranchCode(getString(data, "Company Branch Code"));
            dto.setCompanyBranchName(getString(data, "Company Branch Name"));
            dto.setPolicyBranchCode(getString(data, "Policy Branch Code"));
            dto.setPolicyBranchName(getString(data, "Policy Branch Name"));
            dto.setAgentCode(getString(data, "AGENT CODE"));
            dto.setIntroducer(getString(data, "Introducer"));
            dto.setSupervisor(getString(data, "Supervisor"));
            dto.setRiPercentage(getBigDecimal(data, "RI %"));

            // Contribution & Premium Details
            dto.setInsuredContributionType(getString(data, "INSURED CONTRIBUTION TYPE"));
            dto.setInsuredModalPremium(getBigDecimal(data, "INSURED MODAL PREMIUM"));
            dto.setCompanyModalPremium(getBigDecimal(data, "COMPANY MODAL PREMIUM"));
            dto.setFrequency(getString(data, "FREQUENCY"));
            dto.setNextPremium(getDateFromInteger(data, "NEXT PREMIUM"));
            dto.setEmployerName(getString(data, "EMPLOYER NAME"));
            dto.setInsuranceCategory(getString(data, "INSURANCE CATEGORY"));
            dto.setMinContributionPerc(getBigDecimal(data, "MIN CONTRIBUTION PERC"));
            dto.setMaxContributionPerc(getBigDecimal(data, "MAX CONTRIBUTION PERC"));
            dto.setInsuredPremiumInflation(getBigDecimal(data, "INSURED PREMIUM INFLATION"));

            // Personal Details
            dto.setPin(getString(data, "PIN"));
            dto.setMasterPin(getString(data, "MASTER PIN"));
            dto.setTitle(getString(data, "TITLE"));
            dto.setFullName(getString(data, "FULL NAME"));
            dto.setGender(getString(data, "GENDER"));
            dto.setDob(getDateFromInteger(data, "DOB"));
            dto.setAae(getInteger(data, "AAE"));
            dto.setSarChoice(getString(data, "SAR CHOICE"));
            dto.setNumberOfRidersTaken(getInteger(data, "Number of Riders Taken"));

            // Rider Details
            mapRiderDetails(dto, data);

            // Basic Sums & Values
            dto.setBasicSumInsuredFormula(getString(data, "Basic Sum Insured Formula"));
            dto.setBasicSumAssured(getBigDecimal(data, "Basic Sum Assured"));
            dto.setBasicSumAssuredInflation(getBigDecimal(data, "Basic Sum Assured Inflation"));

            // Value Groups
            mapValueToday(dto, data);
            mapTransactionAmounts(dto, data);
            mapInterestCredited(dto, data);
            mapSurrenderValues(dto, data);

            // Final Policy Attributes
            dto.setInsuranceCoveragePeriod(getString(data, "Insurance Coverage Period"));
            dto.setPacInsuredShare(getBigDecimal(data, "PAC INSURED SHARE"));
            dto.setLastPaymentDate(getDateFromInteger(data, "Last Payment Date"));
            dto.setLastPremiumDueDate(getDateFromInteger(data, "Last Premium Due Date"));

            return dto;
        } catch (Exception e) {
            log.error("UUID: {} - Error mapping ACP Policy row data: {}", uuid, e.getMessage());
            throw new RuntimeException("Failed to map Excel data to ACP Policy DTO", e);
        }
    }

    // ==================== Rider Mapping Helper Methods ====================

    private void mapRiderDetails(ACPPolicyRequestDTO dto, Map<String, Object> data) {
        // DTH Rider
        dto.setDthSar(getBigDecimal(data, "DTH SAR"));
        dto.setSubDth(getString(data, "SUB-DTH"));
        dto.setSubRateMilDth(getBigDecimal(data, "SUB Rate/Mil-DTH"));
        dto.setDthOccupationalLoadingPerc(getBigDecimal(data, "DTH Occupational Loading %"));
        dto.setDthOccupationClass(getString(data, "DTH Occupation Class"));
        dto.setDthInsuredCoiShare(getBigDecimal(data, "DTH INSURED COI SHARE"));

        // ACCD Rider
        dto.setAccdSa(getBigDecimal(data, "ACCD SA"));
        dto.setSubAccd(getString(data, "SUB-ACCD"));
        dto.setSubRateMilAccd(getBigDecimal(data, "SUB Rate/Mil-ACCD"));
        dto.setAccdOccupationalLoadingPerc(getBigDecimal(data, "ACCD Occupational Loading %"));
        dto.setAccdOccupationClass(getString(data, "ACCD Occupation Class"));
        dto.setAccdInsuredCoiShare(getBigDecimal(data, "ACCD INSURED COI SHARE"));

        // ACCP Rider
        dto.setAccpSa(getBigDecimal(data, "ACCP SA"));
        dto.setSubAccp(getString(data, "SUB-ACCP"));
        dto.setSubRateMilAccp(getBigDecimal(data, "SUB Rate/Mil-ACCP"));
        dto.setAccpOccupationalLoadingPerc(getBigDecimal(data, "ACCP Occupational Loading %"));
        dto.setAccpOccupationClass(getString(data, "ACCP Occupation Class"));
        dto.setAccpInsuredCoiShare(getBigDecimal(data, "ACCP INSURED COI SHARE"));

        // ACCT Rider
        dto.setAcctSa(getBigDecimal(data, "ACCT SA"));
        dto.setSubAcct(getString(data, "SUB-ACCT"));
        dto.setSubRateMilAcct(getBigDecimal(data, "SUB Rate/Mil-ACCT"));
        dto.setAcctOccupationalLoadingPerc(getBigDecimal(data, "ACCT Occupational Loading %"));
        dto.setAcctOccupationClass(getString(data, "ACCT Occupation Class"));
        dto.setAcctInsuredCoiShare(getBigDecimal(data, "ACCT INSURED COI SHARE"));

        // CILX Rider
        dto.setCilxSa(getBigDecimal(data, "CILX SA"));
        dto.setSubCilx(getString(data, "SUB-CILX"));
        dto.setSubRateMilCilx(getBigDecimal(data, "SUB Rate/Mil-CILX"));
        dto.setCilxOccupationalLoadingPerc(getBigDecimal(data, "CILX Occupational Loading %"));
        dto.setCilxOccupationClass(getString(data, "CILX Occupation Class"));
        dto.setCilxInsuredCoiShare(getBigDecimal(data, "CILX INSURED COI SHARE"));

        // PTD Rider
        dto.setPtdSa(getBigDecimal(data, "PTD SA"));
        dto.setSubPtd(getString(data, "SUB-PTD"));
        dto.setSubRateMilPtd(getBigDecimal(data, "SUB Rate/Mil-PTD"));
        dto.setPtdOccupationalLoadingPerc(getBigDecimal(data, "PTD Occupational Loading %"));
        dto.setPtdOccupationClass(getString(data, "PTD Occupation Class"));
        dto.setPtdInsuredCoiShare(getBigDecimal(data, "PTD INSURED COI SHARE"));
    }

    private void mapValueToday(ACPPolicyRequestDTO dto, Map<String, Object> data) {
        dto.setInsuredValueToday(getBigDecimal(data, "INSURED VALUE TODAY"));
        dto.setUnvestedPremiumValueToday(getBigDecimal(data, "UNVESTED PREMIUM VALUE TODAY"));
        dto.setVestedPremiumValueToday(getBigDecimal(data, "VESTED PREMIUM VALUE TODAY"));
        dto.setInsuredTopupValueToday(getBigDecimal(data, "INSURED TOPUP VALUE TODAY"));
        dto.setUnvestedTopupValueToday(getBigDecimal(data, "UNVESTED TOPUP VALUE TODAY"));
        dto.setVestedTopupValueToday(getBigDecimal(data, "VESTED TOPUP VALUE TODAY"));
    }

    private void mapTransactionAmounts(ACPPolicyRequestDTO dto, Map<String, Object> data) {
        dto.setInsuredTransactionAmount(getBigDecimal(data, "INSURED TRANSACTION AMOUNT"));
        dto.setUnvestedPremiumTransactionAmount(getBigDecimal(data, "UNVESTED PREMIUM TRANSACTION AMOUNT"));
        dto.setVestedPremiumTransactionAmount(getBigDecimal(data, "VESTED PREMIUM TRANSACTION AMOUNT"));
        dto.setInsuredTopupTransactionAmount(getBigDecimal(data, "INSURED TOPUP TRANSACTION AMOUNT"));
        dto.setUnvestedTopupTransactionAmount(getBigDecimal(data, "UNVESTED TOPUP TRANSACTION AMOUNT"));
        dto.setVestedTopupTransactionAmount(getBigDecimal(data, "VESTED TOPUP TRANSACTION AMOUNT"));
    }

    private void mapInterestCredited(ACPPolicyRequestDTO dto, Map<String, Object> data) {
        dto.setInsuredInterestCredited(getBigDecimal(data, "INSURED INTEREST CREDITED"));
        dto.setUnvestedPremiumInterestCredited(getBigDecimal(data, "UNVESTED PREMIUM INTEREST CREDITED"));
        dto.setVestedPremiumInterestCredited(getBigDecimal(data, "VESTED PREMIUM INTEREST CREDITED"));
        dto.setInsuredTopupInterestCredited(getBigDecimal(data, "INSURED TOPUP INTEREST CREDITED"));
        dto.setUnvestedTopupInterestCredited(getBigDecimal(data, "UNVESTED TOPUP INTEREST CREDITED"));
        dto.setVestedTopupInterestCredited(getBigDecimal(data, "VESTED TOPUP INTEREST CREDITED"));
    }

    private void mapSurrenderValues(ACPPolicyRequestDTO dto, Map<String, Object> data) {
        dto.setInsuredSurrenderValue(getBigDecimal(data, "INSURED SURRENDER VALUE"));
        dto.setUnvestedPremiumSurrenderValue(getBigDecimal(data, "UNVESTED PREMIUM SURRENDER VALUE"));
        dto.setVestedPremiumSurrenderValue(getBigDecimal(data, "VESTED PREMIUM SURRENDER VALUE"));
        dto.setInsuredTopupSurrenderValue(getBigDecimal(data, "INSURED TOPUP SURRENDER VALUE"));
        dto.setUnvestedTopupSurrenderValue(getBigDecimal(data, "UNVESTED TOPUP SURRENDER VALUE"));
        dto.setVestedTopupSurrenderValue(getBigDecimal(data, "VESTED TOPUP SURRENDER VALUE"));
    }

    // ==================== Value Extraction Methods ====================

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String stringValue = value.toString().trim();
            return stringValue.isEmpty() ? null : Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
            log.debug("Could not parse integer for key '{}': {}", key, value);
            return null;
        }
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            String stringValue = value.toString().trim();
            stringValue = stringValue.replace(",", "");
            return stringValue.isEmpty() ? null : new BigDecimal(stringValue);
        } catch (NumberFormatException e) {
            log.debug("Could not parse BigDecimal for key '{}': {}", key, value);
            return null;
        }
    }

    private LocalDate getDateFromInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) return null;

            // Early exit for known non-date values
            if (INVALID_DATE_VALUES.contains(stringValue.toUpperCase())) {
                return null;
            }

            // Quick validation: check if string contains at least one digit
            if (!stringValue.matches(".*\\d.*")) {
                return null;
            }

            // Handle YYYYMMDD format
            if (stringValue.matches(YYYYMMDD_PATTERN)) {
                return parseYYYYMMDD(stringValue);
            }

            // Try standard date formats
            return parseDate(stringValue);

        } catch (Exception e) {
            log.debug("Could not parse date for key '{}': {}", key, value);
            return null;
        }
    }

    // ==================== Date Parsing Utilities ====================

    private LocalDate parseYYYYMMDD(String value) {
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(4, 6));
        int day = Integer.parseInt(value.substring(6, 8));
        return LocalDate.of(year, month, day);
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        return null;
    }
}