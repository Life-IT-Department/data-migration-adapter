package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ClosedClaimReportEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ClosedClaimsReportRepository;
import lk.avengers.datamigrationadapter.service.ClosedClaimReportService;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClosedClaimReportServiceImpl implements ClosedClaimReportService {

    @Value("${closedClaimReport.file}")
    private String closedClaimReportFilePath;

    private final ClosedClaimsReportRepository closedClaimsReportRepository;

    private static final int BATCH_SIZE = 1000;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ResponseEntity<CommonResponseDTO> uploadClosedClaimsReport() {
        File file = new File(closedClaimReportFilePath);

        if (!file.exists()) {
            return error("File not found: " + closedClaimReportFilePath);
        }

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDateTime now = LocalDateTime.now();

        int totalSaved = 0;

        closedClaimsReportRepository.truncate();
        log.info("Old closed claim records deleted.");

        try (InputStream fis = new FileInputStream(file);
             InputStream is = new BufferedInputStream(fis);
             Workbook workbook = openWorkbook(is, file.getName())) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            if (!rowIterator.hasNext()) {
                return error("Excel sheet is empty.");
            }

            List<ClosedClaimReportEntity> batch = new ArrayList<>(BATCH_SIZE);

            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();

                // Skip rows before header row (header is row index 1)
                if (row.getRowNum() < 1) continue;

                // Validate header row
                if (row.getRowNum() == 1) {
                    ResponseEntity<CommonResponseDTO> headerErr = validateHeaders(row);
                    if (headerErr != null) return headerErr;
                    continue;
                }

                if (isRowBlank(row)) continue;

                ClosedClaimReportEntity claim = mapRow(row);
                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                batch.add(claim);

                if (batch.size() >= BATCH_SIZE) {
                    closedClaimsReportRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Inserted {} records so far...", totalSaved);
                }
            }

            if (!batch.isEmpty()) {
                closedClaimsReportRepository.saveAll(batch);
                totalSaved += batch.size();
            }

            log.info("Completed upload. Total records: {}", totalSaved);

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message(totalSaved + " records uploaded successfully.")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            log.error("Excel processing error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponseDTO.builder()
                            .message("Error processing file: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build());
        }
    }

    private Workbook openWorkbook(InputStream bufferedIs, String fileName) throws Exception {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".xlsx")) {
            return StreamingReader.builder()
                    .rowCacheSize(200)
                    .bufferSize(8192)
                    .open(bufferedIs);
        }
        return WorkbookFactory.create(bufferedIs);
    }

    private ClosedClaimReportEntity mapRow(Row row) {

        ClosedClaimReportEntity claim = new ClosedClaimReportEntity();

        claim.setPolicyNo(getString(row, 1));
        claim.setClaimNo(getString(row, 2));
        claim.setSalesBranch(getString(row, 3));
        claim.setCompanyAgency(getString(row, 4));
        claim.setAgentCode(getString(row, 5));
        claim.setAgentName(getString(row, 6));

        claim.setPolicyHolder(getString(row, 7));
        claim.setAddress(getString(row, 8));
        String mobile = getString(row, 9);
        claim.setMobileNo(
                mobile == null ? null : mobile.replace("#", "").trim()
        );

        claim.setTypeOfClaim(getString(row, 10));

        claim.setOccurrenceDate(getDate(row, 11));
        claim.setDeclarationDate(getDate(row, 12));
        claim.setStatusDate(getDate(row, 13));

        claim.setClaimedLifeAssured(getString(row, 14));
        claim.setChildIdentification(getString(row, 15));

        claim.setClaimAmount(getDouble(row, 16));
        claim.setPreLastEvaluation(getDouble(row, 17));
        claim.setEvaluationAmount(getDouble(row, 18));

        claim.setRemarks(getString(row, 19));
        return claim;
    }

    /* ================== HEADER VALIDATION ================== */

    private ResponseEntity<CommonResponseDTO> validateHeaders(Row headerRow) {
        List<String> expectedHeaders = List.of(
                "Policy No.",
                "Claim No",
                "Sales Branch",
                "Company Agency",
                "Agent Code",
                "Agent Name",
                "Policy Holder",
                "Address",
                "Mobile No",
                "Type Of Claim",
                "Occurrence Date",
                "Declaration Date",
                "Status Date",
                "Claimed Life Assured",
                "Child Identification",
                "Claim Amount",
                "Pre-Last Evaluation",
                "Evaluation Amount",
                "Remarks"
        );

        // Your old getStringResponseEntity() returned ResponseEntity<ReportResponseDTO>.
        // Here we validate inline and return CommonResponseDTO errors.
        // NOTE: Your original mapping starts at column index 1 (not 0), so header check should also align.
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);
            String actual = getString(headerRow, i + 1);
            if (actual == null || !expected.equalsIgnoreCase(actual.trim())) {
                return error("Invalid header at column " + (i + 1) + ". Expected: '" + expected + "', Found: '" + actual + "'");
            }
        }

        return null;
    }

    /* ================== CELL HELPERS ================== */

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue()
                            .toLocalDate()
                            .format(DMY);
                }
                double d = cell.getNumericCellValue();
                return d == (long) d ? String.valueOf((long) d)
                        : String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Double getDouble(Row row, int col) {
        String val = getString(row, col);
        return val == null ? null : Double.parseDouble(val);
    }

    private Integer getInt(Row row, int col) {
        Double d = getDouble(row, col);
        return d == null ? null : d.intValue();
    }

    private Long getLong(Row row, int col) {
        Double d = getDouble(row, col);
        return d == null ? null : d.longValue();
    }

    private LocalDate getDate(Row row, int col) {
        String val = getString(row, col);
        return val == null ? null : LocalDate.parse(val, DMY);
    }

    private boolean isRowBlank(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private ResponseEntity<CommonResponseDTO> error(String message) {
        return ResponseEntity.badRequest().body(
                CommonResponseDTO.builder()
                        .message(message)
                        .status(HttpStatus.BAD_REQUEST.toString())
                        .build());
    }
}
