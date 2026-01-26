package lk.avengers.datamigrationadapter.service;


import lk.avengers.datamigrationadapter.dto.excel.ExcelDataResponseDTO;
import lk.avengers.datamigrationadapter.dto.excel.ExcelExtractorRequestDTO;

public interface ExcelDataExtractorService {

    ExcelDataResponseDTO extractExcelFile(ExcelExtractorRequestDTO requestDTO);
}

