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
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainDataReportUploadServiceImpl implements MainDataReportUploadService {

    private static final int BATCH_SIZE = 1000;
    private static final int HEADER_ROW_1 = 0;
    private static final int HEADER_ROW_2 = 1;

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

                rowMapper.accept(row, batch);

                if (batch.size() == BATCH_SIZE) {
                    genisysBatchService.saveBatch(batch);
                    totalCount += batch.size();
                    log.info("Saved {} Main data records so far...", totalCount);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                genisysBatchService.saveBatch(batch);
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

    // -------------------------------------------------------------------------
    // Excel → Entity Mapping (UNCHANGED LOGIC)
    // -------------------------------------------------------------------------

    private void mapExcelRowsToMainDataReportEntity(Row row, List<MainDataReportEntity> list) {
        HashMap<Integer, String> dateMap = new HashMap<>();
        int col = 0;
        MainDataReportEntity entity = MainDataReportEntity.builder()
                // id - auto generated, skip
                .policyNo(getIntegerValue(row.getCell(col++)))
                .proposalNo(getIntegerValue(row.getCell(col++)))
                .productCode(getStringValue(row.getCell(col++)))
                .planNo(getStringValue(row.getCell(col++)))
                // Dates & Terms

                .inception(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .expiry(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .issueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .salesBranchCode(getIntegerValue(row.getCell(col++)))
                .salesBranchName(getStringValue(row.getCell(col++)))
                .companyBranchCode(getIntegerValue(row.getCell(col++)))
                .companyBranchName(getStringValue(row.getCell(col++)))
                .policyBranchCode(getIntegerValue(row.getCell(col++)))
                .policyBranchName(getStringValue(row.getCell(col++)))
                .term(getIntegerValue(row.getCell(col++)))
                .cy(getStringValue(row.getCell(col++)))
                .premiumPaymentTerm(getStringValue(row.getCell(col++)))
                .defermentTerm(getIntegerValue(row.getCell(col++)))
                .retirementBenefitPayoutTerm(getIntegerValue(row.getCell(col++)))
                .modalPremium(getBigDecimalValue(row.getCell(col++)))
                .frequency(getIntegerValue(row.getCell(col++)))
                .nextPremium(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .status(getStringValue(row.getCell(col++)))
                .statusDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .reason(getStringValue(row.getCell(col++)))
                .agentCode(getStringValue(row.getCell(col++)))
                .introducer(getStringValue(row.getCell(col++)))
                .supervisor(getStringValue(row.getCell(col++)))
                .riPercentage(getDoubleValue(row.getCell(col++)))
                // Main Life Details
                .pin(getIntegerValue(row.getCell(col++)))
                .title(getStringValue(row.getCell(col++)))
                .fullName(getStringValue(row.getCell(col++)))
                .gender(getStringValue(row.getCell(col++)))
                .dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .aae(getIntegerValue(row.getCell(col++)))
                .sarChoice(getStringValue(row.getCell(col++)))
                .numberOfRidersTaken(getIntegerValue(row.getCell(col++)))
                .dthSar(getBigDecimalValue(row.getCell(col++)))
                .subDth(getIntegerValue(row.getCell(col++)))
                .subRateMilDth(getBigDecimalValue(row.getCell(col++)))
                .dthOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .accdSa(getBigDecimalValue(row.getCell(col++)))
                .subAccd(getBigDecimalValue(row.getCell(col++)))
                .subRateMilAccd(getBigDecimalValue(row.getCell(col++)))
                .accdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .accpSa(getBigDecimalValue(row.getCell(col++)))
                .subAccp(getBigDecimalValue(row.getCell(col++)))
                .subRateMilAccp(getBigDecimalValue(row.getCell(col++)))
                .accpOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .acctSa(getBigDecimalValue(row.getCell(col++)))
                .subAcct(getBigDecimalValue(row.getCell(col++)))
                .subRateMilAcct(getBigDecimalValue(row.getCell(col++)))
                .acctOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                // CILL
                .cillSa(getBigDecimalValue(row.getCell(col++)))
                .subCill(getBigDecimalValue(row.getCell(col++)))
                .subRateMilCill(getBigDecimalValue(row.getCell(col++)))
                .cillOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // CILX
                .cilxSa(getBigDecimalValue(row.getCell(col++)))
                .subCilx(getBigDecimalValue(row.getCell(col++)))
                .subRateMilCilx(getBigDecimalValue(row.getCell(col++)))
                .cilxOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FIB
                .fibSa(getBigDecimalValue(row.getCell(col++)))
                .subFib(getBigDecimalValue(row.getCell(col++)))
                .subRateMilFib(getBigDecimalValue(row.getCell(col++)))
                .fibOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FIBT
                .fibtSa(getBigDecimalValue(row.getCell(col++)))
                .subFibt(getBigDecimalValue(row.getCell(col++)))
                .subRateMilFibt(getBigDecimalValue(row.getCell(col++)))
                .fibtOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FSEB
                .fsebSa(getBigDecimalValue(row.getCell(col++)))
                .subFseb(getBigDecimalValue(row.getCell(col++)))
                .subRateMilFseb(getBigDecimalValue(row.getCell(col++)))
                .fsebOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HB
                .hbSa(getBigDecimalValue(row.getCell(col++)))
                .subHb(getBigDecimalValue(row.getCell(col++)))
                .subRateMilHb(getBigDecimalValue(row.getCell(col++)))
                .hbOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // LEB
                .lebSa(getBigDecimalValue(row.getCell(col++)))
                .subLeb(getBigDecimalValue(row.getCell(col++)))
                .subRateMilLeb(getBigDecimalValue(row.getCell(col++)))
                .lebOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // PTD
                .ptdSa(getBigDecimalValue(row.getCell(col++)))
                .subPtd(getBigDecimalValue(row.getCell(col++)))
                .subRateMilPtd(getBigDecimalValue(row.getCell(col++)))
                .ptdOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // TILL
                .tillSa(getBigDecimalValue(row.getCell(col++)))
                .subTill(getBigDecimalValue(row.getCell(col++)))
                .subRateMilTill(getBigDecimalValue(row.getCell(col++)))
                .tillOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // TR
                .trSa(getBigDecimalValue(row.getCell(col++)))
                .subTr(getBigDecimalValue(row.getCell(col++)))
                .subRateMilTr(getBigDecimalValue(row.getCell(col++)))
                .trOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPA
                .wopaSa(getBigDecimalValue(row.getCell(col++)))
                .subWopa(getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopa(getBigDecimalValue(row.getCell(col++)))
                .wopaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPC
                .wopcSa(getBigDecimalValue(row.getCell(col++)))
                .subWopc(getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopc(getBigDecimalValue(row.getCell(col++)))
                .wopcOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPD
                .wopdSa(getBigDecimalValue(row.getCell(col++)))
                .subWopd(getBigDecimalValue(row.getCell(col++)))
                .subRateMilWopd(getBigDecimalValue(row.getCell(col++)))
                .wopdOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FSEBA
                .fsebaSa(getBigDecimalValue(row.getCell(col++)))
                .subFseba(getBigDecimalValue(row.getCell(col++)))
                .subRateMilFseba(getBigDecimalValue(row.getCell(col++)))
                .fsebaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HBA
                .hbaSa(getBigDecimalValue(row.getCell(col++)))
                .subHba(getBigDecimalValue(row.getCell(col++)))
                .subRateMilHba(getBigDecimalValue(row.getCell(col++)))
                .hbaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HBAC
                .hbacSa(getBigDecimalValue(row.getCell(col++)))
                .subHbac(getBigDecimalValue(row.getCell(col++)))
                .subRateMilHbac(getBigDecimalValue(row.getCell(col++)))
                .hbacOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // Spouse/Child
                .spouseChildPin(getIntegerValue(row.getCell(col++)))
                .spouseChildTitle(getStringValue(row.getCell(col++)))
                .spouseChildFullName(getStringValue(row.getCell(col++)))
                .spouseChildGender(getStringValue(row.getCell(col++)))
                .spouseChildDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .spouseChildAge(getIntegerValue(row.getCell(col++)))
                .spouseDeathSa(getBigDecimalValue(row.getCell(col++)))
                .spouseSubDeath(getBigDecimalValue(row.getCell(col++)))
                .spouseSubRateMilDeath(getBigDecimalValue(row.getCell(col++)))
                .spouseDeathOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAccdSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAccd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAccd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildAccdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAccpSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAccp(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAccp(getBigDecimalValue(row.getCell(col++)))
                .spouseChildAccpOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAcctSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubAcct(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilAcct(getBigDecimalValue(row.getCell(col++)))
                .spouseChildAcctOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildCillSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubCill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilCill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildCillOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildCilxSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubCilx(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilCilx(getBigDecimalValue(row.getCell(col++)))
                .spouseChildCilxOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildLebSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubLeb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilLeb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildLebOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildPtdSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubPtd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilPtd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildPtdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildTillSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubTill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilTill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildTillOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildHbSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubHb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilHb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildHbOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildPpdSa(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubPpd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildSubRateMilPpd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildPpdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseHbaSa(getBigDecimalValue(row.getCell(col++)))
                .spouseSubHba(getBigDecimalValue(row.getCell(col++)))
                .spouseSubRateMilHba(getBigDecimalValue(row.getCell(col++)))
                .spouseHbaOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                // Children 1..5
                .child1Name(getStringValue(row.getCell(col++)))
                .child1Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child1Age(getIntegerValue(row.getCell(col++)))
                .child1Hbc(getIntegerValue(row.getCell(col++)))
                .child1Hbcac(getIntegerValue(row.getCell(col++)))
                .child2Name(getStringValue(row.getCell(col++)))
                .child2Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child2Age(getIntegerValue(row.getCell(col++)))
                .child2Hbc(getStringValue(row.getCell(col++)))
                .child2Hbcac(getStringValue(row.getCell(col++)))
                .child3Name(getStringValue(row.getCell(col++)))
                .child3Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child3Age(getIntegerValue(row.getCell(col++)))
                .child3Hbc(getIntegerValue(row.getCell(col++)))
                .child3Hbcac(getIntegerValue(row.getCell(col++)))
                .child4Name(getStringValue(row.getCell(col++)))
                .child4Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child4Age(getIntegerValue(row.getCell(col++)))
                .child4Hbc(getIntegerValue(row.getCell(col++)))
                .child4Hbcac(getIntegerValue(row.getCell(col++)))
                .child5Name(getStringValue(row.getCell(col++)))
                .child5Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .child5Age(getIntegerValue(row.getCell(col++)))
                .child5Hbc(getIntegerValue(row.getCell(col++)))
                .child5Hbcac(getIntegerValue(row.getCell(col++)))
                // Financial Summary
                .basicSumAssured(getBigDecimalValue(row.getCell(col++)))
                .interestRate(getDoubleValue(row.getCell(col++)))
                .tpdPremiumLife1(getBigDecimalValue(row.getCell(col++)))
                .tpdPremiumLife2(getBigDecimalValue(row.getCell(col++)))
                .valueToday(getBigDecimalValue(row.getCell(col++)))
                .prmValueToday(getBigDecimalValue(row.getCell(col++)))
                .bstValueToday(getBigDecimalValue(row.getCell(col++)))
                .transactionAmount(getBigDecimalValue(row.getCell(col++)))
                .interestCredited(getBigDecimalValue(row.getCell(col++)))
                .surrenderValue(getBigDecimalValue(row.getCell(col++)))
                .prmSurrenderValue(getBigDecimalValue(row.getCell(col++)))
                .bstSurrenderValue(getBigDecimalValue(row.getCell(col++)))
                .insuranceCoveragePeriod(getIntegerValue(row.getCell(col++)))
                .operationDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .lastPaymentDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .lastPremiumDueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(col++))))
                .premiumEscalationBenefitPercentage(getDoubleValue(row.getCell(col++)))
                .refundValue(getBigDecimalValue(row.getCell(col++)))
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
        entity.setPolicyNo(getIntegerValue(row.getCell(c++)));
        entity.setProposalNo(getIntegerValue(row.getCell(c++)));
        entity.setProductCode(getStringValue(row.getCell(c++)));
        entity.setPlanNo(getStringValue(row.getCell(c++)));
        // Dates (4-6)
        entity.setInception(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setExpiry(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setIssueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        // Branches (7-12)
        entity.setSalesBranchCode(getIntegerValue(row.getCell(c++)));
        entity.setSalesBranchName(getStringValue(row.getCell(c++)));
        entity.setCompanyBranchCode(getIntegerValue(row.getCell(c++)));
        entity.setCompanyBranchName(getStringValue(row.getCell(c++)));
        entity.setPolicyBranchCode(getIntegerValue(row.getCell(c++)));
        entity.setPolicyBranchName(getStringValue(row.getCell(c++)));
        // Term & premium (13-18)
        entity.setTerm(getIntegerValue(row.getCell(c++)));
        entity.setCy(getStringValue(row.getCell(c++)));
        entity.setPremiumPaymentTerm(getIntegerValue(row.getCell(c++)));
        entity.setModalPremium(getBigDecimalValue(row.getCell(c++)));
        entity.setFrequency(getIntegerValue(row.getCell(c++)));
        entity.setNextPremium(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        // Status & dates (19-22)
        entity.setStatus(getStringValue(row.getCell(c++)));
        entity.setDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setOperationDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setReason(getStringValue(row.getCell(c++)));
        // Agent (23-26)
        entity.setAgentCode(getStringValue(row.getCell(c++)));
        entity.setIntroducer(getStringValue(row.getCell(c++)));
        entity.setSupervisor(getStringValue(row.getCell(c++)));
        entity.setRiPercentage(getDoubleValue(row.getCell(c++)));
        // Main life (27-35)
        entity.setPin(getIntegerValue(row.getCell(c++)));
        entity.setTitle(getStringValue(row.getCell(c++)));
        entity.setFullName(getStringValue(row.getCell(c++)));
        entity.setGender(getStringValue(row.getCell(c++)));
        entity.setDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setAae(getIntegerValue(row.getCell(c++)));
        entity.setOccupationMainLife(getStringValue(row.getCell(c++)));
        entity.setSarChoice(getStringValue(row.getCell(c++)));
        entity.setNumberOfRidersTaken(getIntegerValue(row.getCell(c++)));
        // DTH (36-39)
        entity.setDthSar(getBigDecimalValue(row.getCell(c++)));
        entity.setSubDth(getIntegerValue(row.getCell(c++)));
        entity.setSubRateMilDth(getBigDecimalValue(row.getCell(c++)));
        entity.setDthOccupationalLoadingPercent(getDoubleValue(row.getCell(c++)));
        // HB (40-43)
        entity.setHbSa(getBigDecimalValue(row.getCell(c++)));
        entity.setSubHb(getBigDecimalValue(row.getCell(c++)));
        entity.setSubRateMilHb(getBigDecimalValue(row.getCell(c++)));
        entity.setHbOccupationalLoadingPercentage(getDoubleValue(row.getCell(c++)));
        // INP (44-47)
        entity.setInpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setSubInp(getBigDecimalValue(row.getCell(c++)));
        entity.setSubRateMilInp(getBigDecimalValue(row.getCell(c++)));
        entity.setInpOccLoadingPercentage(getDoubleValue(row.getCell(c++)));
        // ML Bonus (48)
        entity.setMlBonus(getBigDecimalValue(row.getCell(c++)));
        // Spouse (49-68)
        entity.setSpousePin(getIntegerValue(row.getCell(c++)));
        entity.setSpouseTitle(getStringValue(row.getCell(c++)));
        entity.setSpouseFullName(getStringValue(row.getCell(c++)));
        entity.setSpouseGender(getStringValue(row.getCell(c++)));
        entity.setSpouseDob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setSpouseAge(getIntegerValue(row.getCell(c++)));
        entity.setOccupationSpouse(getStringValue(row.getCell(c++)));
        entity.setSpouseDeathSa(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubDeath(getIntegerValue(row.getCell(c++)));
        entity.setSpouseSubRateMilDeath(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseDeathOccLoadingPercentage(getDoubleValue(row.getCell(c++)));
        entity.setSpouseHbSa(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubHb(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubRateMilHb(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseHbOccLoadingPercentage(getDoubleValue(row.getCell(c++)));
        entity.setSpouseBonus(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseInpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubInp(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseSubRateMilInp(getBigDecimalValue(row.getCell(c++)));
        entity.setSpouseInpOccLoadingPercentage(getDoubleValue(row.getCell(c++)));
        // Children 1-5 (NAME, DOB, AGE, HBC, INP SAR, Bonus)
        entity.setChild1Name(getStringValue(row.getCell(c++)));
        entity.setChild1Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild1Age(getIntegerValue(row.getCell(c++)));
        entity.setChild1Hbc(getIntegerValue(row.getCell(c++)));
        entity.setChild1InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild1Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild2Name(getStringValue(row.getCell(c++)));
        entity.setChild2Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild2Age(getIntegerValue(row.getCell(c++)));
        entity.setChild2Hbc(getStringValue(row.getCell(c++)));
        entity.setChild2InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild2Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild3Name(getStringValue(row.getCell(c++)));
        entity.setChild3Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild3Age(getIntegerValue(row.getCell(c++)));
        entity.setChild3Hbc(getIntegerValue(row.getCell(c++)));
        entity.setChild3InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild3Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild4Name(getStringValue(row.getCell(c++)));
        entity.setChild4Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild4Age(getIntegerValue(row.getCell(c++)));
        entity.setChild4Hbc(getIntegerValue(row.getCell(c++)));
        entity.setChild4InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild4Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild5Name(getStringValue(row.getCell(c++)));
        entity.setChild5Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild5Age(getIntegerValue(row.getCell(c++)));
        entity.setChild5Hbc(getIntegerValue(row.getCell(c++)));
        entity.setChild5InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild5Bonus(getBigDecimalValue(row.getCell(c++)));
        // Children 6-20 (each: NAME, DOB, AGE, HBC, INP SAR, Bonus)
        entity.setChild6Name(getStringValue(row.getCell(c++)));
        entity.setChild6Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild6Age(getIntegerValue(row.getCell(c++)));
        entity.setChild6Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild6InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild6Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild7Name(getStringValue(row.getCell(c++)));
        entity.setChild7Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild7Age(getIntegerValue(row.getCell(c++)));
        entity.setChild7Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild7InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild7Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild8Name(getStringValue(row.getCell(c++)));
        entity.setChild8Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild8Age(getIntegerValue(row.getCell(c++)));
        entity.setChild8Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild8InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild8Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild9Name(getStringValue(row.getCell(c++)));
        entity.setChild9Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild9Age(getIntegerValue(row.getCell(c++)));
        entity.setChild9Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild9InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild9Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild10Name(getStringValue(row.getCell(c++)));
        entity.setChild10Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild10Age(getIntegerValue(row.getCell(c++)));
        entity.setChild10Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild10InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild10Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild11Name(getStringValue(row.getCell(c++)));
        entity.setChild11Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild11Age(getIntegerValue(row.getCell(c++)));
        entity.setChild11Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild11InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild11Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild12Name(getStringValue(row.getCell(c++)));
        entity.setChild12Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild12Age(getIntegerValue(row.getCell(c++)));
        entity.setChild12Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild12InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild12Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild13Name(getStringValue(row.getCell(c++)));
        entity.setChild13Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild13Age(getIntegerValue(row.getCell(c++)));
        entity.setChild13Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild13InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild13Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild14Name(getStringValue(row.getCell(c++)));
        entity.setChild14Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild14Age(getIntegerValue(row.getCell(c++)));
        entity.setChild14Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild14InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild14Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild15Name(getStringValue(row.getCell(c++)));
        entity.setChild15Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild15Age(getIntegerValue(row.getCell(c++)));
        entity.setChild15Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild15InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild15Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild16Name(getStringValue(row.getCell(c++)));
        entity.setChild16Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild16Age(getIntegerValue(row.getCell(c++)));
        entity.setChild16Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild16InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild16Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild17Name(getStringValue(row.getCell(c++)));
        entity.setChild17Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild17Age(getIntegerValue(row.getCell(c++)));
        entity.setChild17Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild17InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild17Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild18Name(getStringValue(row.getCell(c++)));
        entity.setChild18Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild18Age(getIntegerValue(row.getCell(c++)));
        entity.setChild18Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild18InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild18Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild19Name(getStringValue(row.getCell(c++)));
        entity.setChild19Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild19Age(getIntegerValue(row.getCell(c++)));
        entity.setChild19Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild19InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild19Bonus(getBigDecimalValue(row.getCell(c++)));
        entity.setChild20Name(getStringValue(row.getCell(c++)));
        entity.setChild20Dob(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setChild20Age(getIntegerValue(row.getCell(c++)));
        entity.setChild20Hbc(getBigDecimalValue(row.getCell(c++)));
        entity.setChild20InpSar(getBigDecimalValue(row.getCell(c++)));
        entity.setChild20Bonus(getBigDecimalValue(row.getCell(c++)));
        // Basic Sum Assured, Insurance Coverage Period, Last Payment Date, Last Premium Due Date, Refund Value
        entity.setBasicSumAssured(getBigDecimalValue(row.getCell(c++)));
        entity.setInsuranceCoveragePeriod(getIntegerValue(row.getCell(c++)));
        entity.setLastPaymentDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setLastPremiumDueDate(commonFunction.getDateFromInteger(getStringDateValue(row.getCell(c++))));
        entity.setRefundValue(getBigDecimalValue(row.getCell(c++)));

        list.add(entity);
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

    // -------------------------------------------------------------------------
    // Cell Helpers
    // -------------------------------------------------------------------------

    private Integer getIntegerValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (int) cell.getNumericCellValue();
                case STRING -> Integer.parseInt(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing integer value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    private Long getLongValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (long) cell.getNumericCellValue();
                case STRING -> Long.parseLong(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing long value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    private Double getDoubleValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> Double.parseDouble(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing double value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue()).trim();
                default -> null;
            };
        } catch (Exception e) {
            log.error("Error parsing string value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    private java.math.BigDecimal getBigDecimalValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> java.math.BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> new java.math.BigDecimal(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing BigDecimal value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    private String getStringDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getStringCellValue().trim();
            }
            return null;
        } catch (Exception e) {
            log.error("Error parsing date value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }
}

