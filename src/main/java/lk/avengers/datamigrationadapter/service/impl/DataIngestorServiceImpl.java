package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;
import lk.avengers.datamigrationadapter.dto.request.ACPPolicyRequestDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestorServiceImpl implements DataIngestorService {

    private final ExcelDataExtractorService excelDataExtractorService;

    @Override
    public void ProcessACPData(String uuid, MultipartFile excelFile){
        log.info("UUID: {} PROCESS_ACP_DATA (STRING, MULTIPART_FILE) METHOD ACCESSED.", uuid);

        ExcelExtractorRequestDTO excelData = ExcelExtractorRequestDTO.builder()
                .file(excelFile).sheetIndex(0)
                .headerRow(0).dataRow(1).build();

        ExcelDataResponseDTO extractedExcelFile = excelDataExtractorService.extractExcelFile(excelData);
        List<String> nonEmptyHeaders = extractedExcelFile.getHeaders().stream()
                .filter(header -> header != null && !header.trim().isEmpty())
                .toList();
        System.out.print(nonEmptyHeaders);

        List<ACPPolicyRequestDTO> acpPolicyRequestDTOS= new ArrayList<>();

        // Process each row of data
        List<Map<String, Object>> extractedData = extractedExcelFile.getExtractedData();

        for (Map<String, Object> rowData : extractedData) {
            ACPPolicyRequestDTO acpPolicyDTO = mapToACPPolicyRequestDTO(rowData, nonEmptyHeaders);
            acpPolicyRequestDTOS.add(acpPolicyDTO);
            // Process the mapped DTO (save to a database, validate, etc.)
            log.info("Mapped ACP Policy DTO: {}", acpPolicyDTO);
        }
        System.out.println("sds");

    }

    private ACPPolicyRequestDTO mapToACPPolicyRequestDTO(Map<String, Object> extractedData, List<String> nonEmptyHeaders) {
        ACPPolicyRequestDTO dto = new ACPPolicyRequestDTO();

        try {
            // --- Identification & Policy Metadata ---
            dto.setMasterPolicyNo(getStringValue(extractedData, "Master Policy No"));
            dto.setPolicyNo(getStringValue(extractedData, "Policy No"));
            dto.setCertificateNo(getStringValue(extractedData, "Certificate No"));
            dto.setProposalNo(getStringValue(extractedData, "Proposal No"));
            dto.setProductCode(getStringValue(extractedData, "Product Code"));
            dto.setPlanNo(getStringValue(extractedData, "Plan No"));
            dto.setInception(getLocalDateValue(extractedData, "Inception"));
            dto.setExpiry(getLocalDateValue(extractedData, "Expiry"));
            dto.setIssueDate(getLocalDateValue(extractedData, "Issue Date"));
            dto.setTerm(getIntegerValue(extractedData, "Term"));
            dto.setCy(getStringValue(extractedData, "C/Y"));
            dto.setPremiumPaymentTerm(getStringValue(extractedData, "Premium Payment Term"));
            dto.setStatus(getStringValue(extractedData, "Status"));
            dto.setDate(getLocalDateValue(extractedData, "Date"));
            dto.setOperationDate(getLocalDateTimeValue(extractedData, "Operation Date"));
            dto.setReason(getStringValue(extractedData, "Reason"));

            // --- Sales & Branch Info ---
            dto.setSalesBranchCode(getStringValue(extractedData, "Sales Branch Code"));
            dto.setSalesBranchName(getStringValue(extractedData, "Sales Branch Name"));
            dto.setCompanyBranchCode(getStringValue(extractedData, "Company Branch Code"));
            dto.setCompanyBranchName(getStringValue(extractedData, "Company Branch Name"));
            dto.setPolicyBranchCode(getStringValue(extractedData, "Policy Branch Code"));
            dto.setPolicyBranchName(getStringValue(extractedData, "Policy Branch Name"));
            dto.setAgentCode(getStringValue(extractedData, "Agent Code"));
            dto.setIntroducer(getStringValue(extractedData, "Introducer"));
            dto.setSupervisor(getStringValue(extractedData, "Supervisor"));
            dto.setRiPercentage(getBigDecimalValue(extractedData, "RI %"));

            // --- Contribution & Premium Details ---
            dto.setInsuredContributionType(getStringValue(extractedData, "Insured Contribution Type"));
            dto.setInsuredModalPremium(getBigDecimalValue(extractedData, "Insured Modal Premium"));
            dto.setCompanyModalPremium(getBigDecimalValue(extractedData, "Company Modal Premium"));
            dto.setFrequency(getStringValue(extractedData, "Frequency"));
            dto.setNextPremium(getBigDecimalValue(extractedData, "Next Premium"));
            dto.setEmployerName(getStringValue(extractedData, "Employer Name"));
            dto.setInsuranceCategory(getStringValue(extractedData, "Insurance Category"));
            dto.setMinContributionPerc(getBigDecimalValue(extractedData, "Min Contribution %"));
            dto.setMaxContributionPerc(getBigDecimalValue(extractedData, "Max Contribution %"));
            dto.setInsuredPremiumInflation(getBigDecimalValue(extractedData, "Insured Premium Inflation"));

            // --- Personal Details ---
            dto.setPin(getStringValue(extractedData, "PIN"));
            dto.setMasterPin(getStringValue(extractedData, "Master PIN"));
            dto.setTitle(getStringValue(extractedData, "Title"));
            dto.setFullName(getStringValue(extractedData, "Full Name"));
            dto.setGender(getStringValue(extractedData, "Gender"));
            dto.setDob(getLocalDateValue(extractedData, "DOB"));
            dto.setAae(getIntegerValue(extractedData, "AAE"));
            dto.setSarChoice(getStringValue(extractedData, "SAR Choice"));
            dto.setNumberOfRidersTaken(getIntegerValue(extractedData, "Number of Riders Taken"));

            // --- Death (DTH) Rider Details ---
            dto.setDthSar(getBigDecimalValue(extractedData, "DTH SAR"));
            dto.setSubDth(getStringValue(extractedData, "Sub DTH"));
            dto.setSubRateMilDth(getBigDecimalValue(extractedData, "Sub Rate Mil DTH"));
            dto.setDthOccupationalLoadingPerc(getBigDecimalValue(extractedData, "DTH Occupational Loading %"));
            dto.setDthOccupationClass(getStringValue(extractedData, "DTH Occupation Class"));
            dto.setDthInsuredCoiShare(getBigDecimalValue(extractedData, "DTH Insured COI Share"));

            // --- Accidental Death (ACCD) Rider Details ---
            dto.setAccdSa(getBigDecimalValue(extractedData, "ACCD SA"));
            dto.setSubAccd(getStringValue(extractedData, "Sub ACCD"));
            dto.setSubRateMilAccd(getBigDecimalValue(extractedData, "Sub Rate Mil ACCD"));
            dto.setAccdOccupationalLoadingPerc(getBigDecimalValue(extractedData, "ACCD Occupational Loading %"));
            dto.setAccdOccupationClass(getStringValue(extractedData, "ACCD Occupation Class"));
            dto.setAccdInsuredCoiShare(getBigDecimalValue(extractedData, "ACCD Insured COI Share"));

            // --- Accidental Permanent (ACCP) Rider Details ---
            dto.setAccpSa(getBigDecimalValue(extractedData, "ACCP SA"));
            dto.setSubAccp(getStringValue(extractedData, "Sub ACCP"));
            dto.setSubRateMilAccp(getBigDecimalValue(extractedData, "Sub Rate Mil ACCP"));
            dto.setAccpOccupationalLoadingPerc(getBigDecimalValue(extractedData, "ACCP Occupational Loading %"));
            dto.setAccpOccupationClass(getStringValue(extractedData, "ACCP Occupation Class"));
            dto.setAccpInsuredCoiShare(getBigDecimalValue(extractedData, "ACCP Insured COI Share"));

            // --- Accidental Total (ACCT) Rider Details ---
            dto.setAcctSa(getBigDecimalValue(extractedData, "ACCT SA"));
            dto.setSubAcct(getStringValue(extractedData, "Sub ACCT"));
            dto.setSubRateMilAcct(getBigDecimalValue(extractedData, "Sub Rate Mil ACCT"));
            dto.setAcctOccupationalLoadingPerc(getBigDecimalValue(extractedData, "ACCT Occupational Loading %"));
            dto.setAcctOccupationClass(getStringValue(extractedData, "ACCT Occupation Class"));
            dto.setAcctInsuredCoiShare(getBigDecimalValue(extractedData, "ACCT Insured COI Share"));

            // --- Critical Illness (CILX) Rider Details ---
            dto.setCilxSa(getBigDecimalValue(extractedData, "CILX SA"));
            dto.setSubCilx(getStringValue(extractedData, "Sub CILX"));
            dto.setSubRateMilCilx(getBigDecimalValue(extractedData, "Sub Rate Mil CILX"));
            dto.setCilxOccupationalLoadingPerc(getBigDecimalValue(extractedData, "CILX Occupational Loading %"));
            dto.setCilxOccupationClass(getStringValue(extractedData, "CILX Occupation Class"));
            dto.setCilxInsuredCoiShare(getBigDecimalValue(extractedData, "CILX Insured COI Share"));

            // --- Permanent Total Disability (PTD) Rider Details ---
            dto.setPtdSa(getBigDecimalValue(extractedData, "PTD SA"));
            dto.setSubPtd(getStringValue(extractedData, "Sub PTD"));
            dto.setSubRateMilPtd(getBigDecimalValue(extractedData, "Sub Rate Mil PTD"));
            dto.setPtdOccupationalLoadingPerc(getBigDecimalValue(extractedData, "PTD Occupational Loading %"));
            dto.setPtdOccupationClass(getStringValue(extractedData, "PTD Occupation Class"));
            dto.setPtdInsuredCoiShare(getBigDecimalValue(extractedData, "PTD Insured COI Share"));

            // --- Basic Sums & Values ---
            dto.setBasicSumInsuredFormula(getStringValue(extractedData, "Basic Sum Insured Formula"));
            dto.setBasicSumAssured(getBigDecimalValue(extractedData, "Basic Sum Assured"));
            dto.setBasicSumAssuredInflation(getBigDecimalValue(extractedData, "Basic Sum Assured Inflation"));
            dto.setInsuredValueToday(getBigDecimalValue(extractedData, "Insured Value Today"));
            dto.setUnvestedPremiumValueToday(getBigDecimalValue(extractedData, "Unvested Premium Value Today"));
            dto.setVestedPremiumValueToday(getBigDecimalValue(extractedData, "Vested Premium Value Today"));
            dto.setInsuredTopupValueToday(getBigDecimalValue(extractedData, "Insured Topup Value Today"));
            dto.setUnvestedTopupValueToday(getBigDecimalValue(extractedData, "Unvested Topup Value Today"));
            dto.setVestedTopupValueToday(getBigDecimalValue(extractedData, "Vested Topup Value Today"));

            // --- Transaction Amounts ---
            dto.setInsuredTransactionAmount(getBigDecimalValue(extractedData, "Insured Transaction Amount"));
            dto.setUnvestedPremiumTransactionAmount(getBigDecimalValue(extractedData, "Unvested Premium Transaction Amount"));
            dto.setVestedPremiumTransactionAmount(getBigDecimalValue(extractedData, "Vested Premium Transaction Amount"));
            dto.setInsuredTopupTransactionAmount(getBigDecimalValue(extractedData, "Insured Topup Transaction Amount"));
            dto.setUnvestedTopupTransactionAmount(getBigDecimalValue(extractedData, "Unvested Topup Transaction Amount"));
            dto.setVestedTopupTransactionAmount(getBigDecimalValue(extractedData, "Vested Topup Transaction Amount"));

            // --- Interest Credited ---
            dto.setInsuredInterestCredited(getBigDecimalValue(extractedData, "Insured Interest Credited"));
            dto.setUnvestedPremiumInterestCredited(getBigDecimalValue(extractedData, "Unvested Premium Interest Credited"));
            dto.setVestedPremiumInterestCredited(getBigDecimalValue(extractedData, "Vested Premium Interest Credited"));
            dto.setInsuredTopupInterestCredited(getBigDecimalValue(extractedData, "Insured Topup Interest Credited"));
            dto.setUnvestedTopupInterestCredited(getBigDecimalValue(extractedData, "Unvested Topup Interest Credited"));
            dto.setVestedTopupInterestCredited(getBigDecimalValue(extractedData, "Vested Topup Interest Credited"));

            // --- Surrender Values ---
            dto.setInsuredSurrenderValue(getBigDecimalValue(extractedData, "Insured Surrender Value"));
            dto.setUnvestedPremiumSurrenderValue(getBigDecimalValue(extractedData, "Unvested Premium Surrender Value"));
            dto.setVestedPremiumSurrenderValue(getBigDecimalValue(extractedData, "Vested Premium Surrender Value"));
            dto.setInsuredTopupSurrenderValue(getBigDecimalValue(extractedData, "Insured Topup Surrender Value"));
            dto.setUnvestedTopupSurrenderValue(getBigDecimalValue(extractedData, "Unvested Topup Surrender Value"));
            dto.setVestedTopupSurrenderValue(getBigDecimalValue(extractedData, "Vested Topup Surrender Value"));

            // --- Final Policy Attributes ---
            dto.setInsuranceCoveragePeriod(getStringValue(extractedData, "Insurance Coverage Period"));
            dto.setPacInsuredShare(getBigDecimalValue(extractedData, "PAC Insured Share"));
            dto.setLastPaymentDate(getLocalDateValue(extractedData, "Last Payment Date"));
            dto.setLastPremiumDueDate(getLocalDateValue(extractedData, "Last Premium Due Date"));

        } catch (Exception e) {
            log.error("Error mapping row data to ACPPolicyRequestDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map Excel data to ACP Policy DTO", e);
        }

        return dto;
    }

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String stringValue = value.toString().trim();
            return stringValue.isEmpty() ? null : Integer.valueOf(stringValue);
        } catch (NumberFormatException e) {
            log.warn("Could not parse integer value for key '{}': {}", key, value);
            return null;
        }
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
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
            log.warn("Could not parse BigDecimal value for key '{}': {}", key, value);
            return null;
        }
    }

    private LocalDate getLocalDateValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDate) {
                return (LocalDate) value;
            }
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) return null;

            // Try common date formats
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(stringValue, formatter);
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }

            log.warn("Could not parse date value for key '{}': {}", key, value);
            return null;
        } catch (Exception e) {
            log.warn("Error parsing date value for key '{}': {}", key, value);
            return null;
        }
    }

    private LocalDateTime getLocalDateTimeValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) return null;

            // Try common datetime formats
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(stringValue, formatter);
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }

            // If no datetime format works, try to parse as date and convert to datetime
            LocalDate date = getLocalDateValue(data, key);
            return date != null ? date.atStartOfDay() : null;

        } catch (Exception e) {
            log.warn("Error parsing datetime value for key '{}': {}", key, value);
            return null;
        }
    }


}
