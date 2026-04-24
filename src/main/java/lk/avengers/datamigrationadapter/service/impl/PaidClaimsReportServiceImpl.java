package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PaidClaimEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PaidClaimReportRepository;
import lk.avengers.datamigrationadapter.service.PaidClaimsReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import com.github.pjfanning.xlsx.StreamingReader;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaidClaimsReportServiceImpl implements PaidClaimsReportService {

    @Value("${paidClaimReport.file}")
    private String paidClaimReportFilePath;

    private final PaidClaimReportRepository paidClaimReportRepository;

    private static final int BATCH_SIZE = 2000;
    private static final DateTimeFormatter DMY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ResponseEntity<CommonResponseDTO> uploadPaidClaimsReport() {

        File file = new File(paidClaimReportFilePath);

        if (!file.exists()) {
            return error("File not found: " + paidClaimReportFilePath);
        }

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDateTime now = LocalDateTime.now();

        int totalSaved = 0;

        paidClaimReportRepository.truncate();
        log.info("Old paid claim records deleted.");

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

            List<PaidClaimEntity> batch = new ArrayList<>(BATCH_SIZE);

            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();

                // Skip rows before row index 4 (data starts from 5th row)
                if (row.getRowNum() < 4) continue;

                if (isRowBlank(row)) continue;

                PaidClaimEntity claim = mapRow(row);

                claim.setCurrentYear(currentYear);
                claim.setCurrentMonth(currentMonth);
                claim.setCreatedAt(now);

                batch.add(claim);

                if (batch.size() >= BATCH_SIZE) {
                    paidClaimReportRepository.saveAll(batch);
                    totalSaved += batch.size();
                    batch.clear();
                    log.info("Inserted {} records so far...", totalSaved);
                }
            }

            if (!batch.isEmpty()) {
                paidClaimReportRepository.saveAll(batch);
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

    /* ================== ROW MAPPING ================== */

    private PaidClaimEntity mapRow(Row row) {

        PaidClaimEntity claim = new PaidClaimEntity();

        claim.setClaimOfficeNumber(getString(row, 1));
        claim.setProduct(getString(row, 2));
        claim.setPolicyNo(getString(row, 3));
        claim.setAgentNo(getString(row, 4));
        claim.setAgentName(getString(row, 5));
        claim.setCompanyBranch(getString(row, 6));
        claim.setSalesBranch(getString(row, 7));

        claim.setBranchCode(getInt(row, 8));
        claim.setSumAssuredOfTheBenefit(getDouble(row, 9));
        claim.setMcfp(getDouble(row, 10));
        claim.setMedicalOrNonMedical("yes".equalsIgnoreCase(getString(row, 11)));
        claim.setUwDecision(getDouble(row, 12));

        claim.setOccurredOn(getDate(row, 13));
        claim.setDeclaredOn(getDate(row, 14));

        claim.setOccurred(getDouble(row, 15));
        claim.setDeclared(getDouble(row, 16));
        claim.setOriginalClaimAmt(getDouble(row, 17));
        claim.setTotalClaimAmt(getDouble(row, 18));
        claim.setTotalFeesAmt(getDouble(row, 19));

        claim.setTrnDate(getDate(row, 20));
        claim.setTrnStatus(getString(row, 21));
        claim.setTrnAmountCy(getDouble(row, 22));
        claim.setTrnAmountLc(getDouble(row, 23));

        claim.setPayeePin(getString(row, 24));
        claim.setPolicyHolder(getString(row, 25));
        claim.setClaimedLifeAssured(getString(row, 26));

        claim.setChildIdentification(getString(row, 27));
        claim.setClaimantName(getString(row, 28));

        claim.setProfessionCode(getLong(row, 29));
        claim.setProfessionOrActivity(getString(row, 30));
        claim.setNoOfPreviousClaims(getInt(row, 31));
        claim.setPreviousClaimsTotalSettlement(getDouble(row, 32));
        claim.setPreviousClaimNumbers(getString(row, 33));
        claim.setLifeAssuredCurrentAge(getInt(row, 34));
        claim.setLongRiskNo(getInt(row, 35));
        claim.setGender(getString(row, 36));
        claim.setPolicyAgeAtClaimDate(getInt(row, 37));
        claim.setNoOfDaysHospitalizedStandard(getInt(row, 38));
        claim.setNoOfDaysHospitalizedIcu(getInt(row, 39));

        claim.setPolicyHolderAddress(getString(row, 40));
        claim.setPhoneNumber(getString(row, 41));
        claim.setDistrict(getString(row, 42));
        claim.setPlaceOfClaim(getString(row, 43));
        claim.setCauseOfLoss(getString(row, 44));
        claim.setCauseOfClaim(getString(row, 45));
        claim.setStatusNotes(getString(row, 46));
        claim.setClaimDescription(getString(row, 47));

        claim.setUnderwritingYear(getInt(row, 48));
        claim.setExpert(getInt(row, 49));

        claim.setPayTo(getString(row, 50));
        claim.setXGracia(getString(row, 51));
        claim.setHcpName(getString(row, 52));
        claim.setClaimType(getString(row, 53));
        claim.setPolicyStatus(getString(row, 54));

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

        if (val == null || val.trim().isEmpty()) {
            return null;
        }

        return Double.parseDouble(val.trim());
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

        if (val == null) {
            return null;
        }

        val = val.trim();

        // empty → ignore
        if (val.isEmpty()) {
            return null;
        }

        // ignore numeric values like "0.5", "123", "0,5"
        if (val.matches("^[0-9]+([.,][0-9]+)?$")) {
            return null;
        }

        try {
            return LocalDate.parse(val, DMY);
        } catch (Exception e) {
            return null; // or log if needed
        }
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