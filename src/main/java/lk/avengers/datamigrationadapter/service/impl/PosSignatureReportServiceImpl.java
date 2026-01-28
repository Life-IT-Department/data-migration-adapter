package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PosSignatureReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PosSignatureReportRepository;
import lk.avengers.datamigrationadapter.service.PosSignatureReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosSignatureReportServiceImpl implements PosSignatureReportService {

    @Value("${posSignatureReport.file}")
    private String filePath;

    private final PosSignatureReportRepository posSignatureReportRepository;
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    @Override
    public ResponseEntity<CommonResponseDTO> uploadPosSignatureReport(int year) {

        try (InputStream is = new FileInputStream(Path.of(filePath).toFile());
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<PosSignatureReportEntity> entities = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                PosSignatureReportEntity entity = new PosSignatureReportEntity();

                entity.setProposalType(getString(row.getCell(0)));
                entity.setProposalNo(getString(row.getCell(1)));
                entity.setPolicyNo(getString(row.getCell(2)));
                entity.setAdvisorCode(getString(row.getCell(3)));
                entity.setBranch(getString(row.getCell(4)));
                entity.setCustomerName(getString(row.getCell(5)));
                entity.setCustomerAddress(getString(row.getCell(6)));
                entity.setCustomerMobile(getString(row.getCell(7)));

                entity.setProposalTransferredDate(getDate(row.getCell(8)));
                entity.setPolicyIssuanceDate(getDate(row.getCell(9)));
                entity.setPolicyInceptionDate(getDate(row.getCell(10)));

                entity.setPremium(getDecimal(row.getCell(11)));
                entity.setPolicyYear(getInteger(row.getCell(12)));
                entity.setBasicCommissionOnPremium(getDecimal(row.getCell(13)));
                entity.setYear(year);

                entities.add(entity);
            }

            posSignatureReportRepository.saveAll(entities);
            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message("POS Signature report was uploaded successfully")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            throw new ReportException(HttpStatus.BAD_REQUEST, "Failed to upload POS Signature report from path: " + filePath);
        }
    }

    /* ================= Helpers ================= */

    private String getString(Cell cell) {
        if (cell == null) return null;
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private Integer getInteger(Cell cell) {
        if (cell == null) return null;
        return (int) cell.getNumericCellValue();
    }

    private BigDecimal getDecimal(Cell cell) {
        if (cell == null) return null;
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }

    private LocalDate getDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }
}
