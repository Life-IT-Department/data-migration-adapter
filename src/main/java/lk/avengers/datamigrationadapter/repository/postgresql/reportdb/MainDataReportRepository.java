package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface MainDataReportRepository
        extends JpaRepository<MainDataReportEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE main_data_report RESTART IDENTITY", nativeQuery = true)
    void truncate();

   Optional<MainDataReportEntity> findFirstByProductCodeAndPolicyNo(String productCode, int policyNo);
}
