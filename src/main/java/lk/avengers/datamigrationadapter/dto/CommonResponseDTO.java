package lk.avengers.datamigrationadapter.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CommonResponseDTO {
    private String message;
    private String status;
    private Object data;
}