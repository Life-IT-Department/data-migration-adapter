package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPremiumsDue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrPremiumsDueRepository extends JpaRepository<MigrPremiumsDue, Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dbo.Migr_PremiumsDue; DBCC CHECKIDENT ('policy', RESEED, 1)", nativeQuery = true)
    void truncate();
}
