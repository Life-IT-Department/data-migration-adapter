package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ContactDetailEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ContactDetailRepository;
import lk.avengers.datamigrationadapter.service.ContactDetailReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactDetailReportServiceImpl implements ContactDetailReportService {
    @Value("${contact.detail.file}")
    private String contactDetailReportPath;

    private final ContactDetailRepository repository;

    private static final int BATCH_SIZE = 1000;

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ResponseEntity<CommonResponseDTO> uploadContactDetailReport() {

        List<ContactDetailEntity> batchList = new ArrayList<>(BATCH_SIZE);

        Path path = Path.of(contactDetailReportPath);

        int totalSaved = 0;

        try (InputStream is = new BufferedInputStream(
                Files.newInputStream(path),
                16 * 1024 * 1024
        );
             Workbook workbook = WorkbookFactory.create(is)) {

            log.info("Truncating contact detail table...");
            repository.truncateTable();

            Sheet sheet = workbook.getSheetAt(0);

            int lastRow = sheet.getLastRowNum();

            // Data starts at row index 3 (B4)
            for (int i = 3; i <= lastRow; i++) {

                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;

                batchList.add(mapRowToEntity(row));

                if (batchList.size() == BATCH_SIZE) {
                    totalSaved += saveBatch(batchList);
                    log.info("Total records saved so far: {}", totalSaved);
                    batchList.clear();
                }
            }

            // final batch
            if (!batchList.isEmpty()) {
                totalSaved += saveBatch(batchList);
                log.info("Total records saved: {}", totalSaved);
                batchList.clear();
            }

            log.info("Completed saving {} records in contact detail table", totalSaved);

            return ResponseEntity.ok(
                    CommonResponseDTO.builder()
                            .message(totalSaved + " Contact detail records uploaded successfully.")
                            .status(HttpStatus.OK.toString())
                            .build()
            );

        } catch (Exception e) {
            log.error("Excel processing error", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponseDTO.builder()
                            .message("Error processing file: " + e.getMessage())
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                            .build());
        }
    }

    // ---------------- batch save ----------------

    private int saveBatch(List<ContactDetailEntity> batchList) {
        log.info("Saving batch of size: {}", batchList.size());
        repository.saveAll(batchList);
        repository.flush();
        return batchList.size();
    }

    // ---------------- mapping ----------------

    private ContactDetailEntity mapRowToEntity(Row row) {

        ContactDetailEntity e = new ContactDetailEntity();

        e.setProduct(getString(row, 1));
        e.setPolicyNo(String.valueOf(getInteger(row, 2)));
        e.setPin(getInteger(row, 3));
        e.setTitle(getString(row, 4));
        e.setFirstName(getString(row, 5));
        e.setLastName(getString(row, 6));
        e.setGender(getString(row, 7));
        e.setDateOfBirth(getDate(row, 8));
        e.setCurrentAge(getInteger(row, 9));
        e.setNicNumber(getString(row, 10));
        e.setOccupation(getString(row, 11));
        e.setAddress(getString(row, 12));
        e.setCity(getString(row, 13));
        e.setMobile(getString(row, 14));
        e.setOtherTelephoneNumber(getString(row, 15));
        e.setEmailAddress(getString(row, 16));
        e.setPolicyStatus(getString(row, 17));
        e.setNbrOfCustomers(getInteger(row, 19));
        e.setNationality(getString(row, 20));
        e.setAgentCode(getString(row, 21));
        e.setSalesBranch(getString(row, 22));
        e.setTotalPremiumsPaid(getBigDecimal(row, 23));
        e.setOutstanding(getBigDecimal(row, 24));
        e.setModalPremiumWithoutHandlingFee(getBigDecimal(row, 25));
        e.setPolicyInceptionDate(getDate(row, 26));
        e.setPolicyIssueDate(getDate(row, 27));
        e.setFrequency(getString(row, 28));
        e.setNextPremiumDueDate(getDate(row, 29));
        e.setLastPremiumPaymentDueDate(getDate(row, 30));
        e.setPolicyExpiryDate(getDate(row, 31));
        e.setTerm(getInteger(row, 32));
        e.setLapsedDate(getDate(row, 33));
        e.setExpiryDate(getDate(row, 34));
        e.setLanguagePreference(getString(row, 35));

        return e;
    }

    // ---------------- helpers ----------------

    private boolean isEmptyRow(Row row) {
        short lastCell = row.getLastCellNum();
        for (int i = 1; i < lastCell; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getString(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield DATA_FORMATTER.formatCellValue(cell);
                }
                yield BigDecimal.valueOf(cell.getNumericCellValue())
                        .toPlainString();
            }
            case FORMULA -> DATA_FORMATTER.formatCellValue(cell);
            default -> DATA_FORMATTER.formatCellValue(cell).trim();
        };
    }

    private Integer getInteger(Row row, int index) {
        try {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) return null;

            return switch (cell.getCellType()) {

                case STRING -> {
                    String value = cell.getStringCellValue().trim();
                    if (value.isBlank()) yield null;

                    yield parseIntegerSafe(value);
                }

                case NUMERIC -> {
                    double num = cell.getNumericCellValue();

                    // reject decimals if not whole number
                    if (num % 1 != 0) {
                        throw new NumberFormatException("Non-integer numeric value: " + num);
                    }

                    yield (int) num;
                }

                case FORMULA -> {
                    String value = DATA_FORMATTER.formatCellValue(cell).trim();
                    if (value.isBlank()) yield null;

                    yield parseIntegerSafe(value);
                }

                default -> null;
            };

        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntegerSafe(String value) {
        try {
            if (value == null || value.isBlank()) return null;

            BigDecimal bd = new BigDecimal(value.trim());

            return bd.stripTrailingZeros().intValueExact();

        } catch (Exception e) {
            try {
                return Integer.valueOf(value.trim());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private BigDecimal getBigDecimal(Row row, int index) {
        try {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) return null;

            return switch (cell.getCellType()) {

                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());

                case STRING -> {
                    String value = cell.getStringCellValue().trim();
                    if (value.isBlank()) yield null;

                    yield parseBigDecimalSafe(value);
                }

                case FORMULA -> {
                    String value = DATA_FORMATTER.formatCellValue(cell).trim();
                    if (value.isBlank()) yield null;

                    yield parseBigDecimalSafe(value);
                }

                default -> null;
            };

        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalSafe(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            // handles cases like "1,234.56"
            String cleaned = value.replace(",", "").trim();
            return new BigDecimal(cleaned);
        }
    }

    private LocalDate getDate(Row row, int index) {
        try {
            Cell cell = row.getCell(index);
            if (cell == null) return null;

            String value = DATA_FORMATTER.formatCellValue(cell);
            if (value == null || value.isBlank()) return null;

            return LocalDate.parse(value.trim(), DATE_FORMATTER);

        } catch (Exception e) {
            return null;
        }
    }
}

