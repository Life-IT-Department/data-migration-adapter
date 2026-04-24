package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.RejectedClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.RejectedClaimRepository;
import lk.avengers.datamigrationadapter.service.RejectedClaimReportService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RejectedClaimReportServiceImpl implements RejectedClaimReportService {

    @Value("${rejectedClaimReport.file}")
    private String rejectedClaimReportFilePath;

    private final RejectedClaimRepository rejectedClaimRepository;

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter[] INPUT_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
    };
    private static final int BATCH_SIZE = 2000;

    @SneakyThrows
    @Override
    public ResponseEntity<CommonResponseDTO> uploadRejectedClaimReport() {

        log.info("UPLOAD_EXCEL(FILE_PATH) METHOD ACCESSED.");

        File file = new File(rejectedClaimReportFilePath);
        if (!file.exists() || !file.isFile()) {
            return error("File not found at the given path.");
        }

        Integer currentYear = LocalDateTime.now().getYear();
        Integer currentMonth = LocalDateTime.now().getMonthValue();
        LocalDateTime now = LocalDateTime.now();

        rejectedClaimRepository.truncate();
        log.info("Previous rejected claim records truncated");

        int totalSaved = 0;

        String name = file.getName().toLowerCase(Locale.ROOT);
        boolean isXls = name.endsWith(".xls");
        boolean isXlsxOrXlsm = name.endsWith(".xlsx") || name.endsWith(".xlsm");

        if (!isXls && !isXlsxOrXlsm) {
            return error("Unsupported file type. Only .xls, .xlsx, .xlsm are supported.");
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             Workbook workbook = isXls
                     ? WorkbookFactory.create(is)
                     : StreamingReader.builder()
                     .rowCacheSize(200)
                     .bufferSize(8192)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<String> expectedHeaders = Arrays.asList(
                    "Policy",
                    "Claim No",
                    "Sales Agency",
                    "Company Agency",
                    "Agent",
                    "Claim Type",
                    "Occurred Date",
                    "Declared Date",
                    "PolicyHolder Name",
                    "Claimed Life Assured",
                    "Child Identification",
                    "Claimant Name",
                    "Rider",
                    "Rejected Date",
                    "Reason",
                    "Claimed Amount",
                    "Expert Fee",
                    "Paid Fee",
                    "Curr."
            );

            Row headerRow = null;
            List<RejectedClaimEntity> batch = new ArrayList<>(BATCH_SIZE);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum();

                // Header row index = 1
                if (rowNum == 1) {
                    headerRow = row;

                    ResponseEntity<CommonResponseDTO> headerValidation =
                            getStringResponseEntity(expectedHeaders, headerRow, evaluator);
                    if (headerValidation != null) return headerValidation;

                    continue;
                }

                // Data rows start at index 3
                if (rowNum < 3) continue;
                if (isRowBlank(row)) continue;

                RejectedClaimEntity claim = new RejectedClaimEntity();

                claim.setPolicy(getCellValue(row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setClaimNo(getCellValue(row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setSalesAgency(getCellValue(row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setCompanyAgency(getCellValue(row.getCell(4, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setAgent(getCellValue(row.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setClaimType(getCellValue(row.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setOccurredDate(parseDate(getCellValue(row.getCell(7, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));
                claim.setDeclaredDate(parseDate(getCellValue(row.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));

                claim.setPolicyholderName(getCellValue(row.getCell(9, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setClaimedLifeAssured(getCellValue(row.getCell(10, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setChildIdentification(getCellValue(row.getCell(11, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setClaimantName(getCellValue(row.getCell(12, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                claim.setRider(getCellValue(row.getCell(13, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setRejectedDate(parseDate(getCellValue(row.getCell(14, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));
                claim.setReason(getCellValue(row.getCell(15, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setClaimedAmount(parseDouble(getCellValue(row.getCell(16, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));
                claim.setExpertFee(parseDouble(getCellValue(row.getCell(17, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));
                claim.setPaidFee(parseDouble(getCellValue(row.getCell(18, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator)));

                claim.setCurr(getCellValue(row.getCell(19, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));

                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                batch.add(claim);

                if (batch.size() >= BATCH_SIZE) {
                    rejectedClaimRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Inserted {} rejected claim records so far (last row={})", totalSaved, rowNum);
                }
            }

            if (headerRow == null) {
                return error("Header row (B2) missing.");
            }

            if (!batch.isEmpty()) {
                rejectedClaimRepository.saveAll(batch);
                totalSaved += batch.size();
            }

            log.info("Rejected claim upload completed. Total saved={}", totalSaved);

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

    /* ===================== ADDED METHODS ===================== */

    private ResponseEntity<CommonResponseDTO> error(String message) {
        return ResponseEntity.badRequest().body(
                CommonResponseDTO.builder()
                        .message(message)
                        .status(HttpStatus.BAD_REQUEST.toString())
                        .build()
        );
    }

    /**
     * Validates that the header row matches expected headers.
     * Returns a ResponseEntity with error if mismatch; otherwise null.
     */
    private ResponseEntity<CommonResponseDTO> getStringResponseEntity(
            List<String> expectedHeaders,
            Row headerRow,
            FormulaEvaluator evaluator
    ) {
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);

            Cell cell = headerRow.getCell(i+1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String actual = getCellValue(cell, evaluator);
            String actualSafe = actual == null ? "" : actual.trim();

            if (!expected.equalsIgnoreCase(actualSafe)) {
                String msg = String.format(
                        "Header mismatch at column %d: expected '%s' but found '%s'",
                        i + 2, expected, actualSafe
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

    /**
     * Safely reads an Excel cell as String.
     * Handles STRING, NUMERIC (date/number), BOOLEAN, FORMULA.
     */
    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;

        CellType type = cell.getCellType();

        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        return switch (type) {
            case STRING -> {
                String v = cell.getStringCellValue();
                if (v == null || v.isBlank()) yield null;

                v = v.trim();

                // Try parsing as date
                LocalDate parsedDate = parseDate(v);
                if (parsedDate != null) {
                    yield parsedDate.format(OUTPUT_FORMAT);
                }

                yield v;
            }

            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue()
                            .toLocalDate()
                            .format(OUTPUT_FORMAT);
                }
                double n = cell.getNumericCellValue();
                yield (n == (long) n) ? String.valueOf((long) n) : String.valueOf(n);
            }

            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());

            default -> null; // BLANK, ERROR, _NONE
        };
    }

    private LocalDate parseDate(String val) {
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                return LocalDate.parse(val, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Double parseDouble(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Double.parseDouble(val);
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
