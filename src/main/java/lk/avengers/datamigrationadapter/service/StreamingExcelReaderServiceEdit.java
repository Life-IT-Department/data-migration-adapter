package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;

public interface StreamingExcelReaderServiceEdit {
    ExcelDataResponseDTO readExcelStreaming(String filePath,
                                            int sheetIndex,
                                            int headerRowIndex,
                                            int dataStartRowIndex);
}
