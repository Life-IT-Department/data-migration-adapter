package lk.avengers.datamigrationadapter.service;


import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;

public interface ExcelDataExtractorService {

    ExcelDataResponseDTO extractExcelFileFromPath(ExcelExtractorRequestDTO requestDTO);

    ExcelDataResponseDTO extractExcelFile(ExcelExtractorRequestDTO requestDTO);
}

