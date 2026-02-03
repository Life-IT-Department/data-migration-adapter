package lk.avengers.datamigrationadapter.service;

import org.apache.poi.ss.usermodel.Cell;

import java.time.LocalDate;
import java.time.LocalTime;

public interface CommonFunction {
    LocalDate getDateFromInteger(String date);

    // -------------------------------------------------------------------------
    // Cell Helpers
    // -------------------------------------------------------------------------
    Integer getIntegerValue(Cell cell);

    Long getLongValue(Cell cell);

    Double getDoubleValue(Cell cell);

    String getStringValue(Cell cell);

    java.math.BigDecimal getBigDecimalValue(Cell cell);

    String getStringDateValue(Cell cell);
    
    LocalTime getLocalTimeValue(Cell cell);
}
