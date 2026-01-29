package lk.avengers.datamigrationadapter.service.impl;


import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;
import lk.avengers.datamigrationadapter.service.ExcelDataExtractorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExcelDataExtractorServiceImpl implements ExcelDataExtractorService {

    @Override
    public ExcelDataResponseDTO extractExcelFileFromPath(ExcelExtractorRequestDTO requestDTO) {
        try {
            if (requestDTO.getFilePath() == null || requestDTO.getFilePath().isEmpty()) {
                log.error("File path is null or empty in request");
                return null;
            }

            File file = new File(requestDTO.getFilePath());
            if (!file.exists()) {
                log.error("File not found at path: {}", requestDTO.getFilePath());
                return null;
            }

            log.info("Processing Excel file from path: {}", requestDTO.getFilePath());

            try (InputStream inputStream = new FileInputStream(file)) {
                Workbook workbook = createWorkbookFromPath(inputStream, file.getName());
                Sheet firstSheet = workbook.getSheetAt(requestDTO.getSheetIndex());

                String sheetName = firstSheet.getSheetName();
                log.info("Processing sheet: {}", sheetName);

                // Extract headers from the first row
                List<String> headers = extractHeaders(firstSheet, requestDTO.getHeaderRow());
                log.info("Extracted headers: {}", headers);

                // Extract data rows
                List<Map<String, Object>> data = extractData(firstSheet, headers, requestDTO.getDataRow());
                log.info("Extracted {} data rows", data.size());

                workbook.close();

                if (data.isEmpty()) {
                    log.warn("No data found in the Excel file");
                    return null;
                } else {
                    return ExcelDataResponseDTO.builder()
                            .headers(headers)
                            .extractedData(data)
                            .build();
                }
            }

        } catch (Exception e) {
            log.error("Error processing Excel file from path: {}", e.getMessage(), e);
            return null;
        }
    }

    private Workbook createWorkbookFromPath(InputStream inputStream, String filename) throws IOException {
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xlsm")) {
                // Both .xlsx and .xlsm are Office 2007+ XML format - use XSSFWorkbook
                return new XSSFWorkbook(inputStream);
            } else if (lowerFilename.endsWith(".xls")) {
                // Only .xls is the old OLE2 format - use HSSFWorkbook
                return new HSSFWorkbook(inputStream);
            }
        }
        // If the filename is null or unknown extension, use WorkbookFactory for auto-detection
        try {
            return WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            log.error("Failed to read Excel file with auto-detection");
            throw new IOException("Unsupported Excel file format", e);
        }
    }

    @Override
    public ExcelDataResponseDTO extractExcelFile(ExcelExtractorRequestDTO requestDTO) {
        try {
            log.info("Processing Excel file: {}", requestDTO.getFile().getOriginalFilename());

            Workbook workbook = createWorkbook(requestDTO.getFile());
            Sheet firstSheet = workbook.getSheetAt(requestDTO.getSheetIndex());

            String sheetName = firstSheet.getSheetName();
            log.info("Processing sheet: {}", sheetName);

            // Extract headers from the first row
            List<String> headers = extractHeaders(firstSheet, requestDTO.getHeaderRow());
            log.info("Extracted headers: {}", headers);

            // Extract data rows
            List<Map<String, Object>> data = extractData(firstSheet, headers, requestDTO.getDataRow());
            log.info("Extracted {} data rows", data.size());

            workbook.close();

            if (data.isEmpty()) {
                log.warn("No data found in the Excel file");
                return null;
            } else {
                return ExcelDataResponseDTO.builder()
                        .headers(headers)
                        .extractedData(data)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            return null;
        }
    }

    private Workbook createWorkbook(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        String filename = file.getOriginalFilename();

        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xlsm")) {
                // Both .xlsx and .xlsm are Office 2007+ XML format - use XSSFWorkbook
                return new XSSFWorkbook(inputStream);
            } else if (lowerFilename.endsWith(".xls")) {
                // Only .xls is the old OLE2 format - use HSSFWorkbook
                return new HSSFWorkbook(inputStream);
            }
        }
        // If the filename is null or unknown extension, try to detect the format
        // Try XLSX first (more common nowadays), then XLS
        try {
            return new XSSFWorkbook(inputStream);
        } catch (Exception e) {
            log.debug("Failed to read as XLSX format, trying XLS format: {}", e.getMessage());
            try {
                inputStream = file.getInputStream(); // Reset stream
                return new HSSFWorkbook(inputStream);
            } catch (Exception ex) {
                log.error("Failed to read file in both XLSX and XLS formats");
                throw new IOException("Unsupported Excel file format", ex);
            }
        }
    }


    private List<String> extractHeaders(Sheet sheet, int rowIndex) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(rowIndex); // 0-based index: row 3 in Excel

        if (headerRow != null) {
            int totalCells = headerRow.getLastCellNum(); // returns the number of cells, not the last index
            for (int i = 0; i < totalCells; i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String headerValue = getCellValueAsString(cell);
                headers.add(headerValue != null ? headerValue.trim() : "");
            }
        }

        return headers;
    }

    private List<Map<String, Object>> extractData(Sheet sheet, List<String> headers, int rowNum) {
        List<Map<String, Object>> data = new ArrayList<>();
        // Start from row 3 (skip header row)
        for (int rowIndex = rowNum; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowEmpty(row)) continue;

            Map<String, Object> rowData = new LinkedHashMap<>();
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                String header = headers.get(colIndex);
                Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Object value = getCellValue(cell);
                if (value != null) {
                    rowData.put(header, value);
                }
            }
            data.add(rowData);
        }

        return data;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                    !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Return as integer if it's a whole number
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    }
                    return numericValue;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return evaluateFormula(cell);
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                Object result = evaluateFormula(cell);
                return result != null ? result.toString() : "";
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }

    private Object evaluateFormula(Cell cell) {
        try {
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            CellValue cellValue = evaluator.evaluate(cell);

            switch (cellValue.getCellType()) {
                case STRING:
                    return cellValue.getStringValue();
                case NUMERIC:
                    double numericValue = cellValue.getNumberValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    }
                    return numericValue;
                case BOOLEAN:
                    return cellValue.getBooleanValue();
                default:
                    return cell.getCellFormula();
            }
        } catch (Exception e) {
            log.warn("Error evaluating formula in cell {}: {}", cell.getAddress(), e.getMessage());
            return cell.getCellFormula();
        }
    }
}