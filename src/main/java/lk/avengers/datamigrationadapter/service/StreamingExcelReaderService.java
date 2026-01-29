package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;

public abstract class StreamingExcelReaderService {
    public abstract ExcelDataResponseDTO readExcelStreaming(String filePath,
                                                            int sheetIndex,
                                                            int headerRowIndex,
                                                            int dataStartRowIndex);
}
