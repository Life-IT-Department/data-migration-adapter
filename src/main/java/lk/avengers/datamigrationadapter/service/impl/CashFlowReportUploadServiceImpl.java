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

@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowReportUploadServiceImpl implements CashFlowReportUploadService {

    @Value("${cashFlowReport.file}")
    private String cashFlowReportPath;

    private final BatchProcessService genisysBatchService;
    private final CashFlowReportRepository cashFlowReportRepository;
    private final CommonFunction commonFunction;
    DataFormatter dataFormatter = new DataFormatter();
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
                // Stop at first empty row (past last data row)
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
                        .message("Main data report " + totalCount + " records uploaded successfully.")
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
            // Get the directory path from the configured path
            Path directoryPath;
            Path configuredPath = Paths.get(cashFlowReportPath);

            // Check if the configured path is a directory or file
            if (Files.exists(configuredPath) && Files.isDirectory(configuredPath)) {
                // If it's an existing directory, use it directly
                directoryPath = configuredPath;
            } else if (cashFlowReportPath.contains("/") || cashFlowReportPath.contains("\\")) {
                // If path contains directory separators, check if it's meant to be a directory
                // or get the parent directory if it's a file path
                if (configuredPath.toString().endsWith("/") || configuredPath.toString().endsWith("\\") ||
                        !configuredPath.getFileName().toString().contains(".")) {
                    // Treat as directory path
                    directoryPath = configuredPath;
                } else {
                    // Get parent directory
                    directoryPath = configuredPath.getParent();
                    if (directoryPath == null) {
                        directoryPath = Paths.get(".");
                    }
                }
            } else {
                // If it's just a filename, use current directory
                directoryPath = Paths.get(".");
            }

            // Pattern to match year anywhere in filename as a standalone number (supports files with or without Excel extensions)
            // This pattern looks for the year as a 4-digit number that's either:
            // - At the start of filename followed by non-digit
            // - Preceded by non-digit and followed by non-digit  
            // - At the end of filename preceded by non-digit
            Pattern yearPattern = Pattern.compile(".*(?:^|[^\\d])" + year + "(?:[^\\d]|$).*", Pattern.CASE_INSENSITIVE);

            log.info("Searching for cash flow files for year {} in directory: {}", year, directoryPath.toAbsolutePath());
            log.info("Using pattern: {}", yearPattern.pattern());

            // Find all Excel files in the directory that match the year pattern
            List<Path> matchingFiles = Files.list(directoryPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        // Only consider Excel files
                        return fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".xlsm");
                    })
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        Matcher matcher = yearPattern.matcher(fileName);
                        boolean matches = matcher.matches();
                        log.debug("Checking file '{}' against pattern: {}", fileName, matches);
                        return matches;
                    })
                    .toList();

            // Handle different scenarios
            if (matchingFiles.isEmpty()) {
                // List all Excel files in the directory for debugging
                List<String> allExcelFiles = Files.list(directoryPath)
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".xlsm");
                        })
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());

                String debugInfo = allExcelFiles.isEmpty() ?
                        "No Excel files found in directory." :
                        "Available Excel files: " + String.join(", ", allExcelFiles);

                throw new ReportException(
                        HttpStatus.NOT_FOUND.value(),
                        "No cash flow file found for year " + year + " in directory: " + directoryPath.toAbsolutePath() +
                                ". " + debugInfo + " Expected filename pattern: contains '" + year + "' as standalone number."
                );
            }

            if (matchingFiles.size() > 1) {
                String fileNames = matchingFiles.stream()
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.joining(", "));
                throw new ReportException(
                        HttpStatus.CONFLICT.value(),
                        "Multiple cash flow files found for year " + year + ": " + fileNames +
                                ". Please ensure only one file exists for the specified year."
                );
            }

            // Single file found - return its input stream
            Path selectedFile = matchingFiles.getFirst();
            log.info("Found cash flow file for year {}: {}", year, selectedFile.getFileName());

            return Files.newInputStream(selectedFile);

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
