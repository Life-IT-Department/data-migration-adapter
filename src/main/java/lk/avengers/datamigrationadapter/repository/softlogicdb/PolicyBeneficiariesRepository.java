package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBeneficiariesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyBeneficiariesRepository extends JpaRepository<PolicyBeneficiariesEntity, Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dbo.Migr_PolicyBeneficieries", nativeQuery = true)
    void truncate();
}
