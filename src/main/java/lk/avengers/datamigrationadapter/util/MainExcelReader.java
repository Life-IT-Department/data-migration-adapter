package lk.avengers.datamigrationadapter.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MainExcelReader {

    @Value("${policy.number.list.file}")
    private String policyNumberListFilePath;

    public List<String> readPolicyNumbers(){
        File file = new File(policyNumberListFilePath);

        if (!file.exists() || !file.isFile()) {
            throw new IllegalStateException("File not found: " + policyNumberListFilePath);
        }

        List<String> policyNumbers = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(is)) {

//            Sheet sheet = workbook.getSheet("POC_3_UAT");
//            Sheet sheet = workbook.getSheet("POC_3_INTERMEDIATE");
            Sheet sheet = workbook.getSheet("POC_4");
//            Sheet sheet = workbook.getSheet("ACTUARIAL");
//            Sheet sheet = workbook.getSheet("TEST");

            for (Row row : sheet) {

                if (row == null) continue;

                Cell cell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) continue;

                String value = formatter.formatCellValue(cell).trim();

                if (!value.isEmpty()) {
                    policyNumbers.add(value.toUpperCase());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error reading Excel file: " + policyNumberListFilePath, e);
        }

        return policyNumbers;
    }
}
