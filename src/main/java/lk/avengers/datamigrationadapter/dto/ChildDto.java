package lk.avengers.datamigrationadapter.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ChildDto {
    private String name;
    private LocalDate dob;
    private Integer age;
    private String childHbc;

}
