package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ClosedClaimReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ClosedClaimsReportRepository extends JpaRepository<ClosedClaimReportEntity, Integer> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE closed_claim RESTART IDENTITY", nativeQuery = true)
    void truncate();

    List<ClosedClaimReportEntity> findByPolicyNoIn(List<String> policyNoList);
}
