package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import jakarta.transaction.Transactional;
import lk.avengers.datamigrationadapter.dto.PolicyKey;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ContactDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ContactDetailRepository extends JpaRepository<ContactDetailEntity,Long> {
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE contact_detail RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    Optional<ContactDetailEntity> findFirstByProductAndPolicyNo(String product, String policyNo);

    @Query("""
    SELECT e FROM ContactDetailEntity e
    WHERE e.product IN :productCodes
      AND e.policyNo IN :policyNos
""")
    List<ContactDetailEntity> findFiltered(
            @Param("productCodes") Set<String> productCodes,
            @Param("policyNos") Set<Integer> policyNos
    );
}
