package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.UnitLinkReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrUnitLink;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.UnitLinkRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrUnitLinkRepository;
import lk.avengers.datamigrationadapter.service.UnitLinkMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitLinkMapperServiceImpl implements UnitLinkMapperService {

    private final UnitLinkRepository unitLinkRepository;
    private final MigrUnitLinkRepository migrUnitLinkRepository;

    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public ResponseEntity<CommonResponseDTO> processUnitLinkMapping() {
        List<UnitLinkReportEntity> unitLinkReportEntities = unitLinkRepository.findAll();
        List<MigrUnitLink> migrUnitLinkList = new ArrayList<>();

        unitLinkReportEntities.forEach(unitLinkReportEntity -> migrUnitLinkList.add(unitLinkReportEntity.mapData(MigrUnitLink.class)));
        migrUnitLinkList.forEach(migrUnitLink -> migrUnitLink.setId(null));

        log.info("Truncating unit link table");
        migrUnitLinkRepository.truncate();
        saveInBatches(migrUnitLinkList, migrUnitLinkRepository);

        return ResponseEntity.ok(CommonResponseDTO.builder()
                .message(String.format("%d unit link records were saved",
                                migrUnitLinkList.size()))
                .status(HttpStatus.OK.toString())
                .build());
    }


    private <T> void saveInBatches(List<T> list, JpaRepository<T, ?> repository) {
        int batchSize = 1000;
        int totalSize = list.size();
        int totalBatches = (int) Math.ceil((double) totalSize / batchSize);

        log.info("Starting batch save: totalRecords={}, batchSize={}, totalBatches={}, repository={}",
                totalSize, batchSize, totalBatches, repository.getClass().getSimpleName());

        for (int i = 0; i < list.size(); i += 1000) {
            int batchNumber = (i / batchSize) + 1;
            int end = Math.min(i + 1000, list.size());

            log.info("Saving batch {}/{} (records {} - {})",
                    batchNumber, totalBatches, i + 1, end);

            List<T> batch = list.subList(i, end);
            repository.saveAll(batch);
            repository.flush();
        }
        log.info("Completed batch save: totalRecords={}, repository={}",
                totalSize, repository.getClass().getSimpleName());
    }

}