package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyBenefitsEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBenefitsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyBenefitsEntityRepository extends JpaRepository<MigrPolicyBenefitsEntity, Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dbo.Migr_PolicyBenefits", nativeQuery = true)
    void truncate();
}
