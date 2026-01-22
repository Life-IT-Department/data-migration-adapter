package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PhPinEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PayMeReportRepository;
import lk.avengers.datamigrationadapter.service.ContactDetailReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactDetailReportServiceImpl implements ContactDetailReportService {

    @Value("${payMeReport.file}")
    private String payMeReportPath;

    private final PayMeReportRepository payMeReportRepository;

    @Override
    public ResponseEntity<CommonResponseDTO> processExcel() {
        log.info("Excel processing started...");
        try {
            FileInputStream excelFile = new FileInputStream(payMeReportPath);
            try {
                XSSFWorkbook xSSFWorkbook = new XSSFWorkbook(excelFile);
                try {
                    readExcel(xSSFWorkbook);
                    xSSFWorkbook.close();
                    excelFile.close();
                    return ResponseEntity.ok(
                            CommonResponseDTO.builder()
                                    .message("Main data report records uploaded successfully.")
                                    .status(HttpStatus.OK.toString())
                                    .build()
                    );
                } catch (Throwable throwable) {
                    excelFile.close();
                    try {
                        xSSFWorkbook.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
            } catch (Throwable throwable) {
                excelFile.close();
                try {
                    excelFile.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (FileNotFoundException fe) {
            log.warn("File not found in the specified location : {}", payMeReportPath);
            throw new ReportException(HttpStatus.BAD_REQUEST, "File not found in the specified location : "+ payMeReportPath);
        } catch (IOException e) {
            throw new ReportException(HttpStatus.BAD_REQUEST, "Exception while processing excel. Please contact life it technical team : " + e.getMessage());
        }
    }

    private void readExcel(Workbook workbook) {
        log.info("File available and performing reading operations...");
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        List<PhPinEntity> phPinList = new ArrayList<>();
        DataFormatter dataformatter = new DataFormatter();
        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            if (rowNumber == 0 || rowNumber == 1) {
                rowNumber++;
                continue;
            }
            phPinList.add(processRow(currentRow, dataformatter));
        }
        if (!phPinList.isEmpty()) {
            payMeReportRepository.truncate();
            LocalDateTime startInsert = LocalDateTime.now();
            List<PhPinEntity> pl = this.payMeReportRepository.saveAll(phPinList);
            LocalDateTime endInsert = LocalDateTime.now();
            log.info("Bulk saving of ph pin list successfully completed");
            log.info("{} data inserted, duration in second {}", pl.size(), ChronoUnit.SECONDS.between(startInsert, endInsert));
        }
    }

    private PhPinEntity processRow(Row currentRow, DataFormatter dataFormatter) {
        PhPinEntity phPin = new PhPinEntity();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        phPin.setProduct(dataFormatter.formatCellValue(currentRow.getCell(1)).trim());
        phPin.setPolicyNo(dataFormatter.formatCellValue(currentRow.getCell(2)).trim());
        phPin.setPin(dataFormatter.formatCellValue(currentRow.getCell(3)).trim());
        phPin.setTitle(dataFormatter.formatCellValue(currentRow.getCell(4)).trim());
        phPin.setFirstName(dataFormatter.formatCellValue(currentRow.getCell(5)).trim());
        phPin.setLastName(dataFormatter.formatCellValue(currentRow.getCell(6)).trim());
        phPin.setGender(dataFormatter.formatCellValue(currentRow.getCell(7)).trim());

        // Date of birth
        String dobStr = dataFormatter.formatCellValue(currentRow.getCell(8)).trim();
        if (!dobStr.isEmpty()) {
            phPin.setDateOfBirth(LocalDate.parse(dobStr, dateFormatter));
        }

        // Current age
        String ageStr = dataFormatter.formatCellValue(currentRow.getCell(9)).trim();
        if (!ageStr.isEmpty()) {
            phPin.setCurrentAge(Integer.parseInt(ageStr));
        }

        phPin.setNic(dataFormatter.formatCellValue(currentRow.getCell(10)).trim());
        phPin.setOccupation(dataFormatter.formatCellValue(currentRow.getCell(11)).trim());
        phPin.setAddress(dataFormatter.formatCellValue(currentRow.getCell(12)).trim());
        phPin.setCity(dataFormatter.formatCellValue(currentRow.getCell(13)).trim());
        phPin.setMobile(dataFormatter.formatCellValue(currentRow.getCell(14)).trim());
        phPin.setOtherTelephone(dataFormatter.formatCellValue(currentRow.getCell(15)).trim());
        phPin.setNationality(dataFormatter.formatCellValue(currentRow.getCell(16)).trim());
        phPin.setAgentCode(dataFormatter.formatCellValue(currentRow.getCell(17)).trim());
        phPin.setSalesBranch(dataFormatter.formatCellValue(currentRow.getCell(18)).trim());

        // BigDecimal fields
        String totalPremiumsStr = dataFormatter.formatCellValue(currentRow.getCell(19)).trim();
        if (!totalPremiumsStr.isEmpty()) {
            phPin.setTotalPremiumsPaid(new BigDecimal(totalPremiumsStr));
        }

        String outstandingStr = dataFormatter.formatCellValue(currentRow.getCell(20)).trim();
        if (!outstandingStr.isEmpty()) {
            phPin.setOutstanding(new BigDecimal(outstandingStr));
        }

        String modalPremiumStr = dataFormatter.formatCellValue(currentRow.getCell(21)).trim();
        if (!modalPremiumStr.isEmpty()) {
            phPin.setModalPremiumWithoutHandlingFee(new BigDecimal(modalPremiumStr));
        }

        // Dates
        String inceptionStr = dataFormatter.formatCellValue(currentRow.getCell(22)).trim();
        if (!inceptionStr.isEmpty()) phPin.setPolicyInceptionDate(LocalDate.parse(inceptionStr, dateFormatter));

        String issueStr = dataFormatter.formatCellValue(currentRow.getCell(23)).trim();
        if (!issueStr.isEmpty()) phPin.setPolicyIssueDate(LocalDate.parse(issueStr, dateFormatter));

        phPin.setFrequency(dataFormatter.formatCellValue(currentRow.getCell(24)).trim());

        String nextPremiumStr = dataFormatter.formatCellValue(currentRow.getCell(25)).trim();
        if (!nextPremiumStr.isEmpty()) phPin.setNextPremiumDueDate(LocalDate.parse(nextPremiumStr, dateFormatter));

        String lastPremiumStr = dataFormatter.formatCellValue(currentRow.getCell(26)).trim();
        if (!lastPremiumStr.isEmpty()) phPin.setLastPremiumPaymentDueDate(LocalDate.parse(lastPremiumStr, dateFormatter));

        String expiryStr = dataFormatter.formatCellValue(currentRow.getCell(27)).trim();
        if (!expiryStr.isEmpty()) phPin.setPolicyExpiryDate(LocalDate.parse(expiryStr, dateFormatter));

        String termStr = dataFormatter.formatCellValue(currentRow.getCell(28)).trim();
        if (!termStr.isEmpty()) phPin.setTerm(Integer.parseInt(termStr));

        String lapsedStr = dataFormatter.formatCellValue(currentRow.getCell(29)).trim();
        if (!lapsedStr.isEmpty()) phPin.setLapsedDate(LocalDate.parse(lapsedStr, dateFormatter));

        String expiry2Str = dataFormatter.formatCellValue(currentRow.getCell(30)).trim();
        if (!expiry2Str.isEmpty()) phPin.setExpiryDate(LocalDate.parse(expiry2Str, dateFormatter));

        phPin.setLanguagePreference(dataFormatter.formatCellValue(currentRow.getCell(31)).trim());

        return phPin;
    }
}
