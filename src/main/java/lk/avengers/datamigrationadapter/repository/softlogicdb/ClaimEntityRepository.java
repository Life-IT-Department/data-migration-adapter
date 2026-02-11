package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimEntityRepository extends JpaRepository<ClaimEntity, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DBCC CHECKIDENT ('dbo.claims', RESEED, 0)", nativeQuery = true)
    void truncate();
}
