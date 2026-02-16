package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ACPPolicyEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ACPPolicyRepository extends JpaRepository<ACPPolicyEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE acp_policy RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    Optional<ACPPolicyEntity> findFirstByProductCodeAndPolicyNo(String productCode, String policyNo);
}
