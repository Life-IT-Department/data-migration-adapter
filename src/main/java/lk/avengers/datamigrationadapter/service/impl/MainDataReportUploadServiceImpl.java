package lk.avengers.datamigrationadapter.service.impl;


import com.monitorjbl.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
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
    private static final int HEADER_ROW_2 = 1;

    private final BatchProcessService genisysBatchService;
    private final ResourceLoader resourceLoader;
    private final MainDataReportRepository mainDataReportRepository;

    @Value("${mainDataReport.file}")
    private String mainDataReportFilePath;

    @Value("${mainDataReport2.file}")
    private String mainDataReport2FilePath;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public ResponseEntity<CommonResponseDTO> uploadMainDataReports() {
        log.info("uploadMainDataReports called");
        return upload(
                this::getMainDataReportFileInputStream,
                this::mapExcelRowsToMainDataReportEntity,
                true
        );
    }

    @Override
    public ResponseEntity<CommonResponseDTO> uploadMainDataReport2() {
        log.info("uploadMainDataReport2 called");
        return upload(
                this::getMainDataReport2FileInputStream,
                this::mapExcelRowsToMainDataReport2Entity,
                false
        );
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
                    log.info("Saved {} records so far...", totalCount);
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
                        .message("Main data report ecords uploaded successfully.")
                        .status(HttpStatus.OK.toString())
                        .build()
        );
    }

    // -------------------------------------------------------------------------
    // Row filtering
    // -------------------------------------------------------------------------

    private boolean shouldSkipRow(Row row) {
        if (row.getRowNum() == HEADER_ROW_1 || row.getRowNum() == HEADER_ROW_2) {
            return true;
        }
        return isCellBlank(row.getCell(1)) || isCellBlank(row.getCell(3));
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
        int col = 0;
        MainDataReportEntity entity = MainDataReportEntity.builder()
                // id - auto generated, skip
                .policyNo(getIntegerValue(row.getCell(col++)))
                .proposalNo(getStringValue(row.getCell(col++)))
                .productCode(getStringValue(row.getCell(col++)))
                .planNo(getStringValue(row.getCell(col++)))
                // Dates & Terms
                .inception(getLocalDateValue(row.getCell(col++)))
                .expiry(getLocalDateValue(row.getCell(col++)))
                .issueDate(getLocalDateValue(row.getCell(col++)))
                .salesBranchCode(getStringValue(row.getCell(col++)))
                .salesBranchName(getStringValue(row.getCell(col++)))
                .companyBranchCode(getStringValue(row.getCell(col++)))
                .companyBranchName(getStringValue(row.getCell(col++)))
                .policyBranchCode(getStringValue(row.getCell(col++)))
                .policyBranchName(getStringValue(row.getCell(col++)))
                .term(getIntegerValue(row.getCell(col++)))
                .cy(getStringValue(row.getCell(col++)))
                .premiumPaymentTerm(getStringValue(row.getCell(col++)))
                .defermentTerm(getIntegerValue(row.getCell(col++)))
                .retirementBenefitPayoutTerm(getIntegerValue(row.getCell(col++)))
                .modalPremium(getBigDecimalValue(row.getCell(col++)))
                .frequency(getIntegerValue(row.getCell(col++)))
                .nextPremium(getLocalDateValue(row.getCell(col++)))
                .status(getStringValue(row.getCell(col++)))
                .statusDate(getLocalDateValue(row.getCell(col++)))
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
                .dob(getIntegerValue(row.getCell(col++)))
                .aae(getIntegerValue(row.getCell(col++)))
                .sarChoice(getStringValue(row.getCell(col++)))
                .numberOfRidersTaken(getIntegerValue(row.getCell(col++)))
                .dthSar(getBigDecimalValue(row.getCell(col++)))
                .subDth(getIntegerValue(row.getCell(col++)))
                .subRateMilDth(getBigDecimalValue(row.getCell(col++)))
                .dthOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .accdSa(getIntegerValue(row.getCell(col++)))
                .subAccd(getIntegerValue(row.getCell(col++)))
                .subRateMilAccd(getBigDecimalValue(row.getCell(col++)))
                .accdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .accpSa(getIntegerValue(row.getCell(col++)))
                .subAccp(getIntegerValue(row.getCell(col++)))
                .subRateMilAccp(getBigDecimalValue(row.getCell(col++)))
                .accpOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .acctSa(getIntegerValue(row.getCell(col++)))
                .subAcct(getIntegerValue(row.getCell(col++)))
                .subRateMilAcct(getBigDecimalValue(row.getCell(col++)))
                .acctOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                // CILL
                .cillSa(getIntegerValue(row.getCell(col++)))
                .subCill(getIntegerValue(row.getCell(col++)))
                .subRateMilCill(getBigDecimalValue(row.getCell(col++)))
                .cillOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // CILX
                .cilxSa(getIntegerValue(row.getCell(col++)))
                .subCilx(getIntegerValue(row.getCell(col++)))
                .subRateMilCilx(getBigDecimalValue(row.getCell(col++)))
                .cilxOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FIB
                .fibSa(getIntegerValue(row.getCell(col++)))
                .subFib(getIntegerValue(row.getCell(col++)))
                .subRateMilFib(getBigDecimalValue(row.getCell(col++)))
                .fibOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FIBT
                .fibtSa(getIntegerValue(row.getCell(col++)))
                .subFibt(getIntegerValue(row.getCell(col++)))
                .subRateMilFibt(getBigDecimalValue(row.getCell(col++)))
                .fibtOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FSEB
                .fsebSa(getIntegerValue(row.getCell(col++)))
                .subFseb(getIntegerValue(row.getCell(col++)))
                .subRateMilFseb(getBigDecimalValue(row.getCell(col++)))
                .fsebOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HB
                .hbSa(getBigDecimalValue(row.getCell(col++)))
                .subHb(getBigDecimalValue(row.getCell(col++)))
                .subRateMilHb(getBigDecimalValue(row.getCell(col++)))
                .hbOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // LEB
                .lebSa(getIntegerValue(row.getCell(col++)))
                .subLeb(getIntegerValue(row.getCell(col++)))
                .subRateMilLeb(getBigDecimalValue(row.getCell(col++)))
                .lebOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // PTD
                .ptdSa(getLongValue(row.getCell(col++)))
                .subPtd(getIntegerValue(row.getCell(col++)))
                .subRateMilPtd(getBigDecimalValue(row.getCell(col++)))
                .ptdOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // TILL
                .tillSa(getIntegerValue(row.getCell(col++)))
                .subTill(getIntegerValue(row.getCell(col++)))
                .subRateMilTill(getBigDecimalValue(row.getCell(col++)))
                .tillOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // TR
                .trSa(getLongValue(row.getCell(col++)))
                .subTr(getIntegerValue(row.getCell(col++)))
                .subRateMilTr(getBigDecimalValue(row.getCell(col++)))
                .trOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPA
                .wopaSa(getLongValue(row.getCell(col++)))
                .subWopa(getIntegerValue(row.getCell(col++)))
                .subRateMilWopa(getBigDecimalValue(row.getCell(col++)))
                .wopaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPC
                .wopcSa(getLongValue(row.getCell(col++)))
                .subWopc(getIntegerValue(row.getCell(col++)))
                .subRateMilWopc(getBigDecimalValue(row.getCell(col++)))
                .wopcOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // WOPD
                .wopdSa(getLongValue(row.getCell(col++)))
                .subWopd(getIntegerValue(row.getCell(col++)))
                .subRateMilWopd(getBigDecimalValue(row.getCell(col++)))
                .wopdOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // FSEBA
                .fsebaSa(getLongValue(row.getCell(col++)))
                .subFseba(getIntegerValue(row.getCell(col++)))
                .subRateMilFseba(getBigDecimalValue(row.getCell(col++)))
                .fsebaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HBA
                .hbaSa(getIntegerValue(row.getCell(col++)))
                .subHba(getIntegerValue(row.getCell(col++)))
                .subRateMilHba(getBigDecimalValue(row.getCell(col++)))
                .hbaOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // HBAC
                .hbacSa(getIntegerValue(row.getCell(col++)))
                .subHbac(getIntegerValue(row.getCell(col++)))
                .subRateMilHbac(getBigDecimalValue(row.getCell(col++)))
                .hbacOccupationalLoadingPercentage(getDoubleValue(row.getCell(col++)))
                // Spouse/Child
                .spouseChildPin(getIntegerValue(row.getCell(col++)))
                .spouseChildTitle(getStringValue(row.getCell(col++)))
                .spouseChildFullName(getStringValue(row.getCell(col++)))
                .spouseChildGender(getStringValue(row.getCell(col++)))
                .spouseChildDob(getLocalDateValue(row.getCell(col++)))
                .spouseChildAge(getIntegerValue(row.getCell(col++)))
                .spouseDeathSa(getIntegerValue(row.getCell(col++)))
                .spouseSubDeath(getIntegerValue(row.getCell(col++)))
                .spouseSubRateMilDeath(getBigDecimalValue(row.getCell(col++)))
                .spouseDeathOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAccdSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubAccd(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilAccd(getIntegerValue(row.getCell(col++)))
                .spouseChildAccdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAccpSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubAccp(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilAccp(getIntegerValue(row.getCell(col++)))
                .spouseChildAccpOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildAcctSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubAcct(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilAcct(getBigDecimalValue(row.getCell(col++)))
                .spouseChildAcctOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildCillSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubCill(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilCill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildCillOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildCilxSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubCilx(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilCilx(getIntegerValue(row.getCell(col++)))
                .spouseChildCilxOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildLebSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubLeb(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilLeb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildLebOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildPtdSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubPtd(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilPtd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildPtdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildTillSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubTill(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilTill(getBigDecimalValue(row.getCell(col++)))
                .spouseChildTillOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildHbSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubHb(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilHb(getBigDecimalValue(row.getCell(col++)))
                .spouseChildHbOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseChildPpdSa(getIntegerValue(row.getCell(col++)))
                .spouseChildSubPpd(getIntegerValue(row.getCell(col++)))
                .spouseChildSubRateMilPpd(getBigDecimalValue(row.getCell(col++)))
                .spouseChildPpdOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                .spouseHbaSa(getIntegerValue(row.getCell(col++)))
                .spouseSubHba(getIntegerValue(row.getCell(col++)))
                .spouseSubRateMilHba(getBigDecimalValue(row.getCell(col++)))
                .spouseHbaOccupationalLoadingPercent(getDoubleValue(row.getCell(col++)))
                // Children 1..5
                .child1Name(getStringValue(row.getCell(col++)))
                .child1Dob(getLocalDateValue(row.getCell(col++)))
                .child1Age(getIntegerValue(row.getCell(col++)))
                .child1Hbc(getIntegerValue(row.getCell(col++)))
                .child1Hbcac(getIntegerValue(row.getCell(col++)))
                .child2Name(getStringValue(row.getCell(col++)))
                .child2Dob(getLocalDateValue(row.getCell(col++)))
                .child2Age(getIntegerValue(row.getCell(col++)))
                .child2Hbc(getStringValue(row.getCell(col++)))
                .child2Hbcac(getStringValue(row.getCell(col++)))
                .child3Name(getStringValue(row.getCell(col++)))
                .child3Dob(getLocalDateValue(row.getCell(col++)))
                .child3Age(getIntegerValue(row.getCell(col++)))
                .child3Hbc(getIntegerValue(row.getCell(col++)))
                .child3Hbcac(getIntegerValue(row.getCell(col++)))
                .child4Name(getStringValue(row.getCell(col++)))
                .child4Dob(getLocalDateValue(row.getCell(col++)))
                .child4Age(getIntegerValue(row.getCell(col++)))
                .child4Hbc(getIntegerValue(row.getCell(col++)))
                .child4Hbcac(getIntegerValue(row.getCell(col++)))
                .child5Name(getStringValue(row.getCell(col++)))
                .child5Dob(getLocalDateValue(row.getCell(col++)))
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
                .operationDate(getLocalDateValue(row.getCell(col++)))
                .lastPaymentDate(getLocalDateValue(row.getCell(col++)))
                .lastPremiumDueDate(getLocalDateValue(row.getCell(col++)))
                .premiumEscalationBenefitPercentage(getDoubleValue(row.getCell(col++)))
                .refundValue(getBigDecimalValue(row.getCell(col++)))
                // createdAt - handled by @CreatedDate
                .build();

        list.add(entity);
    }

    private void mapExcelRowsToMainDataReport2Entity(Row row, List<MainDataReportEntity> list) {
        MainDataReportEntity entity = new MainDataReportEntity();
        // --- Main Policy Holder (Indices based on your provided header list) ---
        entity.setPolicyNo(getIntegerValue(row.getCell(1))); // POLICY NO
        entity.setProductCode(getStringValue(row.getCell(3))); // Product Code
        entity.setIntroducer(getStringValue(row.getCell(25)));
        entity.setFullName(getStringValue(row.getCell(30))); // FULL NAME
        entity.setGender(getStringValue(row.getCell(31)));
        entity.setDob(getIntegerValue(row.getCell(32)));
        entity.setAae(getIntegerValue(row.getCell(33))); // AAE
        entity.setNumberOfRidersTaken(getIntegerValue(row.getCell(36))); // Number of Riders Taken
//        entity.setDthSar(getStringValue(row.getCell(37))); // DTH SAR
//        entity.setSubDth(getStringValue(row.getCell(38))); // SUB-DTH
//        entity.setSubRateMilDth(getStringValue(row.getCell(39))); // SUB Rate/Mil-DTH
//        entity.setDthOccupationalLoadingPercent(getDoubleValue(row.getCell(40)));// DTH Occupational Loading %
//        // --- Spouse/Child ---
//        entity.setSpouseChildPin(getIntegerValue(row.getCell(50))); // Spouse/Child PIN
//        entity.setSpouseChildTitle(getStringValue(row.getCell(51))); // Spouse/Child TITLE
//        entity.setSpouseChildFullName(getStringValue(row.getCell(52))); // Spouse/Child FULL NAME
//        entity.setSpouseChildGender(getStringValue(row.getCell(53))); // Spouse/Child GENDER
//        entity.setSpouseChildDob(getIntegerValue(row.getCell(54))); // Spouse/Child DOB
//        entity.setSpouseChildAge(getIntegerValue(row.getCell(55))); // Spouse/Child AGE
//        entity.setSpouseDeathSa(getStringValue(row.getCell(57))); // Spouse DEATH SA
//        entity.setSpouseSubDeath(getStringValue(row.getCell(58))); // Spouse SUB-DEATH
//        entity.setSpouseSubRateMilDeath(getStringValue(row.getCell(59))); // Spouse SUB Rate/Mil-DEATH
//        entity.setSpouseDeathOccupationalLoadingPercent(getDoubleValue(row.getCell(60))); // Spouse DEATH Occupational Loading %
//        entity.setSpouseChildHbSa(getStringValue(row.getCell(61)));
//        entity.setSpouseChildSubHb(getStringValue(row.getCell(62)));
//        entity.setSpouseChildSubRateMilHb(getStringValue(row.getCell(63)));
//        entity.setSpouseChildHbOccupationalLoadingPercent(getDoubleValue(row.getCell(64)));
//        // --- Children 1..5 ---
//        entity.setChild1Name(getStringValue(row.getCell(70))); // CHILD1 NAME
//        entity.setChild1Dob(getIntegerValue(row.getCell(71))); // CHILD1 DOB
//        entity.setChild1Age(getIntegerValue(row.getCell(72))); // CHILD1 AGE
//        entity.setChild1Hbc(getStringValue(row.getCell(73))); // CHILD1 HBC
//
//        entity.setChild2Name(getStringValue(row.getCell(76))); // CHILD2 NAME
//        entity.setChild2Dob(getIntegerValue(row.getCell(77))); // CHILD2 DOB
//        entity.setChild2Age(getIntegerValue(row.getCell(78))); // CHILD2 AGE
//        entity.setChild2Hbc(getStringValue(row.getCell(79))); // CHILD2 HBC
//
//        entity.setChild3Name(getStringValue(row.getCell(82)));
//        entity.setChild3Dob(getIntegerValue(row.getCell(83)));
//        entity.setChild3Age(getIntegerValue(row.getCell(84)));
//        entity.setChild3Hbc(getStringValue(row.getCell(85)));
//
//        entity.setChild4Name(getStringValue(row.getCell(88)));
//        entity.setChild4Dob(getIntegerValue(row.getCell(89)));
//        entity.setChild4Age(getIntegerValue(row.getCell(90)));
//        entity.setChild4Hbc(getStringValue(row.getCell(91)));
//
//        entity.setChild5Name(getStringValue(row.getCell(94)));
//        entity.setChild5Dob(getIntegerValue(row.getCell(95)));
//        entity.setChild5Age(getIntegerValue(row.getCell(96)));
//        entity.setChild5Hbc(getStringValue(row.getCell(97)));

        list.add(entity);
    }

    // -------------------------------------------------------------------------
    // File Loading
    // -------------------------------------------------------------------------

    private InputStream getMainDataReportFileInputStream() {
        return getFileInputStream(mainDataReportFilePath, "MainDataReport");
    }

    private InputStream getMainDataReport2FileInputStream() {
        return getFileInputStream(mainDataReport2FilePath, "MainDataReport2");
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
            return null;
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue()).trim();
            default -> null;
        };
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
            return null;
        }
    }

    private java.time.LocalDate getLocalDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

