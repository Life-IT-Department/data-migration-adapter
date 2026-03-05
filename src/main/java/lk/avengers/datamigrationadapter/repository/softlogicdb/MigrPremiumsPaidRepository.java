package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsPaid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrPremiumsPaidRepository extends JpaRepository<MigrPremiumsPaid, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dbo.Migr_PremiumsPaid; DBCC CHECKIDENT ('policy', RESEED, 1)", nativeQuery = true)
    void truncate();
}
