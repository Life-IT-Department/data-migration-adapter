package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.PolicyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyListRepository extends JpaRepository<PolicyListEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE policy_list RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();
}
