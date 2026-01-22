package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.service.ContactDetailReportService;
import lk.avengers.reportscheduler.common.CommonConstants;
import lk.avengers.reportscheduler.dto.message.MessageDTO;
import lk.avengers.reportscheduler.entity.ausys.MasterProposalAusys;
import lk.avengers.reportscheduler.entity.payme.PhPin;
import lk.avengers.reportscheduler.repository.ausys.MasterProposalAusysRepository;
import lk.avengers.reportscheduler.repository.payme.PayMeRepository;
import lk.avengers.reportscheduler.service.ContactDetailService;
import lk.avengers.reportscheduler.util.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactDetailReportServiceImpl implements ContactDetailReportService {

    @Value("${payme.report.path}")
    private String payMeReportPath;

    private final PayMeReportRepository payMeRepository;

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");

    public void processExcel() {
        log.info("Excel processing started...");
        try {
            FileInputStream excelFile = new FileInputStream(payMeReportPath);
            try {
                XSSFWorkbook xSSFWorkbook = new XSSFWorkbook(excelFile);
                try {
                    readExcel(xSSFWorkbook);
                    xSSFWorkbook.close();
                } catch (Throwable throwable) {
                    try {
                        xSSFWorkbook.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                excelFile.close();
            } catch (Throwable throwable) {
                try {
                    excelFile.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (FileNotFoundException fe) {
            log.warn("File not found in the specified location : {}", payMeReportPath);
        } catch (IOException e) {
            log.error("Exception while processing excel. Please contact life it technical team");
            log.error(e.getMessage());
        }
    }

    private void readExcel(Workbook workbook) {
        log.info("File available and performing reading operations...");
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();
        List<PhPin> phPinList = new ArrayList<>();
        List<PhPin> phPinListFromDb = this.payMeRepository.findAll();
        DataFormatter dataformatter = new DataFormatter();
        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            if (rowNumber == 0 || rowNumber == 1) {
                rowNumber++;
                continue;
            }
            PhPin phPinExcel = processCell(currentRow, dataformatter);
            phPinListFromDb.stream()
                    .filter(phPin -> (phPin.getPolicyNo().equals(phPinExcel.getPolicyNo()) && phPin.getProductCode().equals(phPinExcel.getProductCode())))
                    .findAny()
                    .ifPresentOrElse(phPin -> log.info("This record is available in the database {}", phPinExcel), () -> phPinList.add(phPinExcel));
        }
        if (!phPinList.isEmpty()) {
            LocalDateTime startInsert = LocalDateTime.now();
            List<PhPin> pl = this.payMeRepository.saveAll(phPinList);
            LocalDateTime endInsert = LocalDateTime.now();
            log.info("Bulk saving of ph pin list successfully completed");
            log.info("{} data inserted, duration in second {}", pl.size(), ChronoUnit.SECONDS.between(startInsert, endInsert));
        }
    }

    private PhPin processCell(Row currentRow, DataFormatter dataformatter) {
        PhPin phPin = new PhPin();
        phPin.setCreateBy("system");
        phPin.setCy("144");
        phPin.setDescription("LKR");
        phPin.setPhName(dataformatter.formatCellValue(currentRow.getCell(4)).trim() + " " + dataformatter.formatCellValue(currentRow.getCell(4)).trim() + " " + dataformatter.formatCellValue(currentRow.getCell(5)).trim());
        phPin.setPinNumber(dataformatter.formatCellValue(currentRow.getCell(3)).trim());
        phPin.setPolicyNo(dataformatter.formatCellValue(currentRow.getCell(2)).trim());
        phPin.setProductCode(dataformatter.formatCellValue(currentRow.getCell(1)).trim());
        phPin.setStatus(dataformatter.formatCellValue(currentRow.getCell(17)).trim());
        phPin.setTotalNetOutstanding(dataformatter.formatCellValue(currentRow.getCell(24)).trim());
        phPin.setUpdateBy("system");
        phPin.setMobileNo(dataformatter.formatCellValue(currentRow.getCell(14)).trim());
        phPin.setEmail(dataformatter.formatCellValue(currentRow.getCell(16)).trim());
        return phPin;
    }
}
