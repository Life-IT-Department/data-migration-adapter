package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrUnitLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrUnitLinkRepository extends JpaRepository<MigrUnitLink, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dbo.Migr_UnitLink", nativeQuery = true)
    void truncate();
}
