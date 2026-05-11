package lk.avengers.datamigrationadapter.repository.softlogicdb;

import lk.avengers.datamigrationadapter.entity.softlogicdb.PremiumExtraFields;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PremiumsExtraRepository extends JpaRepository<PremiumExtraFields, Integer> {
}
