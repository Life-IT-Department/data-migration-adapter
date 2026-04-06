package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ContactDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactDetailRepository extends JpaRepository<ContactDetailEntity,Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE contact_detail RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    Optional<ContactDetailEntity> findFirstByProductAndPolicyNo(String product, String policyNo);
}
