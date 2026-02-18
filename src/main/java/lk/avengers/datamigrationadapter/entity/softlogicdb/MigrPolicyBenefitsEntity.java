package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Migr_PolicyBenefits")
public class MigrPolicyBenefitsEntity {

    @EmbeddedId
    private MigrPolicyBenefitsID id;

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

    @Column(name = "PBUpload")
    private Boolean pbUpload;
}
