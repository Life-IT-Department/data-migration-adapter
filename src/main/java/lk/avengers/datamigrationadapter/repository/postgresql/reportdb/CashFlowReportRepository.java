package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.CashFlowReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CashFlowReportRepository extends JpaRepository<CashFlowReportEntity, Long> {

    @Query("SELECT c FROM CashFlowReportEntity c WHERE c.details LIKE %:keyword%")
    List<CashFlowReportEntity> findByDetailsContaining(@Param("keyword") String keyword);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE cash_flow RESTART IDENTITY", nativeQuery = true)
    void truncate();
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM cash_flow WHERE year = :year", nativeQuery = true)
    void deleteAllByYear(@Param("year") int year);
}
