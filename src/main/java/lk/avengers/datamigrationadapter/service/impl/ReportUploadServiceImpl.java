package lk.avengers.datamigrationadapter.service.impl;

import com.monitorjbl.xlsx.StreamingReader;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lk.avengers.datamigrationadapter.service.ReportUploadService;
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
public class ReportUploadServiceImpl implements ReportUploadService {

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
                        .message("Records uploaded successfully.")
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
        MainDataReportEntity entity = new MainDataReportEntity();
        // --- Main Policy Holder (Indices based on your provided header list) ---
        entity.setPolicyNo(getIntegerValue(row.getCell(1))); // POLICY NO
        entity.setProductCode(getStringValue(row.getCell(3))); // Product Code
        entity.setIntroducer(getStringValue(row.getCell(26)));
        entity.setFullName(getStringValue(row.getCell(31))); // FULL NAME
        entity.setDob(getIntegerValue(row.getCell(33)));
        entity.setGender(getStringValue(row.getCell(32)));
        entity.setAae(getIntegerValue(row.getCell(34))); // AAE
        entity.setNumberOfRidersTaken(getIntegerValue(row.getCell(36))); // Number of Riders Taken
        entity.setDthSar(getStringValue(row.getCell(37))); // DTH SAR
        entity.setSubDth(getStringValue(row.getCell(38))); // SUB-DTH
        entity.setSubRateMilDth(getStringValue(row.getCell(39))); // SUB Rate/Mil-DTH
        entity.setDthOccupationalLoadingPercent(getDoubleValue(row.getCell(40)));// DTH Occupational Loading %
        entity.setAccdSa(getStringValue(row.getCell(41))); // ACCD SA
        entity.setSubAccd(getStringValue(row.getCell(42))); // SUB-ACCD
        entity.setSubRateMilAccd(getStringValue(row.getCell(43))); // SUB Rate/Mil-ACCD
        entity.setAccdOccupationalLoadingPercent(getDoubleValue(row.getCell(44))); // ACCD Occupational Loading %
        entity.setAccpSa(getStringValue(row.getCell(45))); // ACCP SA
        entity.setSubAccp(getStringValue(row.getCell(46))); // SUB-ACCP
        entity.setSubRateMilAccp(getStringValue(row.getCell(47))); // SUB Rate/Mil-ACCP
        entity.setAccpOccupationalLoadingPercent(getDoubleValue(row.getCell(48))); // ACCP Occupational Loading %
        entity.setAcctSa(getStringValue(row.getCell(49))); // ACCT SA
        entity.setSubAcct(getStringValue(row.getCell(50))); // SUB-ACCT
        entity.setSubRateMilAcct(getStringValue(row.getCell(51))); // SUB Rate/Mil-ACCT
        entity.setAcctOccupationalLoadingPercent(getDoubleValue(row.getCell(52))); // ACCT Occupational Loading %
        // --- Spouse/Child ---
        entity.setSpouseChildPin(getIntegerValue(row.getCell(117))); // Spouse/Child PIN
        entity.setSpouseChildTitle(getStringValue(row.getCell(118))); // Spouse/Child TITLE
        entity.setSpouseChildFullName(getStringValue(row.getCell(119))); // Spouse/Child FULL NAME
        entity.setSpouseChildGender(getStringValue(row.getCell(120))); // Spouse/Child GENDER
        entity.setSpouseChildDob(getIntegerValue(row.getCell(121))); // Spouse/Child DOB
        entity.setSpouseChildAge(getIntegerValue(row.getCell(122))); // Spouse/Child AGE
        entity.setSpouseDeathSa(getStringValue(row.getCell(123))); // Spouse DEATH SA
        entity.setSpouseSubDeath(getStringValue(row.getCell(124))); // Spouse SUB-DEATH
        entity.setSpouseSubRateMilDeath(getStringValue(row.getCell(125))); // Spouse SUB Rate/Mil-DEATH
        entity.setSpouseDeathOccupationalLoadingPercent(getDoubleValue(row.getCell(126))); // Spouse DEATH Occupational Loading %
        entity.setSpouseChildAccdSa(getStringValue(row.getCell(127))); // Spouse/Child ACCD SA
        entity.setSpouseChildSubAccd(getStringValue(row.getCell(128))); // Spouse/Child SUB-ACCD
        entity.setSpouseChildSubRateMilAccd(getStringValue(row.getCell(129))); // Spouse/Child SUB Rate/Mil-ACCD
        entity.setSpouseChildAccdOccupationalLoadingPercent(getDoubleValue(row.getCell(130)));
        entity.setSpouseChildAccpSa(getStringValue(row.getCell(131))); // Spouse/Child ACCP SA
        entity.setSpouseChildSubAccp(getStringValue(row.getCell(132))); // Spouse/Child SUB-ACCP
        entity.setSpouseChildSubRateMilAccp(getStringValue(row.getCell(133))); // Spouse/Child SUB Rate/Mil-ACCP
        entity.setSpouseChildAccpOccupationalLoadingPercent(getDoubleValue(row.getCell(134))); // Spouse/Child ACCP Occupational Loading %
        entity.setSpouseChildAcctSa(getStringValue(row.getCell(135))); // Spouse/Child ACCT SA
        entity.setSpouseChildSubAcct(getStringValue(row.getCell(136))); // Spouse/Child SUB-ACCT
        entity.setSpouseChildSubRateMilAcct(getStringValue(row.getCell(137))); // Spouse/Child SUB Rate/Mil-ACCT
        entity.setSpouseChildAcctOccupationalLoadingPercent(getDoubleValue(row.getCell(138))); // Spouse/Child ACCT Occupational Loading %
        entity.setSpouseChildCillSa(getStringValue(row.getCell(139))); // Spouse/Child CILL SA
        entity.setSpouseChildSubCill(getStringValue(row.getCell(140))); // Spouse/Child SUB-CILL
        entity.setSpouseChildSubRateMilCill(getStringValue(row.getCell(141))); // Spouse/Child SUB Rate/Mil-CILL
        entity.setSpouseChildCillOccupationalLoadingPercent(getDoubleValue(row.getCell(142))); // Spouse/Child CILL Occupational Loading %
        entity.setSpouseChildCilxSa(getStringValue(row.getCell(143))); // Spouse/Child CILX SA
        entity.setSpouseChildSubCilx(getStringValue(row.getCell(144))); // Spouse/Child SUB-CILX
        entity.setSpouseChildSubRateMilCilx(getStringValue(row.getCell(145))); // Spouse/Child SUB Rate/Mil-CILX
        entity.setSpouseChildCilxOccupationalLoadingPercent(getDoubleValue(row.getCell(146))); // Spouse/Child CILX Occupational Loading %
        entity.setSpouseChildLebSa(getStringValue(row.getCell(147)));
        entity.setSpouseChildSubLeb(getStringValue(row.getCell(148)));
        entity.setSpouseChildSubRateMilLeb(getStringValue(row.getCell(149)));
        entity.setSpouseChildLebOccupationalLoadingPercent(getDoubleValue(row.getCell(150)));
        entity.setSpouseChildPtdSa(getStringValue(row.getCell(151)));
        entity.setSpouseChildSubPtd(getStringValue(row.getCell(152)));
        entity.setSpouseChildSubRateMilPtd(getStringValue(row.getCell(153)));
        entity.setSpouseChildPtdOccupationalLoadingPercent(getDoubleValue(row.getCell(154)));
        entity.setSpouseChildTillSa(getStringValue(row.getCell(155)));
        entity.setSpouseChildSubTill(getStringValue(row.getCell(156)));
        entity.setSpouseChildSubRateMilTill(getStringValue(row.getCell(157)));
        entity.setSpouseChildTillOccupationalLoadingPercent(getDoubleValue(row.getCell(158)));
        entity.setSpouseChildHbSa(getStringValue(row.getCell(159)));
        entity.setSpouseChildSubHb(getStringValue(row.getCell(160)));
        entity.setSpouseChildSubRateMilHb(getStringValue(row.getCell(161)));
        entity.setSpouseChildHbOccupationalLoadingPercent(getDoubleValue(row.getCell(162)));
        entity.setSpouseChildPpdSa(getStringValue(row.getCell(163)));
        entity.setSpouseChildSubPpd(getStringValue(row.getCell(164)));
        entity.setSpouseChildSubRateMilPpd(getStringValue(row.getCell(165)));
        entity.setSpouseChildPpdOccupationalLoadingPercent(getDoubleValue(row.getCell(166)));
        entity.setSpouseHbaSa(getStringValue(row.getCell(167)));
        entity.setSpouseSubHba(getStringValue(row.getCell(168)));
        entity.setSpouseSubRateMilHba(getStringValue(row.getCell(169)));
        entity.setSpouseHbaOccupationalLoadingPercent(getDoubleValue(row.getCell(170)));
        // --- Children 1..5 ---
        entity.setChild1Name(getStringValue(row.getCell(171))); // CHILD1 NAME
        entity.setChild1Dob(getIntegerValue(row.getCell(172))); // CHILD1 DOB
        entity.setChild1Age(getIntegerValue(row.getCell(173))); // CHILD1 AGE
        entity.setChild1Hbc(getStringValue(row.getCell(174))); // CHILD1 HBC
        entity.setChild1Hbcac(getStringValue(row.getCell(175))); // CHILD1 HBCAC

        entity.setChild2Name(getStringValue(row.getCell(176))); // CHILD2 NAME
        entity.setChild2Dob(getIntegerValue(row.getCell(177))); // CHILD2 DOB
        entity.setChild2Age(getIntegerValue(row.getCell(178))); // CHILD2 AGE
        entity.setChild2Hbc(getStringValue(row.getCell(179))); // CHILD2 HBC
        entity.setChild2Hbcac(getStringValue(row.getCell(180))); // CHILD2 HBCAC

        entity.setChild3Name(getStringValue(row.getCell(181)));
        entity.setChild3Dob(getIntegerValue(row.getCell(182)));
        entity.setChild3Age(getIntegerValue(row.getCell(183)));
        entity.setChild3Hbc(getStringValue(row.getCell(184)));
        entity.setChild3Hbcac(getStringValue(row.getCell(185)));

        entity.setChild4Name(getStringValue(row.getCell(186)));
        entity.setChild4Dob(getIntegerValue(row.getCell(187)));
        entity.setChild4Age(getIntegerValue(row.getCell(188)));
        entity.setChild4Hbc(getStringValue(row.getCell(189)));
        entity.setChild4Hbcac(getStringValue(row.getCell(190)));

        entity.setChild5Name(getStringValue(row.getCell(191)));
        entity.setChild5Dob(getIntegerValue(row.getCell(192)));
        entity.setChild5Age(getIntegerValue(row.getCell(193)));
        entity.setChild5Hbc(getStringValue(row.getCell(194)));
        entity.setChild5Hbcac(getStringValue(row.getCell(195)));

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
        entity.setDthSar(getStringValue(row.getCell(37))); // DTH SAR
        entity.setSubDth(getStringValue(row.getCell(38))); // SUB-DTH
        entity.setSubRateMilDth(getStringValue(row.getCell(39))); // SUB Rate/Mil-DTH
        entity.setDthOccupationalLoadingPercent(getDoubleValue(row.getCell(40)));// DTH Occupational Loading %
        // --- Spouse/Child ---
        entity.setSpouseChildPin(getIntegerValue(row.getCell(50))); // Spouse/Child PIN
        entity.setSpouseChildTitle(getStringValue(row.getCell(51))); // Spouse/Child TITLE
        entity.setSpouseChildFullName(getStringValue(row.getCell(52))); // Spouse/Child FULL NAME
        entity.setSpouseChildGender(getStringValue(row.getCell(53))); // Spouse/Child GENDER
        entity.setSpouseChildDob(getIntegerValue(row.getCell(54))); // Spouse/Child DOB
        entity.setSpouseChildAge(getIntegerValue(row.getCell(55))); // Spouse/Child AGE
        entity.setSpouseDeathSa(getStringValue(row.getCell(57))); // Spouse DEATH SA
        entity.setSpouseSubDeath(getStringValue(row.getCell(58))); // Spouse SUB-DEATH
        entity.setSpouseSubRateMilDeath(getStringValue(row.getCell(59))); // Spouse SUB Rate/Mil-DEATH
        entity.setSpouseDeathOccupationalLoadingPercent(getDoubleValue(row.getCell(60))); // Spouse DEATH Occupational Loading %
        entity.setSpouseChildHbSa(getStringValue(row.getCell(61)));
        entity.setSpouseChildSubHb(getStringValue(row.getCell(62)));
        entity.setSpouseChildSubRateMilHb(getStringValue(row.getCell(63)));
        entity.setSpouseChildHbOccupationalLoadingPercent(getDoubleValue(row.getCell(64)));
        // --- Children 1..5 ---
        entity.setChild1Name(getStringValue(row.getCell(70))); // CHILD1 NAME
        entity.setChild1Dob(getIntegerValue(row.getCell(71))); // CHILD1 DOB
        entity.setChild1Age(getIntegerValue(row.getCell(72))); // CHILD1 AGE
        entity.setChild1Hbc(getStringValue(row.getCell(73))); // CHILD1 HBC

        entity.setChild2Name(getStringValue(row.getCell(76))); // CHILD2 NAME
        entity.setChild2Dob(getIntegerValue(row.getCell(77))); // CHILD2 DOB
        entity.setChild2Age(getIntegerValue(row.getCell(78))); // CHILD2 AGE
        entity.setChild2Hbc(getStringValue(row.getCell(79))); // CHILD2 HBC

        entity.setChild3Name(getStringValue(row.getCell(82)));
        entity.setChild3Dob(getIntegerValue(row.getCell(83)));
        entity.setChild3Age(getIntegerValue(row.getCell(84)));
        entity.setChild3Hbc(getStringValue(row.getCell(85)));

        entity.setChild4Name(getStringValue(row.getCell(88)));
        entity.setChild4Dob(getIntegerValue(row.getCell(89)));
        entity.setChild4Age(getIntegerValue(row.getCell(90)));
        entity.setChild4Hbc(getStringValue(row.getCell(91)));

        entity.setChild5Name(getStringValue(row.getCell(94)));
        entity.setChild5Dob(getIntegerValue(row.getCell(95)));
        entity.setChild5Age(getIntegerValue(row.getCell(96)));
        entity.setChild5Hbc(getStringValue(row.getCell(97)));

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
}

