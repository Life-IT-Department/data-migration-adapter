package lk.avengers.datamigrationadapter.repository.softlogicdb;

import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBeneficiariesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyBeneficiariesRepository extends JpaRepository<PolicyBeneficiariesEntity, Long> {
}
