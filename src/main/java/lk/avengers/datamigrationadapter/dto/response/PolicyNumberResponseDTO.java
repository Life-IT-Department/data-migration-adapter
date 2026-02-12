package lk.avengers.datamigrationadapter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNumberResponseDTO {
    private String productCode;
    private Integer policyNo;
}
