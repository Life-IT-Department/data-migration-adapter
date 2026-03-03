package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.CashFlowReportRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lk.avengers.datamigrationadapter.service.CashFlowReportUploadService;
import lk.avengers.datamigrationadapter.service.CommonFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowReportUploadServiceImpl implements CashFlowReportUploadService {

    @Value("${cashFlowReport.file}")
    private String cashFlowReportPath;

    private final BatchProcessService genisysBatchService;
    private final CashFlowReportRepository cashFlowReportRepository;
    private final CommonFunction commonFunction;

    private static final int HEADER_ROW_1 = 0;
    private static final int HEADER_ROW_2 = 1;
    private static final int HEADER_ROW_3 = 2;
    private static final int HEADER_ROW_4 = 3;
    private static final int BATCH_SIZE = 1000;

    @Override
    public ResponseEntity<CommonResponseDTO> uploadCashFlowReport(int year) {
        log.info("uploadCashFlowReport called");
        try {
            return upload(
                    () -> getFileInputStreamByYear(year),
                    (row, list) -> mapExcelRowsToCashFlowReportEntity(row, list, year),
                    year
            );
        } catch (Exception e) {
            log.error("Error in uploadCashFlowReport: {}", e.getMessage());
            CommonResponseDTO commonResponseDTO = new CommonResponseDTO();
            commonResponseDTO.setMessage(e.getMessage());
            commonResponseDTO.setStatus(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));
            return ResponseEntity.internalServerError().body(commonResponseDTO);
        }
    }


    private ResponseEntity<CommonResponseDTO> upload(
            Supplier<InputStream> inputStreamSupplier,
            BiConsumer<Row, List<CashFlowReportEntity>> rowMapper,
            int year
    ) {
        List<CashFlowReportEntity> batch = new ArrayList<>();
        Row currentRow = null;
        int totalCount = 0;

        try (InputStream is = inputStreamSupplier.get();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(1000)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);


            cashFlowReportRepository.deleteAllByYear(year);
            log.info("{} Existing cash flow Report records truncated", year);


            for (Row row : sheet) {
                currentRow = row;

                if (shouldSkipRow(row)) {
                    continue;
                }
                if (isEndOfDataRow(row)) {
                    break;
                }

                rowMapper.accept(row, batch);

                if (batch.size() == BATCH_SIZE) {
                    genisysBatchService.saveCashFlowBatch(batch);
                    totalCount += batch.size();
                    log.info("Saved {} cash flow records so far...", totalCount);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                genisysBatchService.saveCashFlowBatch(batch);
                totalCount += batch.size();
            }

            log.info("Upload completed. Total records saved: {}", totalCount);

        } catch (IOException e) {
            log.error(
                    "Error processing cash flow file. Row index: {}",
                    currentRow != null ? currentRow.getRowNum() : "N/A",
                    e
            );
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to parse Excel cash flow file: " + e.getMessage()
            );
        }

        return ResponseEntity.ok(
                CommonResponseDTO.builder()
                        .message("Cash Flow report " + totalCount + " records uploaded successfully.")
                        .status(HttpStatus.OK.toString())
                        .build()
        );
    }

    private void mapExcelRowsToCashFlowReportEntity(Row row, List<CashFlowReportEntity> list, int year) {
        CashFlowReportEntity cashFlowReportEntity = CashFlowReportEntity.builder()
                .fiscalYear(commonFunction.getIntegerValue(row.getCell(1)))
                .ACDocument(commonFunction.getStringValue(row.getCell(2)))
                .OperationDate(commonFunction.getDateFromInteger(commonFunction.getStringDateValue(row.getCell(3))))
                .time(commonFunction.getLocalTimeValue(row.getCell(4)))
                .station(commonFunction.getIntegerValue(row.getCell(5)))
                .receiptNo(commonFunction.getIntegerValue(row.getCell(6)))
                .payerPin(getStringValue(row.getCell(7)))
                .payerName(commonFunction.getStringValue(row.getCell(8)))
                .payerAddress(commonFunction.getStringValue(row.getCell(9)))
                .details(commonFunction.getStringValue(row.getCell(10)))
                .paymentMode(commonFunction.getStringValue(row.getCell(11)))
                .reference(commonFunction.getStringValue(row.getCell(12)))
                .description(commonFunction.getStringValue(row.getCell(13)))
                .accPin(commonFunction.getIntegerValue(row.getCell(14)))
                .accSeq(commonFunction.getIntegerValue(row.getCell(15)))
                .checkNo(commonFunction.getIntegerValue(row.getCell(16)))
                .checkDate(commonFunction.getDateFromInteger(commonFunction.getStringDateValue(row.getCell(17))))
                .checkStatus(commonFunction.getStringValue(row.getCell(18)))
                .paymentType(commonFunction.getStringValue(row.getCell(19)))
                .agency(commonFunction.getStringValue(row.getCell(20)))
                .drawnBank(commonFunction.getStringValue(row.getCell(21)))
                .clearingBank(commonFunction.getStringValue(row.getCell(22)))
                .amountLC(commonFunction.getBigDecimalValue(row.getCell(23)))
                .postedBy(commonFunction.getStringValue(row.getCell(24)))
                .paidAmount(commonFunction.getBigDecimalValue(row.getCell(25)))
                .totalAmount(commonFunction.getBigDecimalValue(row.getCell(26)))
                .receiptCancellation(commonFunction.getStringValue(row.getCell(27)))
                .reason(commonFunction.getStringValue(row.getCell(28)))
                .authorizer(commonFunction.getStringValue(row.getCell(29)))
                .year(year)
                .build();
        list.add(cashFlowReportEntity);
    }

    private InputStream getFileInputStreamByYear(int year) {
        try {
            Path directoryPath = Paths.get(cashFlowReportPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                throw new ReportException(
                        HttpStatus.NOT_FOUND.value(),
                        "Invalid cash flow directory path: " + directoryPath.toAbsolutePath()
                );
            }

            String expectedPrefix = "CashFlowTrn-" + year;

            List<Path> matchingFiles;

            try (Stream<Path> stream = Files.list(directoryPath)) {
                matchingFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.startsWith(expectedPrefix.toLowerCase())
                                    && (fileName.endsWith(".xlsx")
                                    || fileName.endsWith(".xls")
                                    || fileName.endsWith(".xlsm"));
                        })
                        .toList();
            }

            if (matchingFiles.isEmpty()) {
                throw new ReportException(
                        HttpStatus.NOT_FOUND.value(),
                        "No cash flow file found for year " + year +
                                ". Expected format: CashFlowTrn-" + year + ".xlsx"
                );
            }

            if (matchingFiles.size() > 1) {
                String fileNames = matchingFiles.stream()
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.joining(", "));
                throw new ReportException(
                        HttpStatus.CONFLICT.value(),
                        "Multiple files found for year " + year + ": " + fileNames
                );
            }

            return Files.newInputStream(matchingFiles.get(0));

        } catch (IOException e) {
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error reading cash flow file for year " + year + ": " + e.getMessage()
            );
        }
    }

    private boolean shouldSkipRow(Row row) {
        return row.getRowNum() == HEADER_ROW_1 || row.getRowNum() == HEADER_ROW_2
                || row.getRowNum() == HEADER_ROW_3 || row.getRowNum() == HEADER_ROW_4 || isCellBlank(row.getCell(1));
    }

    /**
     * Returns true if all columns in the row are empty or blank, meaning we have passed the last data row.
     * Processing stops when this returns true.
     */
    private boolean isEndOfDataRow(Row row) {
        if (row == null) {
            return true;
        }
        int lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            if (!isCellBlank(row.getCell(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isCellBlank(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().isBlank();
        }
        return false;
    }

    private String getStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> String.valueOf(cell.getStringCellValue()).trim();
                default -> null;
            };
        } catch (Exception e) {
            log.error("Error parsing string value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }
}
