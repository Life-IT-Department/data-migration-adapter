package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.DeclaredClaimEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PaidClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeclaredClaimReportRepository extends JpaRepository<DeclaredClaimEntity, Integer> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE declared_claim RESTART IDENTITY", nativeQuery = true)
    void truncate();

    List<DeclaredClaimEntity> findByPolicyNo(String policyNo);
}
