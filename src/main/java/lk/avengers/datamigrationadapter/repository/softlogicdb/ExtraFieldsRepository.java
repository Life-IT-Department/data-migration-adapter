package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ExtraFields;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtraFieldsRepository extends JpaRepository<ExtraFields, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE extra_fields; DBCC CHECKIDENT ('dbo.extra_fields', RESEED, 1)", nativeQuery = true)
    void truncate();
}
