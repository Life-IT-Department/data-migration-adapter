package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.service.StreamingExcelReaderServiceEdit;
import org.springframework.stereotype.Service;
import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class StreamingExcelReaderServiceEditImpl implements StreamingExcelReaderServiceEdit {
    /**
     * Read large Excel files using streaming API
     *
     * @param filePath Path to Excel file
     * @param sheetIndex Sheet index (0-based)
     * @param headerRowIndex Header row index (0-based)
     * @param dataStartRowIndex First data row index (0-based)
     * @return ExcelDataResponseDTO with headers and data
     */
    public ExcelDataResponseDTO readExcelStreaming(String filePath,
                                                   int sheetIndex,
                                                   int headerRowIndex,
                                                   int dataStartRowIndex) {
        try {
            log.info("Starting STREAMING read of Excel file: {}", filePath);
            log.info("Parameters: sheetIndex={}, headerRowIndex={}, dataStartRowIndex={}",
                    sheetIndex, headerRowIndex, dataStartRowIndex);

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
                log.info("Shared strings count: {}", sharedStrings.getCount());

                // Create the streaming handler with DEBUG
                StreamingSheetHandler handler = new StreamingSheetHandler(
                        headers, dataRows, headerRowIndex, dataStartRowIndex
                );

                // Get the specific sheet
                XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
                InputStream sheetStream = null;

                int currentSheet = 0;
                while (sheets.hasNext()) {
                    sheetStream = sheets.next();
                    String sheetName = sheets.getSheetName();
                    log.info("Found sheet {} at index {}: {}", currentSheet, currentSheet, sheetName);

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

                // IMPORTANT: Create handler that will call our StreamingSheetHandler
                XSSFSheetXMLHandler xmlHandler = new XSSFSheetXMLHandler(
                        styles,
                        sharedStrings,
                        handler,  // Our custom handler
                        formatter,
                        false
                );

                xmlReader.setContentHandler(xmlHandler);

                // Parse the sheet (streaming)
                log.info("Starting to parse sheet data...");
                log.info("Looking for headers at row index: {}", headerRowIndex);
                log.info("Looking for data starting at row index: {}", dataStartRowIndex);

                xmlReader.parse(new InputSource(sheetStream));
                sheetStream.close();

                long duration = System.currentTimeMillis() - startTime;
                double seconds = duration / 1000.0;

                log.info("✓ Streaming read completed successfully");
                log.info("  - Time taken: {} seconds", seconds);
                log.info("  - Headers extracted: {}", headers.size());
                log.info("  - Data rows extracted: {}", dataRows.size());

                if (headers.size() > 0) {
                    log.info("  - Headers: {}", headers);
                }

                if (dataRows.size() > 0) {
                    log.info("  - Processing speed: {} rows/second", (int)(dataRows.size() / seconds));
                    log.info("  - First row sample: {}", dataRows.get(0));
                } else {
                    log.warn("  - NO DATA ROWS EXTRACTED! Check row indices.");
                }

                return ExcelDataResponseDTO.builder()
                        .headers(headers)
                        .extractedData(dataRows)
                        .build();

            }

        } catch (Exception e) {
            log.error("Error during streaming read: {}", e.getMessage(), e);
            e.printStackTrace();
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

            // DEBUG
            if (rowNum <= 10) {
                System.out.println("▶ startRow called: rowNum=" + rowNum);
            }
        }

        @Override
        public void endRow(int rowNum) {
            // DEBUG
            if (rowNum <= 10) {
                System.out.println("◀ endRow called: rowNum=" + rowNum +
                        ", headerRowIndex=" + headerRowIndex +
                        ", dataStartRowIndex=" + dataStartRowIndex +
                        ", cells=" + currentRowCells.size());
            }

            // Process header row
            if (rowNum == headerRowIndex) {
                System.out.println(">>> PROCESSING HEADER ROW at index " + rowNum);

                // Sort columns by index
                List<Integer> sortedCols = new ArrayList<>(currentRowCells.keySet());
                Collections.sort(sortedCols);

                System.out.println(">>> Found " + sortedCols.size() + " columns in header row");
                System.out.println(">>> Column data: " + currentRowCells);

                // Extract headers in order
                for (Integer colIndex : sortedCols) {
                    String headerValue = currentRowCells.get(colIndex);
                    String cleanHeader = (headerValue != null && !headerValue.trim().isEmpty())
                            ? headerValue.trim()
                            : "Column_" + colIndex;
                    headers.add(cleanHeader);
                }

                System.out.println("✓ Headers extracted: " + headers.size() + " columns");
                System.out.println("  Headers: " + headers);

            }
            // Process data rows
            else if (rowNum >= dataStartRowIndex) {

                if (rowsProcessed < 3) {
                    System.out.println(">>> PROCESSING DATA ROW at index " + rowNum);
                }

                // Build row data
                Map<String, Object> rowData = new LinkedHashMap<>();

                // Iterate through ALL header positions
                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    String value = currentRowCells.getOrDefault(i, "");
                    rowData.put(header, value != null ? value : "");
                }

                // Check if row has ANY data
                boolean hasAnyData = false;
                for (Object value : rowData.values()) {
                    if (value != null && !value.toString().trim().isEmpty()) {
                        hasAnyData = true;
                        break;
                    }
                }

                // Add row if it has data
                if (hasAnyData) {
                    dataRows.add(rowData);
                    rowsProcessed++;

                    // Log progress
                    long now = System.currentTimeMillis();
                    if (rowsProcessed % 10000 == 0 || (now - lastLogTime) > 5000) {
                        System.out.println("  Progress: " + rowsProcessed + " rows processed...");
                        lastLogTime = now;
                    }

                    // Debug: Print first 3 rows
                    if (rowsProcessed <= 3) {
                        System.out.println("  ✓ Sample row " + rowsProcessed + ": " + rowData);
                    }
                }
            }
        }

//        @Override
//        public void endRow(int rowNum) {
//            // FIXED: Process header row
//            if (rowNum == headerRowIndex) {
//                // Sort columns by index
//                List<Integer> sortedCols = new ArrayList<>(currentRowCells.keySet());
//                Collections.sort(sortedCols);
//
//                // Extract headers in order
//                for (Integer colIndex : sortedCols) {
//                    String headerValue = currentRowCells.get(colIndex);
//                    String cleanHeader = (headerValue != null && !headerValue.trim().isEmpty())
//                            ? headerValue.trim()
//                            : "Column_" + colIndex;
//                    headers.add(cleanHeader);
//                }
//
//                System.out.println("✓ Headers extracted: " + headers.size() + " columns");
//                System.out.println("  Headers: " + headers);
//
//            }
//            // FIXED: Process data rows
//            else if (rowNum >= dataStartRowIndex) {
//
//                // Build row data - MUST use header count as reference
//                Map<String, Object> rowData = new LinkedHashMap<>();
//
//                // IMPORTANT: Iterate through ALL header positions
//                for (int i = 0; i < headers.size(); i++) {
//                    String header = headers.get(i);
//                    String value = currentRowCells.getOrDefault(i, "");
//                    rowData.put(header, value != null ? value : "");
//                }
//
//                // Check if row has ANY data (not completely empty)
//                boolean hasAnyData = false;
//                for (Object value : rowData.values()) {
//                    if (value != null && !value.toString().trim().isEmpty()) {
//                        hasAnyData = true;
//                        break;
//                    }
//                }
//
//                // Add row if it has data
//                if (hasAnyData) {
//                    dataRows.add(rowData);
//                    rowsProcessed++;
//
//                    // Log progress
//                    long now = System.currentTimeMillis();
//                    if (rowsProcessed % 10000 == 0 || (now - lastLogTime) > 5000) {
//                        System.out.println("  Progress: " + rowsProcessed + " rows processed...");
//                        lastLogTime = now;
//                    }
//
//                    // Debug: Print first 3 rows
//                    if (rowsProcessed <= 3) {
//                        System.out.println("  Sample row " + rowsProcessed + ": " + rowData);
//                    }
//                }
//            }
//        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int colIndex = getColumnIndex(cellReference);
            currentRowCells.put(colIndex, formattedValue);

            // DEBUG - first 10 rows only
            if (currentRowNum <= 10) {
                System.out.println("  cell: " + cellReference + " -> colIndex=" + colIndex + ", value=" + formattedValue);
            }
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // Not needed for data extraction
        }

        @Override
        public void endSheet() {
            System.out.println("✓ Sheet processing completed. Total rows: " + rowsProcessed);
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
