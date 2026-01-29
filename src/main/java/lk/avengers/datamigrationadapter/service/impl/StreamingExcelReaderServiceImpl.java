package lk.avengers.datamigrationadapter.service.impl;


import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.service.StreamingExcelReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * NEW SERVICE - Streaming Excel Reader for large files
 * Uses SAX parser to avoid loading entire file into memory
 */
@Slf4j
@Service
public class StreamingExcelReaderServiceImpl extends StreamingExcelReaderService {

    /**
     * Read large Excel files using streaming API
     *
     * @param filePath Path to Excel file
     * @param sheetIndex Sheet index (0-based)
     * @param headerRowIndex Header row index (0-based)
     * @param dataStartRowIndex First data row index (0-based)
     * @return ExcelDataResponseDTO with headers and data
     */
    @Override
    public ExcelDataResponseDTO readExcelStreaming(String filePath,
                                                   int sheetIndex,
                                                   int headerRowIndex,
                                                   int dataStartRowIndex) {
        try {
            log.info("Starting STREAMING read of Excel file: {}", filePath);

            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File not found: {}", filePath);
                return null;
            }

            long fileSize = file.length();
            log.info("File size: {} MB ({} bytes)", fileSize / (1024.0 * 1024.0), fileSize);

            long startTime = System.currentTimeMillis();

            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> dataRows = new ArrayList<>();

            try (OPCPackage opcPackage = OPCPackage.open(file)) {
                XSSFReader xssfReader = new XSSFReader(opcPackage);
                SharedStrings sharedStrings = xssfReader.getSharedStringsTable();
                StylesTable styles = xssfReader.getStylesTable();

                log.info("OPCPackage opened successfully");

                // Create the streaming handler
                StreamingSheetHandler handler = new StreamingSheetHandler(
                        headers, dataRows, headerRowIndex, dataStartRowIndex
                );

                // Get the specific sheet
                XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
                InputStream sheetStream = null;

                int currentSheet = 0;
                while (sheets.hasNext()) {
                    sheetStream = sheets.next();
                    if (currentSheet == sheetIndex) {
                        log.info("Found target sheet at index: {}", sheetIndex);
                        break;
                    }
                    sheetStream.close();
                    currentSheet++;
                }

                if (sheetStream == null) {
                    log.error("Sheet index {} not found in file", sheetIndex);
                    return null;
                }

                // Set up SAX parser
                SAXParserFactory saxFactory = SAXParserFactory.newInstance();
                SAXParser saxParser = saxFactory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();

                DataFormatter formatter = new DataFormatter();
                XSSFSheetXMLHandler xmlHandler = new XSSFSheetXMLHandler(
                        styles, sharedStrings, handler, formatter, false
                );

                xmlReader.setContentHandler(xmlHandler);

                // Parse the sheet (streaming)
                log.info("Starting to parse sheet data...");
                xmlReader.parse(new InputSource(sheetStream));
                sheetStream.close();

                long duration = System.currentTimeMillis() - startTime;
                double seconds = duration / 1000.0;

                log.info("✓ Streaming read completed successfully");
                log.info("  - Time taken: {} seconds", seconds);
                log.info("  - Headers extracted: {}", headers.size());
                log.info("  - Data rows extracted: {}", dataRows.size());
                log.info("  - Processing speed: {} rows/second", (int)(dataRows.size() / seconds));

                return ExcelDataResponseDTO.builder()
                        .headers(headers)
                        .extractedData(dataRows)
                        .build();

            }

        } catch (Exception e) {
            log.error("Error during streaming read: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inner class to handle SAX events during streaming
     */
    private static class StreamingSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final List<String> headers;
        private final List<Map<String, Object>> dataRows;
        private final int headerRowIndex;
        private final int dataStartRowIndex;

        private int currentRowNum = -1;
        private Map<Integer, String> currentRowCells;
        private int rowsProcessed = 0;
        private long lastLogTime = System.currentTimeMillis();

        public StreamingSheetHandler(List<String> headers,
                                     List<Map<String, Object>> dataRows,
                                     int headerRowIndex,
                                     int dataStartRowIndex) {
            this.headers = headers;
            this.dataRows = dataRows;
            this.headerRowIndex = headerRowIndex;
            this.dataStartRowIndex = dataStartRowIndex;
        }

        @Override
        public void startRow(int rowNum) {
            this.currentRowNum = rowNum;
            this.currentRowCells = new HashMap<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == headerRowIndex) {
                // Extract headers
                List<Integer> sortedCols = new ArrayList<>(currentRowCells.keySet());
                Collections.sort(sortedCols);

                for (Integer colIndex : sortedCols) {
                    String headerValue = currentRowCells.get(colIndex);
                    headers.add(headerValue != null ? headerValue.trim() : "Column_" + colIndex);
                }

                System.out.println("Headers extracted: " + headers.size() + " columns");

            } else if (rowNum >= dataStartRowIndex) {
                // Extract data row
                Map<String, Object> rowData = new LinkedHashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    String value = currentRowCells.getOrDefault(i, "");
                    rowData.put(header, value);
                }

                // Only add non-empty rows
                boolean hasData = rowData.values().stream()
                        .anyMatch(v -> v != null && !v.toString().trim().isEmpty());

                if (hasData) {
                    dataRows.add(rowData);
                    rowsProcessed++;

                    // Log progress every 10,000 rows or every 5 seconds
                    long now = System.currentTimeMillis();
                    if (rowsProcessed % 10000 == 0 || (now - lastLogTime) > 5000) {
                        System.out.println("  Progress: " + rowsProcessed + " rows processed...");
                        lastLogTime = now;
                    }
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int colIndex = getColumnIndex(cellReference);
            currentRowCells.put(colIndex, formattedValue);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Not needed for data extraction
        }

        @Override
        public void endSheet() {
            System.out.println("Sheet processing completed. Total rows: " + rowsProcessed);
        }

        /**
         * Convert Excel cell reference (e.g., "A1", "AB15") to column index
         */
        private int getColumnIndex(String cellReference) {
            if (cellReference == null || cellReference.isEmpty()) {
                return 0;
            }

            // Extract column letters (e.g., "AB15" -> "AB")
            int i = 0;
            while (i < cellReference.length() && Character.isLetter(cellReference.charAt(i))) {
                i++;
            }

            String columnLetters = cellReference.substring(0, i);

            // Convert letters to 0-based index (A=0, B=1, ... Z=25, AA=26, etc.)
            int colIndex = 0;
            for (int j = 0; j < columnLetters.length(); j++) {
                colIndex = colIndex * 26 + (columnLetters.charAt(j) - 'A' + 1);
            }

            return colIndex - 1;
        }
    }
}