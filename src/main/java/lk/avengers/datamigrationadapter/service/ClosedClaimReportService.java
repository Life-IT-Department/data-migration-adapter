package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import org.springframework.http.ResponseEntity;

public interface ClosedClaimReportService {
    ResponseEntity<CommonResponseDTO> uploadClosedClaimsReport();
}
