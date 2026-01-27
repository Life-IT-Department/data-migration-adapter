package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;
import lk.avengers.datamigrationadapter.dto.request.ACPPolicyRequestDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ACPPolicyEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ACPPolicyRepository;
import lk.avengers.datamigrationadapter.service.DataIngestorService;
import lk.avengers.datamigrationadapter.service.ExcelDataExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestorServiceImpl implements DataIngestorService {

    private final ExcelDataExtractorService excelDataExtractorService;
    private final ACPPolicyRepository acpPolicyRepository;

    // Constants for date patterns
    private static final String YYYYMMDD_PATTERN = "\\d{8}";
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };
    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    };

    @Override
    public void ProcessACPData(String uuid, MultipartFile excelFile) {
        log.info("UUID: {} - Starting ACP data processing", uuid);

        try {
            ExcelExtractorRequestDTO excelData = ExcelExtractorRequestDTO.builder()
                    .file(excelFile)
                    .sheetIndex(0)
                    .headerRow(0)
                    .dataRow(1)
                    .build();

            ExcelDataResponseDTO extractedExcelFile = excelDataExtractorService.extractExcelFile(excelData);

            if (extractedExcelFile == null || extractedExcelFile.getExtractedData().isEmpty()) {
                log.warn("UUID: {} - No data extracted from Excel file", uuid);
                return;
            }

            // Filter out empty headers to avoid processing empty columns
            List<String> validHeaders = extractedExcelFile.getHeaders().stream()
                    .filter(header -> header != null && !header.trim().isEmpty())
                    .collect(Collectors.toList());

            log.info("UUID: {} - Found {} valid headers out of {} total columns",
                    uuid, validHeaders.size(), extractedExcelFile.getHeaders().size());

            // Clean the extracted data by removing empty header columns
            List<Map<String, Object>> cleanedData = extractedExcelFile.getExtractedData().stream()
                    .map(rowData -> filterValidColumns(rowData, validHeaders))
                    .toList();

            // Map to DTOs
            List<ACPPolicyRequestDTO> acpPolicyRequestDTOS = cleanedData.stream()
                    .map(rowData -> mapToACPPolicyRequestDTO(rowData, uuid))
                    .toList();

            // Map DTO's to Entities
            List<ACPPolicyEntity> acpPolicyEntities = acpPolicyRequestDTOS.stream()
                    .map(dto -> dto.mapData(ACPPolicyEntity.class))
                    .toList();

            acpPolicyRepository.saveAll(acpPolicyEntities);
            log.info("UUID: {} - Successfully saved {} ACP policies", uuid, acpPolicyEntities.size());

        } catch (Exception e) {
            log.error("UUID: {} - Error processing ACP data: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to process ACP data", e);
        }
    }

    /**
     * Filter row data to only include valid (non-empty) column headers
     */
    private Map<String, Object> filterValidColumns(Map<String, Object> rowData, List<String> validHeaders) {
        Map<String, Object> cleanedRow = new LinkedHashMap<>();
        for (String header : validHeaders) {
            if (rowData.containsKey(header)) {
                cleanedRow.put(header, rowData.get(header));
            }
        }
        return cleanedRow;
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

            // Rider Details - Using helper method to reduce repetition
            mapRiderDetails(dto, data);

            // Basic Sums & Values
            dto.setBasicSumInsuredFormula(getString(data, "Basic Sum Insured Formula"));
            dto.setBasicSumAssured(getBigDecimal(data, "Basic Sum Assured"));
            dto.setBasicSumAssuredInflation(getBigDecimal(data, "Basic Sum Assured Inflation"));

            // Value Groups - Using helper methods
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
            log.error("UUID: {} - Error mapping row data: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to map Excel data to ACP Policy DTO", e);
        }
    }

    // Helper method to map all rider details
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
            log.warn("Could not parse integer for key '{}': {}", key, value);
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
            return stringValue.isEmpty() ? null : new BigDecimal(stringValue);
        } catch (NumberFormatException e) {
            log.warn("Could not parse BigDecimal for key '{}': {}", key, value);
            return null;
        }
    }

    private LocalDate getDateFromInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) return null;

            // Handle YYYYMMDD format (e.g., 20190301)
            if (stringValue.matches(YYYYMMDD_PATTERN)) {
                return parseYYYYMMDD(stringValue);
            }

            // Try standard date formats
            return parseDate(stringValue, DATE_FORMATTERS);

        } catch (Exception e) {
            log.warn("Could not parse date for key '{}': {}", key, value);
            return null;
        }
    }

    private LocalDateTime getDateTimeFromInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) return null;

            // IMPORTANT: Check for YYYYMMDD format first (same as getDateFromInteger)
            if (stringValue.matches(YYYYMMDD_PATTERN)) {
                LocalDate date = parseYYYYMMDD(stringValue);
                return date != null ? date.atStartOfDay() : null;
            }

            // Try datetime formats (with time component)
            LocalDateTime dateTime = parseDateTime(stringValue, DATETIME_FORMATTERS);
            if (dateTime != null) return dateTime;

            // Fallback to date parsing with standard formatters
            LocalDate date = parseDate(stringValue, DATE_FORMATTERS);
            return date != null ? date.atStartOfDay() : null;

        } catch (Exception e) {
            log.warn("Could not parse datetime for key '{}': {}", key, value);
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

    private LocalDate parseDate(String value, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // The current formatter doesn't match this date format, try the next one
                log.debug("Failed to parse date '{}' with formatter {}: {}", value, formatter, e.getMessage());
            }
        }
        // None of the formatters worked
        log.warn("Unable to parse date value '{}' with any of the configured formatters", value);
        return null;
    }

    private LocalDateTime parseDateTime(String value, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // The current formatter doesn't match this datetime format, try the next one
                log.debug("Failed to parse datetime '{}' with formatter {}: {}", value, formatter, e.getMessage());
            }
        }
        // None of the formatters worked
        log.warn("Unable to parse datetime value '{}' with any of the configured formatters", value);
        return null;
    }
}