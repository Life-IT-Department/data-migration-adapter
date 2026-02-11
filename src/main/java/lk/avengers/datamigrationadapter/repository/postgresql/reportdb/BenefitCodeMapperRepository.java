package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BenefitCodeMapperEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenefitCodeMapperRepository extends JpaRepository<BenefitCodeMapperEntity, Integer> {
}
