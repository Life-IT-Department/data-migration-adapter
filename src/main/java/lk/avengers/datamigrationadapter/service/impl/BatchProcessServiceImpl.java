package lk.avengers.datamigrationadapter.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import lk.avengers.datamigrationadapter.service.BatchProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BatchProcessServiceImpl implements BatchProcessService {

    private static final int BATCH_SIZE = 1000;

    @PersistenceContext(unitName = "reportEntityManagerFactory")
    private EntityManager entityManager;

    @Transactional(
            transactionManager = "reportPlatformTransactionManager",
            propagation = Propagation.REQUIRES_NEW
    )
    @Override
    public void saveMainDataBatch(List<MainDataReportEntity> batch) {

        if (batch == null || batch.isEmpty()) {
            log.debug("ReportBatchService.saveBatch called with empty batch");
            return;
        }

        log.info("ReportBatchService.saveBatch started. Batch size: {}", batch.size());

        for (int i = 0; i < batch.size(); i++) {
            entityManager.persist(batch.get(i));

            if ((i + 1) % BATCH_SIZE == 0) {
                flushAndClear();
            }
        }

        flushAndClear();

        log.info("ReportBatchService.saveBatch completed");
    }
    @Transactional(
            transactionManager = "reportPlatformTransactionManager",
            propagation = Propagation.REQUIRES_NEW
    )
    @Override
    public void saveALHBatch(List<MainDataALHReportEntity> batch) {
        if (batch == null || batch.isEmpty()) {
            log.debug("ReportBatchService.saveALHBatch called with empty batch");
            return;
        }
        log.info("ReportBatchService.saveALHBatch started. Batch size: {}", batch.size());
        for (int i = 0; i < batch.size(); i++) {
            entityManager.persist(batch.get(i));
            if ((i + 1) % BATCH_SIZE == 0) {
                flushAndClear();
            }
        }
        flushAndClear();
        log.info("ReportBatchService.saveALHBatch completed");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
    @Transactional(
            transactionManager = "reportPlatformTransactionManager",
            propagation = Propagation.REQUIRES_NEW
    )
    @Override
    public void saveCashFlowBatch(List<CashFlowReportEntity> batch) {

        if (batch == null || batch.isEmpty()) {
            log.debug("ReportBatchService.saveCashFlowBatch called with empty batch");
            return;
        }
        log.info("ReportBatchService.saveCashFlowBatch started. Batch size: {}", batch.size());
        for (int i = 0; i < batch.size(); i++) {
            entityManager.persist(batch.get(i));

            if ((i + 1) % BATCH_SIZE == 0) {
                flushAndClear();
            }
        }
        flushAndClear();
        log.info("ReportBatchService.saveCashFlowBatch completed");
    }

    @Transactional(
            transactionManager = "reportPlatformTransactionManager",
            propagation = Propagation.REQUIRES_NEW
    )
    @Override
    public void savePremiumDetailsBatch(List<PremiumDetailsEntity> batch) {
        if (batch == null || batch.isEmpty()) {
            log.debug("ReportBatchService.savePremiumDetailsBatch called with empty batch");
            return;
        }
        log.info("ReportBatchService.savePremiumDetailsBatch started. Batch size: {}", batch.size());
        for (int i = 0; i < batch.size(); i++) {
            entityManager.persist(batch.get(i));

            if ((i + 1) % BATCH_SIZE == 0) {
                flushAndClear();
            }
        }
        flushAndClear();
        log.info("ReportBatchService.savePremiumDetailsBatch completed");
    }

}