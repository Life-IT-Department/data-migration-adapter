package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.dto.PolicyKey;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ACPPolicyEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ACPPolicyRepository extends JpaRepository<ACPPolicyEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE acp_policy RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    Optional<ACPPolicyEntity> findFirstByProductCodeAndPolicyNo(String productCode, String policyNo);

    @Query("""
    SELECT e FROM ACPPolicyEntity e
    WHERE e.productCode IN :productCodes
      AND e.policyNo IN :policyNos
""")
    List<ACPPolicyEntity> findFiltered(
            @Param("productCodes") Set<String> productCodes,
            @Param("policyNos") Set<Integer> policyNos
    );
}
