package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "policy_benefits")
public class PolicyBenefitsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PB_PolicyNo", length = 30)
    private String pbPolicyNo;

    @Column(name = "PB_BenefitCode", length = 4)
    private String pbBenefitCode;

    @Column(name = "PB_Coverage", precision = 18, scale = 2)
    private BigDecimal pbCoverage;

    @Column(name = "PB_PremPortion", precision = 18, scale = 2)
    private BigDecimal pbPremPortion;

    @Column(name = "PB_Option", length = 20)
    private String pbOption;

    @Column(name = "PB_OccuExtra", precision = 9, scale = 2)
    private BigDecimal pbOccuExtra;

    @Column(name = "PB_ExtraMortalityRate", precision = 9, scale = 2)
    private BigDecimal pbExtraMortalityRate;

    @Column(name = "PB_InclusionDate")
    private LocalDate pbInclusionDate;

    @Column(name = "PB_expireddate")
    private LocalDate pbExpiredDate;

    @Column(name = "PB_Term")
    private Integer pbTerm;

    @Column(name = "PB_ExtraPremium", precision = 9, scale = 2)
    private BigDecimal pbExtraPremium;
}