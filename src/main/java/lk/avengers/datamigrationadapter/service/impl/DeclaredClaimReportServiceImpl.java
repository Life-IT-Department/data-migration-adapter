package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.DeclaredClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.DeclaredClaimReportRepository;
import lk.avengers.datamigrationadapter.service.DeclaredClaimReportService;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeclaredClaimReportServiceImpl implements DeclaredClaimReportService {

    @Value("${declaredClaimReport.file}")
    private String declaredClaimReportFilePath;

    private final DeclaredClaimReportRepository declaredClaimReportRepository;

    private static final int BATCH_SIZE = 2000;
    private static final DateTimeFormatter DMY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ResponseEntity<CommonResponseDTO> uploadDeclaredClaimsReport() {
        File file = new File(declaredClaimReportFilePath);

        if (!file.exists()) {
            return error("File not found: " + declaredClaimReportFilePath);
        }

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDateTime now = LocalDateTime.now();

        int totalSaved = 0;

        declaredClaimReportRepository.truncate();
        log.info("Old declared claim records deleted.");

        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(200)
                     .bufferSize(8192)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.rowIterator();

            if (!rowIterator.hasNext()) {
                return error("Excel sheet is empty.");
            }

            List<DeclaredClaimEntity> batch = new ArrayList<>(BATCH_SIZE);

            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();

                // Header is 3rd row (index 2) in your original code.
                // Data starts from row index 3 (4th row).
                if (row.getRowNum() < 3) continue;

                // Validate header once at row index 2
                if (row.getRowNum() == 2) {
                    ResponseEntity<CommonResponseDTO> headerErr = validateHeaders(row);
                    if (headerErr != null) return headerErr;
                    continue;
                }

                if (isRowBlank(row)) continue;

                DeclaredClaimEntity claim = mapRow(row);

                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                batch.add(claim);

                if (batch.size() >= BATCH_SIZE) {
                    declaredClaimReportRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Inserted {} records so far...", totalSaved);
                }
            }

            if (!batch.isEmpty()) {
                declaredClaimReportRepository.saveAll(batch);
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

    /* ================== HEADER VALIDATION ================== */

    private ResponseEntity<CommonResponseDTO> validateHeaders(Row headerRow) {
        List<String> expectedHeaders = Arrays.asList(
                "Claim Off. No",
                "Product",
                "Policy No",
                "Agent#",
                "Agent Name",
                "Company Branch",
                "Sales Branch",
                "Occurred",
                "Declared",
                "Occu-red",
                "Decla-red",
                "Last Trn",
                "Original Claim Amt",
                "Total Claim Amt",
                "Total Fees Amt",
                "Paid Amount",
                "Paid Fees Amount",
                "O/S Claim Amt",
                "Recovered Amt",
                "O/S Recovery",
                "Title",
                "Policyholder / Insured",
                "Title",
                "Claimed Life Assured",
                "Child Identification",
                "Claim Closed Date",
                "Remarks",
                "Profession / Activity",
                "District",
                "Place Of Claims",
                "Cause of Loss",
                "Cause Of Claims",
                "Status Notes",
                "Claim Description",
                "Claim Type",
                "Underwriting Year"
        );

        // Your old getStringResponseEntity() returned ResponseEntity<ReportResponseDTO>.
        // Here we validate inline and return CommonResponseDTO errors.
        // NOTE: Your original mapping starts at column index 1 (not 0), so header check should also align.
        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);
            String actual = getString(headerRow, i + 1); // +1 because you read data from col 1 onwards
            if (actual == null || !expected.equalsIgnoreCase(actual.trim())) {
                return error("Invalid header at column " + (i + 1) + ". Expected: '" + expected + "', Found: '" + actual + "'");
            }
        }

        return null;
    }

    /* ================== ROW MAPPING ================== */

    private DeclaredClaimEntity mapRow(Row row) {

        DeclaredClaimEntity claim = new DeclaredClaimEntity();

        claim.setClaimOfficeNumber(getString(row, 1));
        claim.setProduct(getString(row, 2));
        claim.setPolicyNo(getString(row, 3));
        claim.setAgentNumber(getString(row, 4));
        claim.setAgentName(getString(row, 5));

        // Fix your duplicate assignment bug:
        // you had setCompanyBranch twice (cell 5 then cell 6). This keeps only col 6 for company branch.
        claim.setCompanyBranch(getString(row, 6));
        claim.setSalesBranch(getString(row, 7));

        // Dates (your previous code parsed dd/MM/yyyy)
        claim.setOccurredDate(getDate(row, 8));
        claim.setDeclaredDate(getDate(row, 9));

        // Numeric fields
        claim.setOccurred(getDouble(row, 10));
        claim.setDeclared(getDouble(row, 11));
        claim.setLastTrn(getDouble(row, 12));
        claim.setOriginalClaimAmt(getDouble(row, 13));
        claim.setTotalClaimAmt(getDouble(row, 14));
        claim.setTotalFeesAmt(getDouble(row, 15));
        claim.setPaidAmount(getDouble(row, 16));
        claim.setPaidFeesAmount(getDouble(row, 17));
        claim.setOsClaimAmt(getDouble(row, 18));
        claim.setRecoveredAmt(getDouble(row, 19));
        claim.setOsRecovery(getDouble(row, 20));

        claim.setPolicyHolderTitle(getString(row, 21));
        claim.setPolicyholderOrInsured(getString(row, 22));
        claim.setClaimedTitle(getString(row, 23));
        claim.setClaimedLifeAssured(getString(row, 24));

        claim.setChildIdentification(getString(row, 25));
        claim.setClaimClosedDate(getDate(row, 26));

        claim.setRemarks(getString(row, 27));
        claim.setProfessionOrActivity(getString(row, 28));
        claim.setDistrict(getString(row, 29));
        claim.setPlaceOfClaims(getString(row, 30));
        claim.setCauseOfLoss(getString(row, 31));
        claim.setCauseOfClaims(getString(row, 32));
        claim.setStatusNotes(getString(row, 33));
        claim.setClaimDescription(getString(row, 34));
        claim.setClaimType(getString(row, 35));

        claim.setUnderwritingYear(getInt(row, 36));

        return claim;
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
