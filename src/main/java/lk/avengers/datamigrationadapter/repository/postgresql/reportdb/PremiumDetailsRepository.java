package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.dto.PolicyKey;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PremiumDetailsRepository extends JpaRepository<PremiumDetailsEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE premium_details RESTART IDENTITY", nativeQuery = true)
    void truncate();

    Optional<PremiumDetailsEntity> findFirstByPolicyNoAndProductCodeOrderByIdDesc(Integer policyNo, String productCode);
    List<PremiumDetailsEntity> findByPolicyNoAndProductCode(Integer policyNo, String productCode);

    @Query("""
    SELECT e FROM PremiumDetailsEntity e
    WHERE e.productCode IN :productCodes
      AND e.policyNo IN :policyNos
""")
    List<PremiumDetailsEntity> findFiltered(
            @Param("productCodes") Set<String> productCodes,
            @Param("policyNos") Set<Integer> policyNos
    );
}
