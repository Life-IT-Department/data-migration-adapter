package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.UnitLinkReportEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.UnitLinkRepository;
import lk.avengers.datamigrationadapter.service.UnitLinkReportUploadService;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnitLinkReportUploadServiceImpl implements UnitLinkReportUploadService {

    @Value("${unitLink.file}")
    private String filePath;

    private final UnitLinkRepository unitLinkRepository;

    private static final int BATCH_SIZE = 2000;

    @Override
    public ResponseEntity<CommonResponseDTO> uploadUnitLinkReport() {
        File file = new File(filePath);

        if (!file.exists()) {
            return error("File not found: " + filePath);
        }

        int totalSaved = 0;

        unitLinkRepository.truncate();
        log.info("Old unit link records deleted.");

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

            List<UnitLinkReportEntity> batch = new ArrayList<>(BATCH_SIZE);

            boolean headerSkipped = false;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                if (!isRowBlank(row)) {

                    UnitLinkReportEntity entity = mapRow(row);
                    batch.add(entity);

                    if (batch.size() >= BATCH_SIZE) {
                        unitLinkRepository.saveAll(batch);
                        totalSaved += batch.size();
                        batch.clear();
                        log.info("Inserted {} records so far...", totalSaved);
                    }
                }
            }

            if (!batch.isEmpty()) {
                unitLinkRepository.saveAll(batch);
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

    private UnitLinkReportEntity mapRow(Row row) {

        UnitLinkReportEntity e = new UnitLinkReportEntity();

        e.setPolicyNumber(getString(row, 0));
        e.setPlanNumber(getString(row, 1));
        e.setCustomerName(getString(row, 2));

        // Allocations
        e.setBondFundAllocation(getDecimal(row, 3));
        e.setBalanceFundAllocation(getDecimal(row, 4));
        e.setGrowthFundAllocation(getDecimal(row, 5));
        e.setShariaFundAllocation(getDecimal(row, 6));

        // Units Regular
        e.setBondUnitsRegular(getDecimal(row, 7));
        e.setBalanceUnitsRegular(getDecimal(row, 8));
        e.setGrowthUnitsRegular(getDecimal(row, 9));
        e.setShariaUnitsRegular(getDecimal(row, 10));

        // Units Topup
        e.setBondUnitsTopup(getDecimal(row, 11));
        e.setBalanceUnitsTopup(getDecimal(row, 12));
        e.setGrowthUnitsTopup(getDecimal(row, 13));
        e.setShariaUnitsTopup(getDecimal(row, 14));

        // Unit Prices
        e.setBondUnitPrice(getDecimal(row, 15));
        e.setBalanceUnitPrice(getDecimal(row, 16));
        e.setGrowthUnitPrice(getDecimal(row, 17));
        e.setShariaUnitPrice(getDecimal(row, 18));

        // Values Regular
        e.setBondValueRegular(getDecimal(row, 19));
        e.setBalanceValueRegular(getDecimal(row, 20));
        e.setGrowthValueRegular(getDecimal(row, 21));
        e.setShariaValueRegular(getDecimal(row, 22));

        // Values Topup
        e.setBondValueTopup(getDecimal(row, 23));
        e.setBalanceValueTopup(getDecimal(row, 24));
        e.setGrowthValueTopup(getDecimal(row, 25));
        e.setShariaValueTopup(getDecimal(row, 26));

        e.setTotalFundValue(getDecimal(row, 27));
        e.setSwitchingFrequency(getInt(row, 28));
        e.setInternalFund(getString(row, 29));

        return e;
    }

    /* ================== HELPERS ================== */

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private BigDecimal getDecimal(Row row, int col) {
        String val = getString(row, col);

        if (val == null || val.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInt(Row row, int col) {
        BigDecimal bd = getDecimal(row, col);
        return bd == null ? null : bd.intValue();
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