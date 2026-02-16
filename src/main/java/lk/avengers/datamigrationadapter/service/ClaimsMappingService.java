package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;

public interface ClaimsMappingService {
    @SneakyThrows
    ResponseEntity<CommonResponseDTO> mapClaimsData();
}
