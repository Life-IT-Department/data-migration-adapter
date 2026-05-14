package lk.avengers.datamigrationadapter.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ChildDto {
    private String name;
    private LocalDate dob;
    private Integer age;
    private String childHbc;
    private BigDecimal childInpSar;
    private String gender;

    // Remove when running actual list
    private Integer childCode;
    private BigDecimal basicSumAssured;
    private BigDecimal hbcSa;
    private BigDecimal inpcSa;
    private BigDecimal bonus;

}
