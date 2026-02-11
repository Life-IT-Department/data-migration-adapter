package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import org.springframework.transaction.annotation.Transactional;

public interface PolicyBenefitMappingService {
    CommonResponseDTO processBenefitCodeMapping();
}
