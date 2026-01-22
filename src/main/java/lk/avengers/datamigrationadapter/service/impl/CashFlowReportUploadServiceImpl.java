package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.CashFlowReportRepository;
import lk.avengers.datamigrationadapter.service.CashFlowReportUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowReportUploadServiceImpl implements CashFlowReportUploadService {

    @Value("${cashFlowReport.file}")
    private String cashFlowReportPath;

    private final CashFlowReportRepository cashFlowReportRepository;
    DataFormatter dataFormatter = new DataFormatter();

    @Override
    public ResponseEntity<CommonResponseDTO> uploadCashFlowReport() {

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        try (FileInputStream file = new FileInputStream(cashFlowReportPath)) {

            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            iterator.next();
            iterator.next();
            iterator.next();
            iterator.next();

            List<CashFlowReportEntity> cashFlowList = new ArrayList<>();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                if (!isRowEmpty(row)) {
                    cashFlowList.add(
                            CashFlowReportEntity.builder()
                                    .fiscalYear(dataFormatter.formatCellValue(row.getCell(1)).trim())
                                    .ACDocument(dataFormatter.formatCellValue(row.getCell(2)).trim())
                                    .OperationDate(((row.getCell(3) != null)  && (row.getCell(3).getLocalDateTimeCellValue() != null)) ? row.getCell(3).getLocalDateTimeCellValue().format(DateTimeFormatter.ISO_DATE) : "")
                                    .time(((row.getCell(4) != null) && (row.getCell(4).getLocalDateTimeCellValue() != null)) ? row.getCell(4).getLocalDateTimeCellValue().toLocalTime().format(DateTimeFormatter.ISO_TIME) : "")
                                    .station(dataFormatter.formatCellValue(row.getCell(5)).trim())
                                    .receiptNo(dataFormatter.formatCellValue(row.getCell(6)).trim())
                                    .payerPin(dataFormatter.formatCellValue(row.getCell(7)).trim())
                                    .payerName(dataFormatter.formatCellValue(row.getCell(8)).trim())
                                    .payerAddress(dataFormatter.formatCellValue(row.getCell(9)).trim())
                                    .details(dataFormatter.formatCellValue(row.getCell(10)).trim())
                                    .paymentMode(dataFormatter.formatCellValue(row.getCell(11)).trim())
                                    .reference(row.getCell(12) != null ? dataFormatter.formatCellValue(row.getCell(12)).trim() : "")
                                    .description(dataFormatter.formatCellValue(row.getCell(13)).trim())
                                    .accPin(dataFormatter.formatCellValue(row.getCell(14)).trim())
                                    .accSeq(dataFormatter.formatCellValue(row.getCell(15)).trim())
                                    .checkNo(dataFormatter.formatCellValue(row.getCell(16)).trim())
                                    .checkDate(dataFormatter.formatCellValue(row.getCell(17)).trim())
                                    .checkStatus(dataFormatter.formatCellValue(row.getCell(18)).trim())
                                    .paymentType(dataFormatter.formatCellValue(row.getCell(19)).trim())
                                    .agency(dataFormatter.formatCellValue(row.getCell(20)).trim())
                                    .drawnBank(dataFormatter.formatCellValue(row.getCell(21)).trim())
                                    .clearingBank(dataFormatter.formatCellValue(row.getCell(22)).trim())
                                    .amountLC(dataFormatter.formatCellValue(row.getCell(23)).trim())
                                    .postedBy(dataFormatter.formatCellValue(row.getCell(24)).trim())
                                    .paidAmount(dataFormatter.formatCellValue(row.getCell(25)).trim())
                                    .totalAmount(dataFormatter.formatCellValue(row.getCell(26)).trim())
                                    .receiptCancellation(dataFormatter.formatCellValue(row.getCell(27)).trim())
                                    .reason(dataFormatter.formatCellValue(row.getCell(28)).trim())
                                    .authorizer(dataFormatter.formatCellValue(row.getCell(29)).trim())
                                    .year(currentYear)
                                    .month(currentMonth)
                                    .sysDate(new Date())
                                    .build()
                    );
                }
            }
            cashFlowReportRepository.truncate();
            cashFlowReportRepository.saveAll(cashFlowList);

            workbook.close();

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message("Cash flow records uploaded successfully.")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            log.error("Error processing cash flow report file");
            throw new ReportException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to parse Excel file: " + e.getMessage()
            );
        }
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);

            if (cell != null && !Objects.equals(cell, "") && row.getCell(c).getCellType() != CellType.BLANK) {
                return false;
            }

        }
        return true;
    }
}
