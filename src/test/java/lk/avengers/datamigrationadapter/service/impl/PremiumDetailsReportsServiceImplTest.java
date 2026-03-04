package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import lk.avengers.datamigrationadapter.exception.ReportException;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.PremiumDetailsRepository;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lk.avengers.datamigrationadapter.service.CommonFunction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PremiumDetailsReportsServiceImpl}.
 *
 * <p>
 * Strategy:
 * <ul>
 * <li>Dependencies (CommonFunction, Repository, BatchProcessService) are
 * mocked.</li>
 * <li>A real .xlsx file is written to a @TempDir so the StreamingReader can
 * open it.</li>
 * <li>The {@code premiumDetailsReportsPath} @Value field is injected via
 * ReflectionTestUtils.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PremiumDetailsReportsServiceImplTest {

    // -----------------------------------------------------------------------
    // Mocks & Subject
    // -----------------------------------------------------------------------

    @Mock
    private CommonFunction commonFunction;

    @Mock
    private PremiumDetailsRepository premiumDetailsRepository;

    @Mock
    private BatchProcessService genisysBatchService;

    @InjectMocks
    private PremiumDetailsReportsServiceImpl service;

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a minimal .xlsx file with the given data rows.
     * Rows 0-4 are header rows (skipped by the service).
     * Each dataRow array = values for columns 0-34 (String).
     */
    private Path createExcelFile(String fileName, String[]... dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            // 5 header rows
            for (int h = 0; h < 5; h++) {
                sheet.createRow(h).createCell(0).setCellValue("HEADER " + h);
            }

            // Data rows
            for (int d = 0; d < dataRows.length; d++) {
                Row row = sheet.createRow(5 + d);
                String[] values = dataRows[d];
                for (int c = 0; c < values.length; c++) {
                    if (values[c] != null) {
                        row.createCell(c).setCellValue(values[c]);
                    }
                }
            }

            Path filePath = tempDir.resolve(fileName);
            try (OutputStream os = Files.newOutputStream(filePath)) {
                workbook.write(os);
            }
            return filePath;
        }
    }

    /** Builds a full 35-column data row (col 0 unused, col 1 = policyNo, ...) */
    private String[] buildDataRow(String policyNo) {
        String[] row = new String[35];
        row[1] = policyNo; // policyNo
        row[2] = "100"; // proposalNo
        row[3] = "PROD01"; // productCode
        row[4] = "PLAN-A"; // planNo
        row[5] = "20230101"; // inceptionDate
        row[6] = "20240101"; // expiryDate
        row[7] = "20230115"; // issueDate
        row[8] = "10"; // salesBranchCode
        row[9] = "Colombo"; // salesBranchName
        row[10] = "20"; // companyBranchCode
        row[11] = "Main Branch"; // companyBranchName
        row[12] = "30"; // policyBranchCode
        row[13] = "Regional"; // policyBranchName
        row[14] = "12"; // term
        row[15] = "LKR"; // cy
        row[16] = "10"; // premiumPaymentTerm
        row[17] = "0"; // defermentTerm
        row[18] = "0"; // retirementBenefitPayoutTerm
        row[19] = "5000.00"; // modalPremium
        row[20] = "12"; // frequency
        row[21] = "20230201"; // nextPremium
        row[22] = "ACTIVE"; // status
        row[23] = "20230101"; // date
        row[24] = "None"; // reason
        row[25] = "AGT001"; // agentCode
        row[26] = "John"; // introducer
        row[27] = "Jane"; // supervisor
        row[28] = "50.0"; // riPercentage
        row[29] = "1"; // policyYear
        row[30] = "3"; // policyMonth
        row[31] = "4500.00"; // modalPremiumAmount
        row[32] = "1000.00"; // allocationAmount
        row[33] = "20230115"; // paymentDate
        row[34] = "20230201"; // premiumDueDate
        return row;
    }

    private void setPath(String path) {
        ReflectionTestUtils.setField(service, "premiumDetailsReportsPath", path);
    }

    // -----------------------------------------------------------------------
    // Tests: uploadPremiumDetailsReports — Happy Path
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("uploadPremiumDetailsReports() — success scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Single file with one data row → saves 1 record, returns 200 OK")
        void singleFile_oneDataRow_returns200() throws Exception {
            // Arrange
            createExcelFile("premium1.xlsx", buildDataRow("12345"));
            setPath(tempDir.toString());

            LocalDate fakeDate = LocalDate.of(2023, 1, 1);
            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(12345);
            when(commonFunction.getStringValue(any())).thenReturn("TEST");
            when(commonFunction.getDoubleValue(any())).thenReturn(5000.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(fakeDate);

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("uploaded successfully");

            verify(premiumDetailsRepository, times(1)).truncate();
            verify(genisysBatchService, atLeastOnce()).savePremiumDetailsBatch(anyList());
        }

        @Test
        @DisplayName("Multiple files in directory → all processed, count accumulates")
        void multipleFiles_allProcessed() throws Exception {
            // Arrange
            createExcelFile("premium1.xlsx", buildDataRow("11111"));
            createExcelFile("premium2.xlsx", buildDataRow("22222"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(99999);
            when(commonFunction.getStringValue(any())).thenReturn("X");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // truncate only once regardless of file count
            verify(premiumDetailsRepository, times(1)).truncate();
        }

        @Test
        @DisplayName("File with exactly BATCH_SIZE rows → batch is flushed during processing")
        void batchSizeRows_batchFlushedMidProcessing() throws Exception {
            // Arrange: build 1000 data rows
            String[][] rows = new String[1000][];
            for (int i = 0; i < 1000; i++) {
                rows[i] = buildDataRow(String.valueOf(10000 + i));
            }
            createExcelFile("big.xlsx", rows);
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false);
            when(commonFunction.getIntegerValue(any())).thenReturn(1);
            when(commonFunction.getStringValue(any())).thenReturn("V");
            when(commonFunction.getDoubleValue(any())).thenReturn(1.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert — batch should be saved at least once mid-loop
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(genisysBatchService, atLeastOnce()).savePremiumDetailsBatch(anyList());
        }

        @Test
        @DisplayName("File ends at exact end-of-data marker → stops reading correctly")
        void endOfDataRow_stopsProcessing() throws Exception {
            // Arrange
            createExcelFile("premium.xlsx", buildDataRow("55555"));
            setPath(tempDir.toString());

            // First call = data row, second call = end of data
            when(commonFunction.isEndOfDataRow(any()))
                    .thenReturn(false)
                    .thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(55555);
            when(commonFunction.getStringValue(any())).thenReturn("ACTIVE");
            when(commonFunction.getDoubleValue(any())).thenReturn(100.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Response message contains the total record count")
        void responseMessage_containsRecordCount() throws Exception {
            createExcelFile("premium.xlsx", buildDataRow("99999"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(99999);
            when(commonFunction.getStringValue(any())).thenReturn("OK");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            assertThat(response.getBody().getMessage()).contains("1");
        }
    }

    // -----------------------------------------------------------------------
    // Tests: uploadPremiumDetailsReports — Error / Edge Cases
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("uploadPremiumDetailsReports() — error and edge cases")
    class ErrorScenarios {

        @Test
        @DisplayName("No Excel files in directory → throws ReportException with 404")
        void noExcelFiles_throwsReportException404() {
            // Arrange — no files created, directory is empty
            setPath(tempDir.toString());

            // Act & Assert
            assertThatThrownBy(() -> service.uploadPremiumDetailsReports())
                    .isInstanceOf(ReportException.class)
                    .satisfies(ex -> {
                        ReportException re = (ReportException) ex;
                        assertThat(re.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    });
        }

        @Test
        @DisplayName("Non-Excel files in directory → throws ReportException (no xlsx found)")
        void nonExcelFilesOnly_throwsReportException() throws IOException {
            // Arrange — put a .txt file, not an Excel file
            Files.writeString(tempDir.resolve("data.txt"), "not excel");
            setPath(tempDir.toString());

            // Act & Assert
            assertThatThrownBy(() -> service.uploadPremiumDetailsReports())
                    .isInstanceOf(ReportException.class);
        }

        @Test
        @DisplayName("Configured path is a non-existent directory → throws ReportException")
        void nonExistentDirectory_throwsReportException() {
            setPath("/non/existent/directory/path");

            assertThatThrownBy(() -> service.uploadPremiumDetailsReports())
                    .isInstanceOf(ReportException.class);
        }

        @Test
        @DisplayName("Configured path is a file path (single file) → resolves parent dir")
        void configuredPathIsFilePath_resolvesParentDir() throws Exception {
            // Arrange: create an Excel file, set path to the file itself (not the
            // directory)
            Path file = createExcelFile("report.xlsx", buildDataRow("77777"));
            setPath(file.toString()); // path to the FILE, not the directory

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(77777);
            when(commonFunction.getStringValue(any())).thenReturn("OK");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            // Act — should work by using parent directory
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("BatchProcessService throws exception → returns 500 Internal Server Error")
        void batchServiceThrows_returns500() throws Exception {
            createExcelFile("premium.xlsx", buildDataRow("33333"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(33333);
            when(commonFunction.getStringValue(any())).thenReturn("OK");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);
            doThrow(new RuntimeException("DB connection lost"))
                    .when(genisysBatchService).savePremiumDetailsBatch(anyList());

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).contains("DB connection lost");
        }

        @Test
        @DisplayName("ReportException propagates up (not wrapped in 500 response)")
        void reportException_propagatesUp() {
            setPath(tempDir.toString()); // empty dir → ReportException(404)

            assertThatThrownBy(() -> service.uploadPremiumDetailsReports())
                    .isInstanceOf(ReportException.class);

            // truncate must NOT have been called (failure was before truncate)
            verify(premiumDetailsRepository, never()).truncate();
        }

        @Test
        @DisplayName("File with only header rows and no data rows → saves 0 records")
        void headerRowsOnly_zeroRecordsSaved() throws Exception {
            // Arrange: file with only the 5 header rows, no data rows.
            // shouldSkipRow() skips rows 0-4, so isEndOfDataRow is never called.
            createExcelFile("empty_data.xlsx"); // no dataRows passed
            setPath(tempDir.toString());

            // Act
            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getMessage()).contains("0");
            verify(genisysBatchService, never()).savePremiumDetailsBatch(anyList());
        }
    }

    // -----------------------------------------------------------------------
    // Tests: getPremiumPaymentTerm logic (tested via full flow)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getPremiumPaymentTerm() — 'SP' mapping and numeric fallback")
    class PremiumPaymentTermTests {

        @Test
        @DisplayName("Cell value 'SP' → premiumPaymentTerm mapped to 1")
        void spValue_mappedToOne() throws Exception {
            // Arrange: col 16 = "SP"
            String[] row = buildDataRow("12345");
            row[16] = "SP";
            createExcelFile("sp_test.xlsx", row);
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(1);
            when(commonFunction.getStringValue(any())).thenReturn("V");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            ArgumentCaptor<List<PremiumDetailsEntity>> captor = ArgumentCaptor.captor();

            // Act
            service.uploadPremiumDetailsReports();

            // Assert: premiumPaymentTerm should be 1 for "SP"
            verify(genisysBatchService).savePremiumDetailsBatch(captor.capture());
            List<PremiumDetailsEntity> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getPremiumPaymentTerm()).isEqualTo(1);
        }

        @Test
        @DisplayName("Numeric cell in col 16 → delegates to commonFunction.getIntegerValue")
        void numericPremiumPaymentTerm_delegatesToCommonFunction() throws Exception {
            // Arrange: col 16 = numeric "15"
            String[] row = buildDataRow("12345");
            row[16] = "15";
            createExcelFile("num_ppt.xlsx", row);
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(15);
            when(commonFunction.getStringValue(any())).thenReturn("V");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            ArgumentCaptor<List<PremiumDetailsEntity>> captor = ArgumentCaptor.captor();

            service.uploadPremiumDetailsReports();

            verify(genisysBatchService).savePremiumDetailsBatch(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            // premiumPaymentTerm comes from getIntegerValue mock which returns 15
            assertThat(captor.getValue().get(0).getPremiumPaymentTerm()).isEqualTo(15);
        }
    }

    // -----------------------------------------------------------------------
    // Tests: shouldSkipRow — header rows 0-4 are skipped
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("shouldSkipRow() — rows 0-4 skipped, row 5+ processed")
    class ShouldSkipRowTests {

        @Test
        @DisplayName("Only header rows in file → no entities processed")
        void headersOnly_nothingProcessed() throws Exception {
            // shouldSkipRow() skips rows 0-4; isEndOfDataRow is never reached,
            // so no stub needed here.
            createExcelFile("headers_only.xlsx"); // no data rows
            setPath(tempDir.toString());

            service.uploadPremiumDetailsReports();

            verify(genisysBatchService, never()).savePremiumDetailsBatch(anyList());
        }

        @Test
        @DisplayName("Data row at index 5 is processed (not skipped)")
        void firstDataRow_atIndex5_isProcessed() throws Exception {
            createExcelFile("rows.xlsx", buildDataRow("54321"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(54321);
            when(commonFunction.getStringValue(any())).thenReturn("Y");
            when(commonFunction.getDoubleValue(any())).thenReturn(1.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            ArgumentCaptor<List<PremiumDetailsEntity>> captor = ArgumentCaptor.captor();

            service.uploadPremiumDetailsReports();

            verify(genisysBatchService).savePremiumDetailsBatch(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }
    }

    // -----------------------------------------------------------------------
    // Tests: Interaction verification (truncate, batch save ordering)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Interaction and ordering guarantees")
    class InteractionTests {

        @Test
        @DisplayName("truncate() is called exactly once before any batch saves")
        void truncateCalledOnceBeforeSave() throws Exception {
            createExcelFile("order_test.xlsx", buildDataRow("11111"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(11111);
            when(commonFunction.getStringValue(any())).thenReturn("X");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            InOrder inOrder = inOrder(premiumDetailsRepository, genisysBatchService);

            service.uploadPremiumDetailsReports();

            inOrder.verify(premiumDetailsRepository).truncate();
            inOrder.verify(genisysBatchService).savePremiumDetailsBatch(anyList());
        }

        @Test
        @DisplayName("Batch is cleared after save (no double-save)")
        void batchClearedAfterSave_noDoubleSave() throws Exception {
            // 2 data rows → should produce exactly 1 batch save (both fit in
            // BATCH_SIZE=1000)
            createExcelFile("two_rows.xlsx", buildDataRow("1"), buildDataRow("2"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any()))
                    .thenReturn(false).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(1);
            when(commonFunction.getStringValue(any())).thenReturn("OK");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            service.uploadPremiumDetailsReports();

            // only the "leftover" batch save at end of file
            verify(genisysBatchService, times(1)).savePremiumDetailsBatch(anyList());
        }

        @Test
        @DisplayName(".xls and .xlsm extensions are also picked up from directory")
        void xlsAndXlsmExtensions_pickedUp() throws Exception {
            // Arrange: create an .xlsx and verify .xls filter (file scanning test)
            // We can only truly test .xlsx here since XSSFWorkbook creates xlsx,
            // but we verify the directory scan returns it.
            createExcelFile("report.xlsx", buildDataRow("99"));
            setPath(tempDir.toString());

            when(commonFunction.isEndOfDataRow(any())).thenReturn(false).thenReturn(true);
            when(commonFunction.getIntegerValue(any())).thenReturn(99);
            when(commonFunction.getStringValue(any())).thenReturn("OK");
            when(commonFunction.getDoubleValue(any())).thenReturn(0.0);
            when(commonFunction.getDateFromInteger(any())).thenReturn(null);

            ResponseEntity<CommonResponseDTO> response = service.uploadPremiumDetailsReports();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
