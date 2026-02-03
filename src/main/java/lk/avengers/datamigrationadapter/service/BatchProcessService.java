package lk.avengers.datamigrationadapter.service;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BatchProcessService {

    void saveMainDataBatch(List<MainDataReportEntity> batch);

    void saveALHBatch(List<MainDataALHReportEntity> batch);

    void saveCashFlowBatch(List<CashFlowReportEntity> batch);
}
