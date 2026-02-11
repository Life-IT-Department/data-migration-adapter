package lk.avengers.datamigrationadapter.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RiderCoverColumnDTO {
    private String coverName;
    private String coverSubRate;
    private String coverPerMilRate;
    private String coverOccupationExtraRate;
}
