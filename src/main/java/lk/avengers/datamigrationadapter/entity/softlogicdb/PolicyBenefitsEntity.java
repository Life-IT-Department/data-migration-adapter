package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policy_benefits")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PolicyBenefitsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", length = 50, nullable = false)
    private String policyNo;

    @Column(name = "benefit_code", length = 50)
    private String benefitCode;

    @Column(name = "coverage", precision = 18, scale = 2)
    private BigDecimal coverage;

    @Column(name = "prem_portion", precision = 18, scale = 2)
    private BigDecimal premPortion;

    @Column(name = "[option]", length = 50)
    private String option;

    @Column(name = "occu_extra", precision = 18, scale = 2)
    private BigDecimal occuExtra;

    @Column(name = "extra_mortality_rate", precision = 18, scale = 2)
    private BigDecimal extraMortalityRate;

    @Column(name = "inclusion_date")
    private LocalDate inclusionDate;

    @Column(name = "expireddate")
    private LocalDate expiredDate;

    @Column(name = "term")
    private Integer term;

    @Column(name = "extra_premium", precision = 18, scale = 2)
    private BigDecimal extraPremium;
}
