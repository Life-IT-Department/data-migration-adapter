package lk.avengers.datamigrationadapter.dto.excel;

import lombok.*;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelDataResponseDTO {

    private List<String> headers;
    private List<Map<String, Object>> extractedData;
}
