package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PremiumDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PremiumDetailsRepository extends JpaRepository<PremiumDetailsEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "TRUNCATE TABLE premium_details RESTART IDENTITY", nativeQuery = true)
    void truncate();

    Optional<PremiumDetailsEntity> findFirstByPolicyNoAndProductCodeOrderByIdDesc(Integer policyNo, String productCode);
    List<PremiumDetailsEntity> findByPolicyNoAndProductCode(Integer policyNo, String productCode);
}
