package lk.avengers.datamigrationadapter.repository.softlogicdb;

import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBenefitsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyBeneficiariesEntityRepository extends JpaRepository<PolicyBenefitsEntity, Long> {
}
