package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "branch_mapping")
public class BranchCodeMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "allianzbranchcode", nullable = false)
    private Integer allianzbranchcode;

    @Column(name = "slbranchcode", nullable = false, length = 50)
    private String slbranchcode;

    @Column(name = "branchname", nullable = false)
    private String branchname;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_date")
    private Instant createdDate;


}