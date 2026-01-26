package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PaidClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PaidClaimReportRepository;
import lk.avengers.datamigrationadapter.service.PaidClaimsReportService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaidClaimsReportServiceImpl implements PaidClaimsReportService {

    @Value("${paidClaimReport.file}")
    String paidClaimReportFilePath;

    private final PaidClaimReportRepository paidClaimReportRepository;

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> uploadPaidClaimsReport(){
        log.info("UPLOAD_EXCEL(MULTIPART_FILE) METHOD ACCESSED.");

// Get file path from properties
        File file = new File(paidClaimReportFilePath);

// Validate file existence
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.badRequest().body(
                    CommonResponseDTO.builder()
                            .message("File not found at the given path: " + paidClaimReportFilePath)
                            .status(HttpStatus.BAD_REQUEST.toString())
                            .build()
            );
        }

// Date/time for current year, month, and created timestamp
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDateTime now = LocalDateTime.now();

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // HEADER ROW IS 4TH (INDEX 3)
            Row headerRow = sheet.getRow(3);
            if (headerRow == null) {
                return ResponseEntity.badRequest().body(
                        CommonResponseDTO.builder()
                                .message("Header row (A4) missing.")
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .build()
                );
            }

            // Validate headers
            List<String> expectedHeaders = Arrays.asList(
                    "Claim Off#", "Product", "Policy No", "Agent #", "Agent Name",
                    "Company Branch", "Sales Branch", "Branch Code", "Sum Assured of the benefit",
                    "MCFP", "Medical or non medical", "U/W Decision", "Occurred On",
                    "Declared On", "Occu-red", "Decla-red", "Original Claim Amt",
                    "Total Claim Amt", "Total Fees Amt", "Trn. Date", "Trn. Status",
                    "Trn. Amount / CY", "Trn. Amount /LC", "Payee Pin", "Policy Holder",
                    "Claimed Life Assured", "Claimant Name", "Profession Code",
                    "Profession / Activity", "No of previous claims",
                    "Previous claims total Settlement", "Previous Claim Numbers",
                    "Life assured Current Age", "Risk No", "Gender",
                    "Policy Age At Claim Date", "No of Days hospitalized in standard units",
                    "No of Days hospitalized in ICU units", "Policy Holder Address",
                    "Phone Number", "District", "Place of Claim", "Cause of Loss",
                    "Cause of Claim", "Status Notes", "Claim Description",
                    "U/Year", "Expert", "Pay To", "X/Gracia", "HCP Name",
                    "Claim Type", "Policy Status"
            );

            ResponseEntity<CommonResponseDTO> validationResponse = getStringResponseEntity(expectedHeaders, headerRow);
            if (validationResponse != null) return validationResponse;

            // Get last row with data
            int lastRowWithData = getLastRowWithData(sheet);

            List<PaidClaimEntity> claims = new ArrayList<>();

            // Read records starting from row 5 (index 4)
            for (int i = 4; i <= lastRowWithData; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                PaidClaimEntity claim = new PaidClaimEntity();

                // Helper lambda to safely parse numeric cells
                java.util.function.Function<Integer, Double> getDouble = col -> {
                    String val = getCellValue(row.getCell(col));
                    return (val == null || val.isEmpty()) ? null : Double.parseDouble(val);
                };

                // Helper lambda to safely parse integer cells
                java.util.function.Function<Integer, Integer> getInt = col -> {
                    String val = getCellValue(row.getCell(col));
                    return (val == null || val.isEmpty()) ? null : (int) Double.parseDouble(val);
                };

                // Helper lambda to parse LocalDate from string
                java.util.function.Function<Integer, LocalDate> getDate = col -> {
                    String val = getCellValue(row.getCell(col));
                    return (val == null || val.isEmpty()) ? null :
                            LocalDate.parse(val, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                };

                // Map fields
                claim.setClaimOfficeNumber(getCellValue(row.getCell(1)));
                claim.setProduct(getCellValue(row.getCell(2)));
                claim.setPolicyNo(getCellValue(row.getCell(3)));
                claim.setAgentNo(getCellValue(row.getCell(4)));
                claim.setAgentName(getCellValue(row.getCell(5)));
                claim.setCompanyBranch(getCellValue(row.getCell(6)));
                claim.setSalesBranch(getCellValue(row.getCell(7)));

                claim.setBranchCode(getInt.apply(8));
                claim.setSumAssuredOfTheBenefit(getDouble.apply(9));
                claim.setMcfp(getDouble.apply(10));
                claim.setMedicalOrNonMedical("yes".equalsIgnoreCase(getCellValue(row.getCell(11))));
                claim.setUwDecision(getDouble.apply(12));

                claim.setOccurredOn(getDate.apply(13));
                claim.setDeclaredOn(getDate.apply(14));

                claim.setOccurred(getDouble.apply(15));
                claim.setDeclared(getDouble.apply(16));
                claim.setOriginalClaimAmt(getDouble.apply(17));
                claim.setTotalClaimAmt(getDouble.apply(18));
                claim.setTotalFeesAmt(getDouble.apply(19));

                claim.setTrnDate(getDate.apply(20));
                claim.setTrnStatus(getCellValue(row.getCell(21)));
                claim.setTrnAmountCy(getDouble.apply(22));
                claim.setTrnAmountLc(getDouble.apply(23));

                claim.setPayeePin(getCellValue(row.getCell(24)));
                claim.setPolicyHolder(getCellValue(row.getCell(25)));
                claim.setClaimedLifeAssured(getCellValue(row.getCell(26)));
                claim.setClaimantName(getCellValue(row.getCell(27)));

                claim.setProfessionCode(row.getCell(28) == null || getCellValue(row.getCell(28)).isEmpty() ? null : (long) Double.parseDouble(getCellValue(row.getCell(28))));
                claim.setProfessionOrActivity(getCellValue(row.getCell(29)));
                claim.setNoOfPreviousClaims(getInt.apply(30));
                claim.setPreviousClaimsTotalSettlement(getDouble.apply(31));
                claim.setPreviousClaimNumbers(getCellValue(row.getCell(32)));
                claim.setLifeAssuredCurrentAge(getInt.apply(33));
                claim.setLongRiskNo(getInt.apply(34));
                claim.setGender(getCellValue(row.getCell(35)));
                claim.setPolicyAgeAtClaimDate(getInt.apply(36));
                claim.setNoOfDaysHospitalizedStandard(getInt.apply(37));
                claim.setNoOfDaysHospitalizedIcu(getInt.apply(38));

                claim.setPolicyHolderAddress(getCellValue(row.getCell(39)));
                claim.setPhoneNumber(getCellValue(row.getCell(40)));
                claim.setDistrict(getCellValue(row.getCell(41)));
                claim.setPlaceOfClaim(getCellValue(row.getCell(42)));
                claim.setCauseOfLoss(getCellValue(row.getCell(43)));
                claim.setCauseOfClaim(getCellValue(row.getCell(44)));
                claim.setStatusNotes(getCellValue(row.getCell(45)));
                claim.setClaimDescription(getCellValue(row.getCell(46)));

                claim.setUnderwritingYear(getInt.apply(47));
                claim.setExpert(getInt.apply(48));

                claim.setPayTo(getCellValue(row.getCell(49)));
                claim.setXGracia(getCellValue(row.getCell(50)));
                claim.setHcpName(getCellValue(row.getCell(51)));
                claim.setClaimType(getCellValue(row.getCell(52)));
                claim.setPolicyStatus(getCellValue(row.getCell(53)));

                // Current metadata
                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                claims.add(claim);
                log.debug("Processed row index: {}", i);
            }

            paidClaimReportRepository.truncate();
            log.info("Deleted previous paid claim records.");

            // Save new records
            paidClaimReportRepository.saveAll(claims);
            log.info("Saved {} paid claim records successfully.", claims.size());

            CommonResponseDTO response = CommonResponseDTO.builder()
                    .message(claims.size() + " records uploaded successfully.")
                    .status(HttpStatus.OK.toString())
                    .build();

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonResponseDTO.builder()
                            .message("Error processing Excel file: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build()
            );
        }
    }

    /**
     * Safely reads the value of an Excel cell as a String.
     * Handles STRING, NUMERIC, BOOLEAN, FORMULA types.
     * Returns null if the cell is null or blank.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue();
                return (val == null || val.isBlank()) ? null : val.trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Format date as dd/MM/yyyy
                    return cell.getLocalDateTimeCellValue().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Avoid scientific notation for integers
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                // Evaluate the formula
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                return getCellValue(evaluator.evaluateInCell(cell));

            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return null;
        }
    }

    /**
     * Validates that the header row matches the expected headers.
     * Returns ResponseEntity with error if headers mismatch; otherwise returns null.
     */
    private ResponseEntity<CommonResponseDTO> getStringResponseEntity(List<String> expectedHeaders, Row headerRow) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expectedHeader = expectedHeaders.get(i);
            Cell cell = headerRow.getCell(i);
            String actualHeader = getCellValue(cell);

            if (!expectedHeader.equalsIgnoreCase(actualHeader != null ? actualHeader : "")) {
                String message = String.format(
                        "Header mismatch at column %d: expected '%s' but found '%s'",
                        i + 1, expectedHeader, actualHeader
                );
                log.error(message);
                return ResponseEntity.badRequest().body(
                        CommonResponseDTO.builder()
                                .message(message)
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .build()
                );
            }
        }
        return null;
    }

    /**
     * Finds the last row in the sheet that contains any non-empty cell.
     * Returns the zero-based row index of the last row with data.
     */
    private int getLastRowWithData(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        for (int i = lastRow; i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (Cell cell : row) {
                    String cellValue = getCellValue(cell);
                    if (cellValue != null && !cellValue.isBlank()) {
                        return i; // Found last row with data
                    }
                }
            }
        }
        return 0; // Sheet is empty or all rows blank
    }


}
