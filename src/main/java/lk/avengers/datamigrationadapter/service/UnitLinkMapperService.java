package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

public interface UnitLinkMapperService {
    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    ResponseEntity<CommonResponseDTO> processUnitLinkMapping();
}
