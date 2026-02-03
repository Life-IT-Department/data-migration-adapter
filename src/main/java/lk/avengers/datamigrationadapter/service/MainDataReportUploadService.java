package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import org.springframework.http.ResponseEntity;

public interface MainDataReportUploadService {

    /* ============================================================
           PUBLIC METHODS
           ============================================================ */
    ResponseEntity<CommonResponseDTO> uploadMainDataReports();

    ResponseEntity<CommonResponseDTO> uploadMainDataALHReportExcel();
}
