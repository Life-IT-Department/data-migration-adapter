package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "extra_fields")
public class ExtraFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "policy_no", length = 50)
    private String policyNo;

    @Column(name = "proposal_no", length = 50)
    private String proposalNo;

    @Column(name = "company_branch_code", length = 50)
    private String companyBranchCode;

    @Column(name = "company_branch_name", length = 100)
    private String companyBranchName;

    @Column(name = "policy_branch_code", length = 50)
    private String policyBranchCode;

    @Column(name = "policy_branch_name", length = 100)
    private String policyBranchName;

    @Column(name = "interest_rate", precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tpd_premium_life_1", precision = 18, scale = 2)
    private BigDecimal tpdPremiumLife1;

    @Column(name = "tpd_premium_life_2", precision = 18, scale = 2)
    private BigDecimal tpdPremiumLife2;

    @Column(name = "interest_credited", precision = 18, scale = 2)
    private BigDecimal interestCredited;

    @Column(name = "surrender_value", precision = 18, scale = 2)
    private BigDecimal surrenderValue;

    @Column(name = "prm_surrender_value", precision = 18, scale = 2)
    private BigDecimal prmSurrenderValue;

    @Column(name = "bst_surrender_value", precision = 18, scale = 2)
    private BigDecimal bstSurrenderValue;

    @Column(name = "insurance_coverage_period")
    private Integer insuranceCoveragePeriod;

    @Column(name = "total_premium_allocation", precision = 18, scale = 2)
    private BigDecimal totalPremiumAllocation;

    @Column(name = "bank_name")
    private String bankName;


}
