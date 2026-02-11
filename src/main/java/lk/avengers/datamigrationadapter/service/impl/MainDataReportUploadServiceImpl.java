package lk.avengers.datamigrationadapter.service.impl;


import com.github.pjfanning.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lk.avengers.datamigrationadapter.service.CommonFunction;
import lk.avengers.datamigrationadapter.service.MainDataReportUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainDataReportUploadServiceImpl implements MainDataReportUploadService {

    private static final int BATCH_SIZE = 1000;
    private static final int HEADER_ROW_1 = 0;

    private final BatchProcessService genisysBatchService;
    private final ResourceLoader resourceLoader;
    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final CommonFunction commonFunction;

    @Value("${mainDataReport.file}")
    private String mainDataReportFilePath;

    @Value("${mainDataAlhReport.file}")
    private String mainDataALHReportFilePath;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public ResponseEntity<CommonResponseDTO> uploadMainDataReports() {
        log.info("uploadMainDataReports called");
        try {
            return upload(
                    this::getMainDataReportFileInputStream,
                    this::mapExcelRowsToMainDataReportEntity,
                    true
            );
        } catch (Exception e) {
            log.error("Error in uploadMainDataReports: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<CommonResponseDTO> uploadMainDataALHReportExcel() {
        log.info("uploadMainDataALLReportExcel called");
        try {
            return uploadALHReport(
                    this::getMainDataALHReportFileInputStream,
                    this::mapExcelRowsToMainDataALHReportEntity,
                    true
            );
        } catch (Exception e) {
            log.error("Error in uploadMainDataALLReportExcel: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // -------------------------------------------------------------------------
    // Core Upload Template (shared logic)
    // -------------------------------------------------------------------------

    private ResponseEntity<CommonResponseDTO> upload(
            Supplier<InputStream> inputStreamSupplier,
            BiConsumer<Row, List<MainDataReportEntity>> rowMapper,
            boolean truncateBeforeInsert
    ) {
        List<MainDataReportEntity> batch = new ArrayList<>();
        Row currentRow = null;
        int totalCount = 0;

        try (InputStream is = inputStreamSupplier.get();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(1000)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (truncateBeforeInsert) {
                mainDataReportRepository.truncate();
                log.info("Existing Main Data Report records truncated");
            }

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
                    genisysBatchService.saveMainDataBatch(batch);
                    totalCount += batch.size();
                    log.info("Saved {} Main data records so far...", totalCount);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                genisysBatchService.saveMainDataBatch(batch);
                totalCount += batch.size();
            }

            log.info("Upload completed. Total records saved: {}", totalCount);

        } catch (IOException e) {
            log.error(
                    "Error processing file. Row index: {}",
                    currentRow != null ? currentRow.getRowNum() : "N/A",
                    e
            );
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to parse Excel file: " + e.getMessage()
            );
        }

        return ResponseEntity.ok(
                CommonResponseDTO.builder()
                        .message("Main data report " + totalCount + " records uploaded successfully.")
                        .status(HttpStatus.OK.toString())
                        .build()
        );
    }

    private ResponseEntity<CommonResponseDTO> uploadALHReport(
            Supplier<InputStream> inputStreamSupplier,
            BiConsumer<Row, List<MainDataALHReportEntity>> rowMapper,
            boolean truncateBeforeInsert
    ) {
        log.info("called uploadALHReport method.");
        List<MainDataALHReportEntity> batch = new ArrayList<>();
        Row currentRow = null;
        int totalCount = 0;

        try (InputStream is = inputStreamSupplier.get();
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(1000)
                     .bufferSize(4096)
                     .open(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (truncateBeforeInsert) {
                mainDataALHReportRepository.truncate();
                log.info("Existing Main Data ALH Report records truncated");
            }

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
                    genisysBatchService.saveALHBatch(batch);
                    totalCount += batch.size();
                    log.info("Saved {} ALH records so far...", totalCount);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                genisysBatchService.saveALHBatch(batch);
                totalCount += batch.size();
            }

            log.info("ALH upload completed. Total records saved: {}", totalCount);

        } catch (IOException e) {
            log.error(
                    "Error processing ALH file. Row index: {}",
                    currentRow != null ? currentRow.getRowNum() : "N/A",
                    e
            );
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to parse Excel file: " + e.getMessage()
            );
        }

        return ResponseEntity.ok(
                CommonResponseDTO.builder()
                        .message("Main data ALH report " + totalCount + " records uploaded successfully.")
                        .status(HttpStatus.OK.toString())
                        .build()
        );
    }

    // -------------------------------------------------------------------------
    // Row filtering
    // -------------------------------------------------------------------------

    private boolean shouldSkipRow(Row row) {
        return row.getRowNum() == HEADER_ROW_1;
        // return isCellBlank(row.getCell(1)) || isCellBlank(row.getCell(3));
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

    // -------------------------------------------------------------------------
    // Excel → Entity Mapping (UNCHANGED LOGIC)
    // -------------------------------------------------------------------------

    private void mapExcelRowsToMainDataReportEntity(Row row, List<MainDataReportEntity> list) {
        int col = 0;
        MainDataReportEntity entity = MainDataReportEntity.builder()
                // id - auto generated, skip
                .policyNo(commonFunction.getIntegerValue(row.getCell(col++)))
                .proposalNo(commonFunction.getIntegerValue(row.getCell(col++)))
                .productCode(commonFunction.getStringValue(row.getCell(col++)))
                .planNo(commonFunction.getStringValue(row.getCell(col++)))
                // Dates & Terms

                .inception(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .expiry(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .issueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .salesBranchCode(commonFunction.getIntegerValue(row.getCell(col++)))
                .salesBranchName(commonFunction.getStringValue(row.getCell(col++)))
                .companyBranchCode(commonFunction.getIntegerValue(row.getCell(col++)))
                .companyBranchName(commonFunction.getStringValue(row.getCell(col++)))
                .policyBranchCode(commonFunction.getIntegerValue(row.getCell(col++)))
                .policyBranchName(commonFunction.getStringValue(row.getCell(col++)))
                .term(commonFunction.getIntegerValue(row.getCell(col++)))
                .cy(commonFunction.getStringValue(row.getCell(col++)))
                .premiumPaymentTerm(commonFunction.getStringValue(row.getCell(col++)))
                .defermentTerm(commonFunction.getIntegerValue(row.getCell(col++)))
                .retirementBenefitPayoutTerm(commonFunction.getIntegerValue(row.getCell(col++)))
                .modalPremium(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .frequency(commonFunction.getIntegerValue(row.getCell(col++)))
                .nextPremium(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .status(commonFunction.getStringValue(row.getCell(col++)))
                .statusDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .reason(commonFunction.getStringValue(row.getCell(col++)))
                .agentCode(commonFunction.getStringValue(row.getCell(col++)))
                .introducer(commonFunction.getStringValue(row.getCell(col++)))
                .supervisor(commonFunction.getStringValue(row.getCell(col++)))
                .riPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // Main Life Details
                .pin(commonFunction.getIntegerValue(row.getCell(col++)))
                .title(commonFunction.getStringValue(row.getCell(col++)))
                .fullName(commonFunction.getStringValue(row.getCell(col++)))
                .gender(commonFunction.getStringValue(row.getCell(col++)))
                .dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .aae(commonFunction.getIntegerValue(row.getCell(col++)))
                .sarChoice(commonFunction.getStringValue(row.getCell(col++)))
                .numberOfRidersTaken(commonFunction.getIntegerValue(row.getCell(col++)))
                .dth_Sar(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subDth_(commonFunction.getIntegerValue(row.getCell(col++)))
                .subRateMilDth_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .dth_OccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                .accd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subAccd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilAccd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .accd_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .accp_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subAccp_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilAccp_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .accp_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .acct_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subAcct_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilAcct_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .acct_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                // CILL
                .cill_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subCill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilCill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .cill_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // CILX
                .cilx_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subCilx_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilCilx_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .cilx_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // FIB
                .fib_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subFib_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilFib_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .fib_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // FIBT
                .fibt_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subFibt_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilFibt_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .fibt_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // FSEB
                .fseb_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subFseb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilFseb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .fseb_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // LEB
                .leb_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subLeb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilLeb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .leb_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // PTD
                .ptd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subPtd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilPtd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .ptd_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // TILL
                .till_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subTill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilTill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .till_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // TR
                .tr_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subTr_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilTr_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .tr_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // WOPA
                .wopa_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subWopa_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopa_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .wopa_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // WOPC
                .wopc_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subWopc_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopc_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .wopc_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // WOPD
                .wopd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subWopd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .wopd_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // FSEBA
                .fseba_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subFseba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilFseba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .fseba_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // HBA
                .hba_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subHba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilHba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .hba_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // HBAC
                .hbac_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subHbac_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .subRateMilHbac_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .hbac_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                // Spouse/Child
                .spouseChildPin(commonFunction.getIntegerValue(row.getCell(col++)))
                .spouseChildTitle(commonFunction.getStringValue(row.getCell(col++)))
                .spouseChildFullName(commonFunction.getStringValue(row.getCell(col++)))
                .spouseChildGender(commonFunction.getStringValue(row.getCell(col++)))
                .spouseChildDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .spouseChildAge(commonFunction.getIntegerValue(row.getCell(col++)))
                .spouseDeath_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseSubDeath_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseSubRateMilDeath_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseDeath_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildAccd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAccd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAccd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildAccd_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildAccp_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAccp_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAccp_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildAccp_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildAcct_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAcct_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAcct_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildAcct_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildCill_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubCill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilCill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildCill_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildCilx_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubCilx_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilCilx_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildCilx_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildLeb_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubLeb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilLeb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildLeb_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildPtd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubPtd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilPtd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildPtd_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildTill_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubTill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilTill_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildTill_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildHb_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubHb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilHb_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildHb_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseChildPpd_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubPpd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilPpd_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseChildPpd_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                .spouseHba_Sa(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseSubHba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseSubRateMilHba_(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .spouseHba_OccupationalLoadingPercent(commonFunction.getDoubleValue(row.getCell(col++)))
                // Children 1..5
                .child1Name(commonFunction.getStringValue(row.getCell(col++)))
                .child1Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child1Age(commonFunction.getIntegerValue(row.getCell(col++)))
                .child1Hbc_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child1Hbcac_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child2Name(commonFunction.getStringValue(row.getCell(col++)))
                .child2Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child2Age(commonFunction.getIntegerValue(row.getCell(col++)))
                .child2Hbc_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child2Hbcac_(commonFunction.getStringValue(row.getCell(col++)))
                .child3Name(commonFunction.getStringValue(row.getCell(col++)))
                .child3Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child3Age(commonFunction.getIntegerValue(row.getCell(col++)))
                .child3Hbc_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child3Hbcac_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child4Name(commonFunction.getStringValue(row.getCell(col++)))
                .child4Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child4Age(commonFunction.getIntegerValue(row.getCell(col++)))
                .child4Hbc_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child4Hbcac_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child5Name(commonFunction.getStringValue(row.getCell(col++)))
                .child5Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child5Age(commonFunction.getIntegerValue(row.getCell(col++)))
                .child5Hbc_(commonFunction.getIntegerValue(row.getCell(col++)))
                .child5Hbcac_(commonFunction.getIntegerValue(row.getCell(col++)))
                // Financial Summary
                .basicSumAssured(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .interestRate(commonFunction.getDoubleValue(row.getCell(col++)))
                .tpdPremiumLife1(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .tpdPremiumLife2(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .valueToday(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .prmValueToday(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .bstValueToday(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .transactionAmount(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .interestCredited(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .surrenderValue(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .prmSurrenderValue(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .bstSurrenderValue(commonFunction.getBigDecimalValue(row.getCell(col++)))
                .insuranceCoveragePeriod(commonFunction.getIntegerValue(row.getCell(col++)))
                .operationDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .lastPaymentDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .lastPremiumDueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .premiumEscalationBenefitPercentage(commonFunction.getDoubleValue(row.getCell(col++)))
                .refundValue(commonFunction.getBigDecimalValue(row.getCell(col++)))
                // createdAt - handled by @CreatedDate
                .build();

        list.add(entity);
    }

    /**
     * Maps Excel row to MainDataALHReportEntity using MainDataReport2 column order (0-based):
     * POLICY NO, PROPOSAL NO, Product Code, PLAN NO, INCEPTION, EXPIRY, Issue Date,
     * Sales Branch Code, Sales Branch Name, Company Branch Code, Company Branch Name,
     * Policy Branch Code, Policy Branch Name, TERM, C/Y, Premium Payment Term, MODAL PREMIUM,
     * FREQUENCY, NEXT PREMIUM, STATUS, DATE, Operation Date, Reason, AGENT CODE, Introducer,
     * Supervisor, RI %, PIN, TITLE, FULL NAME, GENDER, DOB, AAE, Occupation Main Life, SAR CHOICE,
     * Number of Riders Taken, DTH SAR, SUB-DTH, SUB Rate/Mil-DTH, DTH Occupational Loading %,
     * HB SA, SUB-HB, SUB Rate/Mil-HB, HB Occupational Loading %, INP SAR, SUB-INP, SUB Rate/Mil-INP,
     * INP Occupational Loading %, ML Bonus, Spouse PIN..Spouse INP Occupational Loading %,
     * CHILD1..CHILD20 (each: NAME, DOB, AGE, HBC, INP SAR, Bonus), Basic Sum Assured,
     * Insurance Coverage Period, Last Payment Date, Last Premium Due Date, Refund Value.
     */
    private void mapExcelRowsToMainDataALHReportEntity(Row row, List<MainDataALHReportEntity> list) {
        MainDataALHReportEntity entity = MainDataALHReportEntity.builder().build();
        int c = 0;

        // Policy & identifiers (0-3)
        entity.setPolicyNo(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setProposalNo(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setProductCode(commonFunction.getStringValue(row.getCell(c++)));
        entity.setPlanNo(commonFunction.getStringValue(row.getCell(c++)));
        // Dates (4-6)
        entity.setInception(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setExpiry(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setIssueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        // Branches (7-12)
        entity.setSalesBranchCode(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setSalesBranchName(commonFunction.getStringValue(row.getCell(c++)));
        entity.setCompanyBranchCode(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setCompanyBranchName(commonFunction.getStringValue(row.getCell(c++)));
        entity.setPolicyBranchCode(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setPolicyBranchName(commonFunction.getStringValue(row.getCell(c++)));
        // Term & premium (13-18)
        entity.setTerm(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setCy(commonFunction.getStringValue(row.getCell(c++)));
        entity.setPremiumPaymentTerm(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setModalPremium(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setFrequency(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setNextPremium(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        // Status & dates (19-22)
        entity.setStatus(commonFunction.getStringValue(row.getCell(c++)));
        entity.setDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setOperationDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setReason(commonFunction.getStringValue(row.getCell(c++)));
        // Agent (23-26)
        entity.setAgentCode(commonFunction.getStringValue(row.getCell(c++)));
        entity.setIntroducer(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSupervisor(commonFunction.getStringValue(row.getCell(c++)));
        entity.setRiPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        // Main life (27-35)
        entity.setPin(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setTitle(commonFunction.getStringValue(row.getCell(c++)));
        entity.setFullName(commonFunction.getStringValue(row.getCell(c++)));
        entity.setGender(commonFunction.getStringValue(row.getCell(c++)));
        entity.setDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setAae(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setOccupationMainLife(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSarChoice(commonFunction.getStringValue(row.getCell(c++)));
        entity.setNumberOfRidersTaken(commonFunction.getIntegerValue(row.getCell(c++)));
        // DTH (36-39)
        entity.setDth_Sar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSubDth_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setSubRateMilDth_(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setDth_OccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        // HB (40-43)
        entity.setHb_Sa(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSubHb_(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSubRateMilHb_(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setHb_OccupationalLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        // INP (44-47)
        entity.setInpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSubInp(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSubRateMilInp(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setInpOccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        // ML Bonus (48)
        entity.setMlBonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        // Spouse (49-68)
        entity.setSpousePin(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setSpouseTitle(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSpouseFullName(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSpouseGender(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSpouseDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setSpouseAge(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setOccupationSpouse(commonFunction.getStringValue(row.getCell(c++)));
        entity.setSpouseDeathSa(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubDeath(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setSpouseSubRateMilDeath(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseDeathOccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        entity.setSpouseHbSa(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubHb(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubRateMilHb(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseHbOccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        entity.setSpouseBonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseInpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubInp(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubRateMilInp(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseInpOccLoadingPercentage(commonFunction.getDoubleValue(row.getCell(c++)));
        // Children 1-5 (NAME, DOB, AGE, HBC, INP SAR, Bonus)
        entity.setChild1Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild1Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild1Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild1Hbc_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild1InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild1Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild2Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild2Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild2Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild2Hbc_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild2InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild2Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild3Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild3Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild3Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild3Hbc_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild3InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild3Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild4Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild4Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild4Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild4Hbc_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild4InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild4Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild5Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild5Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild5Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild5Hbc_(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild5InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild5Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        // Children 6-20 (each: NAME, DOB, AGE, HBC, INP SAR, Bonus)
        entity.setChild6Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild6Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild6Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild6Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild6InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild6Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild7Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild7Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild7Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild7Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild7InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild7Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild8Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild8Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild8Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild8Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild8InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild8Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild9Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild9Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild9Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild9Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild9InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild9Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild10Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild10Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild10Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild10Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild10InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild10Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild11Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild11Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild11Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild11Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild11InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild11Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild12Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild12Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild12Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild12Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild12InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild12Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild13Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild13Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild13Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild13Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild13InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild13Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild14Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild14Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild14Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild14Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild14InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild14Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild15Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild15Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild15Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild15Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild15InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild15Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild16Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild16Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild16Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild16Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild16InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild16Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild17Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild17Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild17Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild17Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild17InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild17Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild18Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild18Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild18Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild18Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild18InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild18Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild19Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild19Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild19Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild19Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild19InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild19Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild20Name(commonFunction.getStringValue(row.getCell(c++)));
        entity.setChild20Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild20Age(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setChild20Hbc(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild20InpSar(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setChild20Bonus(commonFunction.getBigDecimalValue(row.getCell(c++)));
        // Basic Sum Assured, Insurance Coverage Period, Last Payment Date, Last Premium Due Date, Refund Value
        entity.setBasicSumAssured(commonFunction.getBigDecimalValue(row.getCell(c++)));
        entity.setInsuranceCoveragePeriod(commonFunction.getIntegerValue(row.getCell(c++)));
        entity.setLastPaymentDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setLastPremiumDueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setRefundValue(commonFunction.getBigDecimalValue(row.getCell(c++)));

        list.add(entity);
    }

    private String getStringDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return cell.getStringCellValue().trim();
            }
            return null;
        } catch (Exception e) {
            log.error("Error parsing date value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // File Loading
    // -------------------------------------------------------------------------

    private InputStream getMainDataReportFileInputStream() {
        return getFileInputStream(mainDataReportFilePath, "Main Data Report");
    }

    private InputStream getMainDataALHReportFileInputStream() {
        return getFileInputStream(mainDataALHReportFilePath, "Main Data ALH Report");
    }

    private InputStream getFileInputStream(String path, String name) {
        try {
            Resource resource = resourceLoader.getResource("file:" + path);
            if (!resource.exists()) {
                throw new ReportException(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        name + " file not found at path: " + path
                );
            }
            return resource.getInputStream();
        } catch (IOException e) {
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error reading " + name + " file: " + e.getMessage()
            );
        }
    }
}

