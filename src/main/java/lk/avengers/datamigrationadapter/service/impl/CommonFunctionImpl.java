package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.service.CommonFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Slf4j
@Service
public class CommonFunctionImpl implements CommonFunction {
    private static final String YYYYMMDD_PATTERN = "\\d{8}";
    private static final Set<String> INVALID_DATE_VALUES = Set.of(
            "?", "-", "N/A", "NA", "NULL", "NONE", "#N/A", ""
    );
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    @Override
    public LocalDate getDateFromInteger(String date) {
        try {
            if (date == null) return null;
            String stringDate = date.trim();
            if (stringDate.isEmpty()) return null;

            // Early exit for known non-date values
            if (INVALID_DATE_VALUES.contains(stringDate.toUpperCase())) {
                return null;
            }

            // Quick validation: check if string contains at least one digit
            if (!stringDate.matches(".*\\d.*")) {
                return null;
            }

            // Handle YYYYMMDD format
            if (stringDate.matches(YYYYMMDD_PATTERN)) {
                return parseYYYYMMDD(stringDate);
            }

            // Try standard date formats
            return parseDate(stringDate);

        } catch (Exception e) {
            log.debug("Could not parse date  '{}'", date);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Cell Helpers
    // -------------------------------------------------------------------------
    @Override
    public Integer getIntegerValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (int) cell.getNumericCellValue();
                case STRING -> Integer.parseInt(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing integer value from cell value : {} cell address: {}", cell.getStringCellValue(), cell.getAddress());
            return null;
        }
    }

    @Override
    public Long getLongValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (long) cell.getNumericCellValue();
                case STRING -> Long.parseLong(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing long value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    @Override
    public Double getDoubleValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> Double.parseDouble(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing double value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    @Override
    public String getStringValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue()).trim();
                default -> null;
            };
        } catch (Exception e) {
            log.error("Error parsing string value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    @Override
    public java.math.BigDecimal getBigDecimalValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> java.math.BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> new java.math.BigDecimal(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            log.error("Error parsing BigDecimal value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    @Override
    public String getStringDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (cell.getLocalDateTimeCellValue() != null) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
            } else if (cell.getCellType() == CellType.STRING) {
                return cell.getStringCellValue().trim();
            }
            return null;
        } catch (Exception e) {
            log.error("Error parsing date value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    @Override
    public LocalTime getLocalTimeValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> {
                    // Handle Excel time as decimal (e.g., 0.5 = 12:00:00)
                    double timeValue = cell.getNumericCellValue();
                    if (timeValue >= 0 && timeValue < 1) {
                        // Convert decimal to seconds and then to LocalTime
                        int totalSeconds = (int) (timeValue * 24 * 60 * 60);
                        yield LocalTime.ofSecondOfDay(totalSeconds);
                    }
                    yield null;
                }
                case STRING -> {
                    String timeStr = cell.getStringCellValue().trim();
                    if (timeStr.isEmpty()) yield null;

                    // Try different time formats
                    DateTimeFormatter[] timeFormatters = {
                            DateTimeFormatter.ofPattern("HH:mm:ss"),
                            DateTimeFormatter.ofPattern("HH:mm"),
                            DateTimeFormatter.ofPattern("H:mm:ss"),
                            DateTimeFormatter.ofPattern("H:mm"),
                            DateTimeFormatter.ofPattern("hh:mm:ss a"),
                            DateTimeFormatter.ofPattern("h:mm:ss a"),
                            DateTimeFormatter.ofPattern("hh:mm a"),
                            DateTimeFormatter.ofPattern("h:mm a")
                    };

                    for (DateTimeFormatter formatter : timeFormatters) {
                        try {
                            yield LocalTime.parse(timeStr, formatter);
                        } catch (DateTimeParseException ignored) {
                            // Try next formatter
                        }
                    }
                    yield null;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.error("Error parsing LocalTime value from cell: {} cell address: {}", cell.toString(), cell.getAddress());
            return null;
        }
    }

    /**
     * Returns true if all columns in the row are empty or blank, meaning we have passed the last data row.
     * Processing stops when this returns true.
     */
    @Override
    public boolean isEndOfDataRow(Row row) {
        if (row == null) {
            return true;
        }
        int lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            if (!isCellBlank(row.getCell(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isCellBlank(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().isBlank();
        }
        return false;
    }

    // ==================== Date Parsing Utilities ====================

    private LocalDate parseYYYYMMDD(String value) {
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(4, 6));
        int day = Integer.parseInt(value.substring(6, 8));
        return LocalDate.of(year, month, day);
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }
        return null;
    }

}
