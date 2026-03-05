package lk.avengers.datamigrationadapter.repository.postgresql.reportdb;

import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BankPinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankPinRepository extends JpaRepository<BankPinEntity, Integer> {
}
