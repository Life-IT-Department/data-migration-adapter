package lk.avengers.datamigrationadapter.dto.excel;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExcelExtractorRequestDTO {
    private MultipartFile file;
    private String filePath; // Only when reading the file using a path.
    private Integer sheetIndex; // Put which sheet number should select to process (0-based indexing)
    private Integer headerRow; // Put which row to select as the header row (0-based indexing)
    private Integer dataRow; // put from which row to start extracting the data (0-based indexing)
}
