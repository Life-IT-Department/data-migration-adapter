package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "benefit_code_mapper")
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BenefitCodeMapperEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "softlogic_benefit_code", nullable = false )
    private String softlogicBenefitCode;
    @Column(name = "allianz_benefit_code", nullable = false, unique = true)
    private String allianzBenefitCode;
}
