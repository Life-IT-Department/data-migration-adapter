package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;
import lk.avengers.datamigrationadapter.service.ExcelDataExtractorEnhancedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class ExcelDataExtractorEnhancedServiceImpl implements ExcelDataExtractorEnhancedService {

    // File size threshold for switching to streaming API (in MB)
    private static final long STREAMING_THRESHOLD_MB = 30;

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

            long fileSizeInMB = file.length() / (1024 * 1024);
            String fileName = file.getName().toLowerCase();
            log.info("Processing Excel file from path: {} (Size: {} MB)", requestDTO.getFilePath(), fileSizeInMB);

            // Log memory before processing
            logMemoryUsage("Before Excel Processing");

            ExcelDataResponseDTO result;

            // Use streaming API for large files or XLSM files (which are complex)
            if (fileSizeInMB > STREAMING_THRESHOLD_MB || fileName.endsWith(".xlsm")) {
                log.info("Using STREAMING API for large/complex file (Size: {} MB, Type: {})",
                        fileSizeInMB, fileName.substring(fileName.lastIndexOf('.')));
                result = extractUsingStreamingAPI(file, requestDTO);
            } else {
                log.info("Using STANDARD API for smaller file");
                result = extractUsingStandardAPI(file, requestDTO);
            }

            // Log memory after processing
            logMemoryUsage("After Excel Processing");

            return result;

        } catch (OutOfMemoryError oom) {
            log.error("OUT OF MEMORY while processing file: {}", requestDTO.getFilePath(), oom);
            logMemoryUsage("After OOM Error");
            System.gc(); // Force garbage collection
            throw new RuntimeException("Insufficient memory to process this Excel file. Please contact support or reduce file size.", oom);
        } catch (Exception e) {
            log.error("Error processing Excel file from path: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Streaming API implementation for large files - uses minimal memory
     * Matches original behavior: Only includes columns with non-null values in row maps
     */
    private ExcelDataResponseDTO extractUsingStreamingAPI(File file, ExcelExtractorRequestDTO requestDTO) {
        log.info("Initializing streaming Excel reader...");

        try (OPCPackage pkg = OPCPackage.open(file)) {

            XSSFReader reader = new XSSFReader(pkg);

            // Use ReadOnlySharedStringsTable for compatibility with POI 5.x
            //ReadOnlySharedStringsTable sst = (ReadOnlySharedStringsTable) reader.getSharedStringsTable();
            SharedStrings sst = reader.getSharedStringsTable();
            StylesTable styles = reader.getStylesTable();

            // Get the sheet at the specified index
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            int currentSheetIndex = 0;
            InputStream targetStream = null;
            String sheetName = null;

            while (sheets.hasNext()) {
                InputStream stream = sheets.next();
                if (currentSheetIndex == requestDTO.getSheetIndex()) {
                    targetStream = stream;
                    sheetName = sheets.getSheetName();
                    log.info("Found target sheet: {} at index {}", sheetName, currentSheetIndex);
                    break;
                }
                stream.close();
                currentSheetIndex++;
            }

            if (targetStream == null) {
                log.error("Sheet index {} not found in file", requestDTO.getSheetIndex());
                return null;
            }

            // Create handler to process rows - matches original behavior
            StreamingExcelHandler handler = new StreamingExcelHandler(
                    requestDTO.getHeaderRow(),
                    requestDTO.getDataRow()
            );

            // Parse the sheet using SAX parser
            XMLReader parser = SAXHelper.newXMLReader();
            ContentHandler contentHandler = new XSSFSheetXMLHandler(
                    styles, sst, handler, false
            );
            parser.setContentHandler(contentHandler);

            log.info("Starting SAX parsing of sheet...");
            parser.parse(new InputSource(targetStream));
            targetStream.close();

            List<Map<String, Object>> data = handler.getExtractedData();
            List<String> headers = handler.getHeaders();

            log.info("Streaming extraction complete - Headers: {}, Data rows: {}", headers.size(), data.size());

            if (data.isEmpty()) {
                log.warn("No data found in the Excel file");
                return null;
            }

            return ExcelDataResponseDTO.builder()
                    .headers(headers)
                    .extractedData(data)
                    .build();

        } catch (Exception e) {
            log.error("Error in streaming API extraction: {}", e.getMessage(), e);
            return null;
        }
    }


    /**
     * Standard API implementation - YOUR ORIGINAL CODE, UNCHANGED
     */
    private ExcelDataResponseDTO extractUsingStandardAPI(File file, ExcelExtractorRequestDTO requestDTO) {
        try (InputStream inputStream = new FileInputStream(file)) {
            Workbook workbook = createWorkbookFromPath(inputStream, file.getName());
            Sheet firstSheet = workbook.getSheetAt(requestDTO.getSheetIndex());

            String sheetName = firstSheet.getSheetName();
            log.info("Processing sheet: {}", sheetName);

            // Extract headers from the specified row
            List<String> headers = extractHeaders(firstSheet, requestDTO.getHeaderRow());
            log.info("Extracted headers: {}", headers);

            // Extract data rows
            List<Map<String, Object>> data = extractData(firstSheet, headers, requestDTO.getDataRow());
            log.info("Extracted {} data rows", data.size());

            workbook.close();

            if (data.isEmpty()) {
                log.warn("No data found in the Excel file");
                return null;
            }

            return ExcelDataResponseDTO.builder()
                    .headers(headers)
                    .extractedData(data)
                    .build();

        } catch (Exception e) {
            log.error("Error in standard API extraction: {}", e.getMessage(), e);
            return null;
        }
    }


    private Workbook createWorkbookFromPath(InputStream inputStream, String filename) throws IOException {
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xlsm")) {
                return new XSSFWorkbook(inputStream);
            } else if (lowerFilename.endsWith(".xls")) {
                return new HSSFWorkbook(inputStream);
            }
        }
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

            List<String> headers = extractHeaders(firstSheet, requestDTO.getHeaderRow());
            log.info("Extracted headers: {}", headers);

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
                return new XSSFWorkbook(inputStream);
            } else if (lowerFilename.endsWith(".xls")) {
                return new HSSFWorkbook(inputStream);
            }
        }
        try {
            return new XSSFWorkbook(inputStream);
        } catch (Exception e) {
            log.debug("Failed to read as XLSX format, trying XLS format: {}", e.getMessage());
            try {
                inputStream = file.getInputStream();
                return new HSSFWorkbook(inputStream);
            } catch (Exception ex) {
                log.error("Failed to read file in both XLSX and XLS formats");
                throw new IOException("Unsupported Excel file format", ex);
            }
        }
    }

    // YOUR ORIGINAL IMPLEMENTATION - UNCHANGED
    private List<String> extractHeaders(Sheet sheet, int rowIndex) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(rowIndex);

        if (headerRow != null) {
            int totalCells = headerRow.getLastCellNum();
            for (int i = 0; i < totalCells; i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String headerValue = getCellValueAsString(cell);
                headers.add(headerValue != null ? headerValue.trim() : "");
            }
        }

        return headers;
    }

    // YOUR ORIGINAL IMPLEMENTATION - UNCHANGED
    // Only includes columns with non-null values in the row map
    private List<Map<String, Object>> extractData(Sheet sheet, List<String> headers, int rowNum) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int rowIndex = rowNum; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowEmpty(row)) continue;

            Map<String, Object> rowData = new LinkedHashMap<>();
            for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                String header = headers.get(colIndex);
                Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Object value = getCellValue(cell);
                // ORIGINAL BEHAVIOR: Only add if value is not null
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

    private void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        log.info("Memory [{}] - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
                context, usedMemory, freeMemory, totalMemory, maxMemory);

        double usagePercentage = (double) usedMemory / maxMemory * 100;
        if (usagePercentage > 80) {
            log.warn("⚠️ Memory usage is HIGH: {}% - Consider garbage collection",
                    String.format("%.2f", usagePercentage));
        }
    }

    /**
     * CORRECT Handler for streaming Excel processing
     * Matches original behavior: Only adds columns with non-null/non-empty values to row map
     */
    private static class StreamingExcelHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final int headerRowIndex;
        private final int dataStartRowIndex;
        private int currentRowIndex = -1;
        private final List<String> headers = new ArrayList<>();
        private final List<Map<String, Object>> extractedData = new ArrayList<>();
        private Map<String, Object> currentRowData = null;
        private boolean isHeaderRowProcessed = false;

        public StreamingExcelHandler(int headerRowIndex, int dataStartRowIndex) {
            this.headerRowIndex = headerRowIndex;
            this.dataStartRowIndex = dataStartRowIndex;
        }

        @Override
        public void startRow(int rowNum) {
            currentRowIndex = rowNum;

            if (rowNum == headerRowIndex) {
                headers.clear();
            } else if (rowNum >= dataStartRowIndex && isHeaderRowProcessed) {
                currentRowData = new LinkedHashMap<>();
            }
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == headerRowIndex) {
                isHeaderRowProcessed = true;
                log.debug("Header row processed: {} headers found", headers.size());
            } else if (rowNum >= dataStartRowIndex && currentRowData != null) {
                // MATCHES ORIGINAL: Add row regardless (even if some columns are empty)
                // Empty columns are simply not in the map
                extractedData.add(currentRowData);
                currentRowData = null;

                if (extractedData.size() % 1000 == 0) {
                    log.info("Processed {} data rows so far...", extractedData.size());
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            // Parse column index from cell reference for correct mapping
            int columnIndex = new CellReference(cellReference).getCol();

            if (currentRowIndex == headerRowIndex) {
                // Collecting headers - expand list to accommodate column index
                while (headers.size() <= columnIndex) {
                    headers.add("");
                }
                headers.set(columnIndex, formattedValue != null ? formattedValue.trim() : "");

            } else if (currentRowIndex >= dataStartRowIndex && currentRowData != null && isHeaderRowProcessed) {
                // MATCHES ORIGINAL BEHAVIOR: Only add non-empty values
                if (columnIndex < headers.size()) {
                    String header = headers.get(columnIndex);
                    // Only add to map if value is not empty (matches original: if (value != null))
                    if (formattedValue != null && !formattedValue.trim().isEmpty()) {
                        currentRowData.put(header, parseValue(formattedValue));
                    }
                }
            }
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Not needed
        }

        private Object parseValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            value = value.trim();

            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(value);
                }
                return value;
            }
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Map<String, Object>> getExtractedData() {
            return extractedData;
        }
    }
}
