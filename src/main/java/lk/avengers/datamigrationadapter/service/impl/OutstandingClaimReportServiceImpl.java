package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.OutstandingClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.OutstandingClaimRepository;
import lk.avengers.datamigrationadapter.service.OutstandingClaimReportService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutstandingClaimReportServiceImpl implements OutstandingClaimReportService {

    @Value("${oustandingClaimReport.file}")
    private String outstandingClaimReportFilePath;

    private final OutstandingClaimRepository outstandingClaimRepository;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int BATCH_SIZE = 2000;

    private static final int HEADER_ROW_INDEX = 3;   // Excel row 4
    private static final int DATA_ROW_INDEX = 4;     // Excel row 5
    private static final int START_COL = 1;          // Column B (A is empty)

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> uploadOutstandingClaimsReport() {

        log.info("UPLOAD_EXCEL(FILE_PATH) METHOD ACCESSED.");

        File file = new File(outstandingClaimReportFilePath);
        if (!file.exists() || !file.isFile()) {
            return error("File not found at the given path.");
        }

        String filename = file.getName().toLowerCase(Locale.ROOT);
        boolean isXls = filename.endsWith(".xls");
        boolean isXlsxOrXlsm = filename.endsWith(".xlsx") || filename.endsWith(".xlsm");

        if (!isXls && !isXlsxOrXlsm) {
            return error("Unsupported file type. Only .xls, .xlsx, .xlsm are supported.");
        }

        // clear previous (your original truncates all)
        outstandingClaimRepository.truncate();
        log.info("Previous outstanding claim records deleted.");

        int totalSaved = 0;
        LocalDateTime now = LocalDateTime.now();

        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             Workbook workbook = isXls
                     ? WorkbookFactory.create(is)
                     : StreamingReader.builder()
                     .rowCacheSize(200)
                     .bufferSize(8192)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> it = sheet.rowIterator();

            List<String> expectedHeaders = Arrays.asList(
                    "Claim Off. No",
                    "Product",
                    "Policy No",
                    "Agent #",
                    "Agent Name",
                    "Agent Type",
                    "Company Branch",
                    "Sales Branch",
                    "Occurred On",
                    "Declared On",
                    "Occu-red",
                    "Decla-red",
                    "Last Trn",
                    "Original Claim Amt",
                    "Total Claim Amt",
                    "Total Fees Amt",
                    "Paid Amount",
                    "Paid Fees Amount",
                    "O/S Claim +Fees",
                    "Recovery Amt",
                    "O/S Recovery",
                    "Policyholder / Insured",
                    "Claimed Life Assured",
                    "Profession / Activity",
                    "District",
                    "Place of Claim",
                    "Cause of Loss",
                    "Cause of Claim",
                    "Status Notes",
                    "Claim Description",
                    "Claim Type",
                    "Underwriting Year"
            );

            Integer currentYear = LocalDateTime.now().getYear();
            Integer currentMonth = LocalDateTime.now().getMonthValue();

            Row headerRow = null;
            List<OutstandingClaimEntity> batch = new ArrayList<>(BATCH_SIZE);

            while (it.hasNext()) {
                Row row = it.next();
                int r = row.getRowNum();

                // Header row
                if (r == HEADER_ROW_INDEX) {
                    headerRow = row;
                    ResponseEntity<CommonResponseDTO> headerValidation =
                            validateHeaders(expectedHeaders, headerRow);
                    if (headerValidation != null) return headerValidation;
                    continue;
                }

                // Data rows
                if (r < DATA_ROW_INDEX) continue;
                if (isRowBlank(row)) continue;

                OutstandingClaimEntity claim = new OutstandingClaimEntity();

                // Column mapping starts from B -> START_COL + offset
                claim.setClaimOfficeNumber(getCellValue(row.getCell(START_COL, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setProduct(getCellValue(row.getCell(START_COL + 1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setPolicyNumber(getCellValue(row.getCell(START_COL + 2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setAgentNumber(getCellValue(row.getCell(START_COL + 3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setAgentName(getCellValue(row.getCell(START_COL + 4, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setAgentType(getCellValue(row.getCell(START_COL + 5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setCompanyBranch(getCellValue(row.getCell(START_COL + 6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setSalesBranch(getCellValue(row.getCell(START_COL + 7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));

                claim.setOccurredOn(parseDate(getCellValue(row.getCell(START_COL + 8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setDeclaredOn(parseDate(getCellValue(row.getCell(START_COL + 9, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));

                claim.setOccurred(parseDouble(getCellValue(row.getCell(START_COL + 10, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setDeclared(parseDouble(getCellValue(row.getCell(START_COL + 11, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setLastTrn(parseDouble(getCellValue(row.getCell(START_COL + 12, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));

                claim.setOriginalClaimAmount(parseDouble(getCellValue(row.getCell(START_COL + 13, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setTotalClaimAmount(parseDouble(getCellValue(row.getCell(START_COL + 14, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setTotalFeesAmount(parseDouble(getCellValue(row.getCell(START_COL + 15, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setPaidAmount(parseDouble(getCellValue(row.getCell(START_COL + 16, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setPaidFeesAmount(parseDouble(getCellValue(row.getCell(START_COL + 17, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setOsClaimAndFees(parseDouble(getCellValue(row.getCell(START_COL + 18, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setRecoveryAmount(parseDouble(getCellValue(row.getCell(START_COL + 19, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));
                claim.setOsRecovery(parseDouble(getCellValue(row.getCell(START_COL + 20, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));

                claim.setPolicyholderOrInsured(getCellValue(row.getCell(START_COL + 21, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setClaimedLifeAssured(getCellValue(row.getCell(START_COL + 22, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setProfessionOrActivity(getCellValue(row.getCell(START_COL + 23, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setDistrict(getCellValue(row.getCell(START_COL + 24, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setPlaceOfClaim(getCellValue(row.getCell(START_COL + 25, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setCauseOfLoss(getCellValue(row.getCell(START_COL + 26, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setCauseOfClaim(getCellValue(row.getCell(START_COL + 27, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setStatusNotes(getCellValue(row.getCell(START_COL + 28, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setClaimDescription(getCellValue(row.getCell(START_COL + 29, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));
                claim.setClaimType(getCellValue(row.getCell(START_COL + 30, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));

                claim.setUnderwritingYear(parseInt(getCellValue(row.getCell(START_COL + 31, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL))));

                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                batch.add(claim);

                if (batch.size() >= BATCH_SIZE) {
                    outstandingClaimRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Inserted {} outstanding claim records so far (last row={})", totalSaved, r);
                }
            }

            if (headerRow == null) {
                return error("Header row (Row 3) missing.");
            }

            if (!batch.isEmpty()) {
                outstandingClaimRepository.saveAll(batch);
                totalSaved += batch.size();
            }

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message(totalSaved + " records uploaded successfully.")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            log.error("Error processing Excel file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonResponseDTO.builder()
                            .message("Error processing Excel file: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build()
            );
        }
    }

    /* ================= Helpers ================= */

    private ResponseEntity<CommonResponseDTO> error(String message) {
        return ResponseEntity.badRequest().body(
                CommonResponseDTO.builder()
                        .message(message)
                        .status(HttpStatus.BAD_REQUEST.toString())
                        .build()
        );
    }

    // Header validation starts from column B (START_COL)
    private ResponseEntity<CommonResponseDTO> validateHeaders(List<String> expectedHeaders, Row headerRow) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);

            Cell cell = headerRow.getCell(START_COL + i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String actual = getCellValue(cell);
            String actualSafe = actual == null ? "" : actual.trim();

            if (!expected.equalsIgnoreCase(actualSafe)) {
                String msg = String.format(
                        "Header mismatch at column %d: expected '%s' but found '%s'",
                        (START_COL + i) + 1, // 1-based column number
                        expected,
                        actualSafe
                );
                log.error(msg);
                return ResponseEntity.badRequest().body(
                        CommonResponseDTO.builder()
                                .message(msg)
                                .status(HttpStatus.BAD_REQUEST.toString())
                                .build()
                );
            }
        }
        return null;
    }

    // IMPORTANT: no formula evaluation (uses cached values)
    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        return switch (type) {
            case STRING -> {
                String v = cell.getStringCellValue();
                yield (v == null || v.isBlank()) ? null : v.trim();
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue()
                            .toLocalDate()
                            .format(DMY);
                }
                double n = cell.getNumericCellValue();
                yield (n == (long) n) ? String.valueOf((long) n) : String.valueOf(n);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        return LocalDate.parse(val, DMY);
    }

    private Double parseDouble(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return (int) Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isRowBlank(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}