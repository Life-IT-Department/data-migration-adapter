package lk.avengers.datamigrationadapter.service.impl;

import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PremiumDetailsRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lk.avengers.datamigrationadapter.service.CommonFunction;
import lk.avengers.datamigrationadapter.service.PremiumDetailsReportsService;
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
import java.util.ArrayList;
import java.util.List;




@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumDetailsReportsServiceImpl implements PremiumDetailsReportsService {

    private final CommonFunction commonFunction;
    private final PremiumDetailsRepository premiumDetailsRepository;
    private final BatchProcessService genisysBatchService;

    @Value("${premiumDetailsReport.file}")
    private String premiumDetailsReportsPath;

    private static final int HEADER_ROW_1 = 0;
    private static final int HEADER_ROW_2 = 1;
    private static final int HEADER_ROW_3 = 2;
    private static final int HEADER_ROW_4 = 3;
    private static final int HEADER_ROW_5 = 4;
    private static final int BATCH_SIZE = 1000;


    @Override
    public ResponseEntity<CommonResponseDTO> uploadPremiumDetailsReports() {
        log.info("Premium Details Reports Upload (multiple files)");
        String fileName = "";
        try {
            List<Path> files = getFilesPath();
            log.info("Found {} Premium Details file(s) to process.", files.size());

            // truncate once before inserting all files
//            premiumDetailsRepository.truncate();
//            log.info("Existing Premium Details Reports records truncated");

            int totalCount = 0;

            for (Path file : files) {
                log.info("Processing file: {}", file.getFileName());
                fileName = file.getFileName().toString();
                Row currentRow = null;
                int reportRecordCount = 0;
                try (InputStream is = Files.newInputStream(file);
                     Workbook workbook = StreamingReader.builder()
                             .rowCacheSize(1000)
                             .bufferSize(4096)
                             .open(is)) {

                    Sheet sheet = workbook.getSheetAt(0);
                    List<PremiumDetailsEntity> batch = new ArrayList<>();

                    for (Row row : sheet) {
                        currentRow = row;

                        if (shouldSkipRow(row)) {
                            continue;
                        }

                        if (commonFunction.isEndOfDataRow(row)) {
                            break;
                        }

                        mapExcelRowsToPremiumDetailsEntity(row, batch);

                        if (batch.size() == BATCH_SIZE) {
                            genisysBatchService.savePremiumDetailsBatch(batch);
                            totalCount += batch.size();
                            reportRecordCount += batch.size();
                            log.info("Saved {} records from {} Premium Details file so far...", reportRecordCount, fileName);
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        genisysBatchService.savePremiumDetailsBatch(batch);
                        totalCount += batch.size();
                        reportRecordCount += batch.size();
                        log.info("Saved {} records from {} Premium Details file so far...", reportRecordCount, fileName);
                    }

                } catch (IOException e) {
                    log.error("Error processing Premium Details file: {}. Row index: {}", file.getFileName(),
                            currentRow != null ? currentRow.getRowNum() : "N/A", e);
                    throw new ReportException(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to parse Premium Details Excel file: " + file.getFileName() + ": " + e.getMessage()
                    );
                }
                log.info("Premium Details file: {} processed successfully. Total records saved: {}", fileName, reportRecordCount);
            }

            log.info("All Premium Details Excel files Upload completed. Total records saved: {}", totalCount);

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message("Premium Details report " + totalCount + " records uploaded successfully.")
                            .status(HttpStatus.OK.toString())
                            .build()
            );
        } catch (ReportException re) {
            throw re;
        } catch (Exception e) {
            log.error("Error in Premium Details Report -> file name: {} error: {}", fileName, e.getMessage());
            CommonResponseDTO commonResponseDTO = CommonResponseDTO.builder()
                    .message(e.getMessage())
                    .status(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                    .build();
            return ResponseEntity.internalServerError().body(commonResponseDTO);
        }
    }

    private void mapExcelRowsToPremiumDetailsEntity(Row row, List<PremiumDetailsEntity> list) {
        PremiumDetailsEntity premiumDetailsEntity = PremiumDetailsEntity.builder()
                .policyNo(commonFunction.getIntegerValue(row.getCell(1)))
                .proposalNo(commonFunction.getIntegerValue(row.getCell(2)))
                .productCode(commonFunction.getStringValue(row.getCell(3)))
                .planNo(commonFunction.getStringValue(row.getCell(4)))
                .inceptionDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(5))))
                .expiryDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(6))))
                .issueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(7))))
                .salesBranchCode(commonFunction.getIntegerValue(row.getCell(8)))
                .salesBranchName(commonFunction.getStringValue(row.getCell(9)))
                .companyBranchCode(commonFunction.getIntegerValue(row.getCell(10)))
                .companyBranchName(commonFunction.getStringValue(row.getCell(11)))
                .policyBranchCode(commonFunction.getIntegerValue(row.getCell(12)))
                .policyBranchName(commonFunction.getStringValue(row.getCell(13)))
                .term(commonFunction.getIntegerValue(row.getCell(14)))
                .cy(commonFunction.getStringValue(row.getCell(15)))
                .premiumPaymentTerm(getPremiumPaymentTerm(row.getCell(16)))
                .defermentTerm(commonFunction.getIntegerValue(row.getCell(17)))
                .retirementBenefitPayoutTerm(commonFunction.getIntegerValue(row.getCell(18)))
                .modalPremium(commonFunction.getDoubleValue(row.getCell(19)))
                .frequency(commonFunction.getIntegerValue(row.getCell(20)))
                .nextPremium(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(21))))
                .status(commonFunction.getStringValue(row.getCell(22)))
                .date(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(23))))
                .reason(commonFunction.getStringValue(row.getCell(24)))
                .agentCode(commonFunction.getStringValue(row.getCell(25)))
                .introducer(commonFunction.getStringValue(row.getCell(26)))
                .supervisor(commonFunction.getStringValue(row.getCell(27)))
                .riPercentage(commonFunction.getDoubleValue(row.getCell(28)))
                .policyYear(commonFunction.getIntegerValue(row.getCell(29)))
                .policyMonth(commonFunction.getIntegerValue(row.getCell(30)))
                .modalPremiumAmount(commonFunction.getDoubleValue(row.getCell(31)))
                .allocationAmount(commonFunction.getDoubleValue(row.getCell(32)))
                .paymentDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(33))))
                .premiumDueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(34))))
                .build();
        list.add(premiumDetailsEntity);
    }

    private List<Path> getFilesPath() {
        log.info("PremiumDetailsReportsService: Getting files path from directory: {}", premiumDetailsReportsPath);
        try {
            Path configuredPath = Paths.get(premiumDetailsReportsPath);
            Path directoryPath;

            // Resolve directory to search
            if (Files.exists(configuredPath) && Files.isDirectory(configuredPath)) {
                directoryPath = configuredPath;
            } else if (configuredPath.getFileName().toString().contains(".")) {
                // If configuredPath looks like a file path, use its parent directory (or current dir if null)
                directoryPath = configuredPath.getParent();
                if (directoryPath == null) {
                    directoryPath = Paths.get(".");
                }
            } else {
                // Treat as non-existing directory name -> use current directory
                directoryPath = Paths.get(".");
            }

            // Use try-with-resources to ensure the stream from Files.list is closed
            try (var stream = Files.list(directoryPath)) {
                List<Path> excelFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".xlsm");
                        })
                        .toList();

                if (excelFiles.isEmpty()) {
                    throw new ReportException(
                            HttpStatus.NOT_FOUND.value(),
                            "No Premium Details Excel files found in directory: " + directoryPath.toAbsolutePath()
                    );
                }
                return excelFiles;
            }

        } catch (IOException e) {
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error listing Premium Details Excel files from path " + premiumDetailsReportsPath + ": " + e.getMessage()
            );
        }
    }

    private Integer getPremiumPaymentTerm(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.STRING) {
            String cellValue = cell.getStringCellValue().trim();
            if (cellValue.equalsIgnoreCase("SP")) {
                return 1;
            }
        }
        return commonFunction.getIntegerValue(cell);
    }

    private boolean shouldSkipRow(Row row) {
        return row.getRowNum() == HEADER_ROW_1 || row.getRowNum() == HEADER_ROW_2
                || row.getRowNum() == HEADER_ROW_3 || row.getRowNum() == HEADER_ROW_4 || row.getRowNum() == HEADER_ROW_5;
    }

    private String getStringDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return cell.getStringCellValue().trim();
        } catch (Exception e) {
            log.error("Error parsing date value from cell: {} cell address: {}", cell, cell.getAddress());
            return null;
        }
    }
}
