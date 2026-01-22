package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BatchProcessService {
    @Transactional(
            transactionManager = "reportPlatformTransactionManager",
            propagation = Propagation.REQUIRES_NEW
    )
    void saveBatch(List<MainDataReportEntity> batch);
}
