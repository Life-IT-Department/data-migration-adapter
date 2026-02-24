package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.FundCurrentBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FundCurrentBalanceEntityRepository extends JpaRepository<FundCurrentBalanceEntity, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE fund_current_balance; DBCC CHECKIDENT ('policy', RESEED, 1)", nativeQuery = true)
    void truncate();
}
