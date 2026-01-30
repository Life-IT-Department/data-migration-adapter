package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.service.CommonFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
