package lk.avengers.datamigrationadapter.repository.softlogicdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MigrPolicyRepository extends JpaRepository<MigrPolicyData, Integer> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE Migr_PolicyData", nativeQuery = true)
    void truncateTable();

    List<MigrPolicyData> findByLaPolicyNoIn(List<String> policyNo);
}
