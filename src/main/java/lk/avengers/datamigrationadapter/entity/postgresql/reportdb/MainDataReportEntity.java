package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "main_data_report")
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MainDataReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_no", nullable = false)
    private Integer policyNo;
    @Column(name = "proposal_no",nullable = false)
    private String proposalNo;
    @Column(name = "product_code",nullable = false)
    private String productCode;
    @Column(name = "plan_no")
    private String planNo;
    // --- Dates & Terms ---
    private LocalDate inception;
    @Column(name = "expiry")
    private LocalDate expiry;
    @Column(name = "issue_date")
    private LocalDate issueDate;
    @Column(name = "sales_branch_code")
    private String salesBranchCode;
    @Column(name = "sales_branch_name")
    private String salesBranchName;
    @Column(name = "company_branch_code")
    private String companyBranchCode;
    @Column(name = "company_branch_name")
    private String companyBranchName;
    @Column(name = "policy_branch_code")
    private String policyBranchCode;
    @Column(name = "policy_branch_name")
    private String policyBranchName;
    private Integer term;
    @Column(name = "cy")
    private String cy;
    @Column(name = "premium_payment_term")
    private String premiumPaymentTerm;
    @Column(name = "deferment_term")
    private Integer defermentTerm;
    @Column(name = "retirement_benefit_payout_term")
    private Integer retirementBenefitPayoutTerm;
    @Column(name = "modal_premium")
    private BigDecimal modalPremium;
    private Integer frequency;
    @Column(name = "next_premium")
    private LocalDate nextPremium;
    private String status;
    @Column(name = "status_date")
    private LocalDate statusDate;
    private String reason;
    @Column(name = "agent_code")
    private String agentCode;
    private String introducer;
    private String supervisor;
    @Column(name = "ri_percentage")
    private Double riPercentage;

    // --- Main Life Details ---
    private Integer pin;
    private String title;
    @Column(name = "full_name")
    private String fullName;
    private String gender;
    private Integer dob;
    private Integer aae;
    @Column(name = "sar_choice")
    private String sarChoice;
    @Column(name = "number_of_riders_taken")
    private Integer numberOfRidersTaken;
    @Column(name = "dth_sar")
    private Integer dthSar;
    //SUB
    @Column(name = "sub_dth")
    private Integer subDth;
    @Column(name = "sub_rate_mil_dth")
    private BigDecimal subRateMilDth;
    @Column(name = "dth_occupational_loading_percent")
    private Double dthOccupationalLoadingPercent;

    @Column(name = "accd_sa")
    private Integer accdSa;
    @Column(name = "sub_accd")
    private Integer subAccd;
    @Column(name = "sub_rate_mil_accd")
    private BigDecimal subRateMilAccd;
    @Column(name = "accd_occupational_loading_percent")
    private Double accdOccupationalLoadingPercent;

    @Column(name = "accp_sa")
    private Integer accpSa;
    @Column(name = "sub_accp")
    private Integer subAccp;
    @Column(name = "sub_rate_mil_accp")
    private BigDecimal subRateMilAccp;
    @Column(name = "accp_occupational_loading_percent")
    private Double accpOccupationalLoadingPercent;

    @Column(name = "acct_sa")
    private Integer acctSa;
    @Column(name = "sub_acct")
    private Integer subAcct;
    @Column(name = "sub_rate_mil_acct")
    private BigDecimal subRateMilAcct;
    @Column(name = "acct_occupational_loading_percent")
    private Double acctOccupationalLoadingPercent;

    // CILL
    @Column(name = "cill_sa")
    private Integer cillSa;

    @Column(name = "sub_cill")
    private Integer subCill;

    @Column(name = "sub_rate_mil_cill")
    private BigDecimal subRateMilCill;

    @Column(name = "cill_occupational_loading_percentage")
    private Double cillOccupationalLoadingPercentage;

    // CILX
    @Column(name = "cilx_sa")
    private Integer cilxSa;

    @Column(name = "sub_cilx")
    private Integer subCilx;

    @Column(name = "sub_rate_mil_cilx")
    private BigDecimal subRateMilCilx;

    @Column(name = "cilx_occupational_loading_percentage")
    private Double cilxOccupationalLoadingPercentage;

    // FIB
    @Column(name = "fib_sa")
    private Integer fibSa;

    @Column(name = "sub_fib")
    private Integer subFib;

    @Column(name = "sub_rate_mil_fib")
    private BigDecimal subRateMilFib;

    @Column(name = "fib_occupational_loading_percentage")
    private Double fibOccupationalLoadingPercentage;

    // FIBT
    @Column(name = "fibt_sa")
    private Integer fibtSa;

    @Column(name = "sub_fibt")
    private Integer subFibt;

    @Column(name = "sub_rate_mil_fibt")
    private BigDecimal subRateMilFibt;

    @Column(name = "fibt_occupational_loading_percentage")
    private Double fibtOccupationalLoadingPercentage;

    // FSEB
    @Column(name = "fseb_sa")
    private Integer fsebSa;

    @Column(name = "sub_fseb")
    private Integer subFseb;

    @Column(name = "sub_rate_mil_fseb")
    private BigDecimal subRateMilFseb;

    @Column(name = "fseb_occupational_loading_percentage")
    private Double fsebOccupationalLoadingPercentage;

    // HB
    @Column(name = "hb_sa")
    private BigDecimal hbSa;

    @Column(name = "sub_hb")
    private BigDecimal subHb;

    @Column(name = "sub_rate_mil_hb")
    private BigDecimal subRateMilHb;

    @Column(name = "hb_occupational_loading_percentage")
    private Double hbOccupationalLoadingPercentage;

    // LEB
    @Column(name = "leb_sa")
    private Integer lebSa;

    @Column(name = "sub_leb")
    private Integer subLeb;

    @Column(name = "sub_rate_mil_leb")
    private BigDecimal subRateMilLeb;

    @Column(name = "leb_occupational_loading_percentage")
    private Double lebOccupationalLoadingPercentage;

    // PTD
    @Column(name = "ptd_sa")
    private Long ptdSa;

    @Column(name = "sub_ptd")
    private Integer subPtd;

    @Column(name = "sub_rate_mil_ptd")
    private BigDecimal subRateMilPtd;

    @Column(name = "ptd_occupational_loading_percentage")
    private Double ptdOccupationalLoadingPercentage;

    // TILL
    @Column(name = "till_sa")
    private Integer tillSa;

    @Column(name = "sub_till")
    private Integer subTill;

    @Column(name = "sub_rate_mil_till")
    private BigDecimal subRateMilTill;

    @Column(name = "till_occupational_loading_percentage")
    private Double tillOccupationalLoadingPercentage;

    // TR
    @Column(name = "tr_sa")
    private Long trSa;

    @Column(name = "sub_tr")
    private Integer subTr;

    @Column(name = "sub_rate_mil_tr")
    private BigDecimal subRateMilTr;

    @Column(name = "tr_occupational_loading_percentage")
    private Double trOccupationalLoadingPercentage;

    // WOPA
    @Column(name = "wopa_sa")
    private Long wopaSa;

    @Column(name = "sub_wopa")
    private Integer subWopa;

    @Column(name = "sub_rate_mil_wopa")
    private BigDecimal subRateMilWopa;

    @Column(name = "wopa_occupational_loading_percentage")
    private Double wopaOccupationalLoadingPercentage;

    // WOPC
    @Column(name = "wopc_sa")
    private Long wopcSa;

    @Column(name = "sub_wopc")
    private Integer subWopc;

    @Column(name = "sub_rate_mil_wopc")
    private BigDecimal subRateMilWopc;

    @Column(name = "wopc_occupational_loading_percentage")
    private Double wopcOccupationalLoadingPercentage;

    // WOPD
    @Column(name = "wopd_sa")
    private Long wopdSa;

    @Column(name = "sub_wopd")
    private Integer subWopd;

    @Column(name = "sub_rate_mil_wopd")
    private BigDecimal subRateMilWopd;

    @Column(name = "wopd_occupational_loading_percentage")
    private Double wopdOccupationalLoadingPercentage;

    // FSEBA
    @Column(name = "fseba_sa")
    private Long fsebaSa;

    @Column(name = "sub_fseba")
    private Integer subFseba;

    @Column(name = "sub_rate_mil_fseba")
    private BigDecimal subRateMilFseba;

    @Column(name = "fseba_occupational_loading_percentage")
    private Double fsebaOccupationalLoadingPercentage;

    // HBA
    @Column(name = "hba_sa")
    private Integer hbaSa;

    @Column(name = "sub_hba")
    private Integer subHba;

    @Column(name = "sub_rate_mil_hba")
    private BigDecimal subRateMilHba;

    @Column(name = "hba_occupational_loading_percentage")
    private Double hbaOccupationalLoadingPercentage;

    // HBAC
    @Column(name = "hbac_sa")
    private Integer hbacSa;

    @Column(name = "sub_hbac")
    private Integer subHbac;

    @Column(name = "sub_rate_mil_hbac")
    private BigDecimal subRateMilHbac;

    @Column(name = "hbac_occupational_loading_percentage")
    private Double hbacOccupationalLoadingPercentage;
    // Spouse/Child
    @Column(name = "spouse_child_pin")
    private Integer spouseChildPin;
    @Column(name = "spouse_child_title")
    private String spouseChildTitle;
    @Column(name = "spouse_child_full_name")
    private String spouseChildFullName;
    @Column(name = "spouse_child_gender")
    private String spouseChildGender;
    @Column(name = "spouse_child_dob")
    private LocalDate spouseChildDob;
    @Column(name = "spouse_child_age")
    private Integer spouseChildAge;
    @Column(name = "spouse_death_sa")
    private Integer spouseDeathSa;
    @Column(name = "spouse_sub_death")
    private Integer spouseSubDeath;
    @Column(name = "spouse_sub_rate_mil_death")
    private BigDecimal spouseSubRateMilDeath;
    @Column(name = "spouse_death_occupational_loading_percent")
    private Double spouseDeathOccupationalLoadingPercent;
    @Column(name = "spouse_child_accd_sa")
    private Integer spouseChildAccdSa;

    @Column(name = "spouse_child_sub_accd")
    private Integer spouseChildSubAccd;

    @Column(name = "spouse_child_sub_rate_mil_accd")
    private Integer spouseChildSubRateMilAccd;

    @Column(name = "spouse_child_accd_occupational_loading_percent")
    private Double spouseChildAccdOccupationalLoadingPercent;

    @Column(name = "spouse_child_accp_sa")
    private Integer spouseChildAccpSa;

    @Column(name = "spouse_child_sub_accp")
    private Integer spouseChildSubAccp;

    @Column(name = "spouse_child_sub_rate_mil_accp")
    private Integer spouseChildSubRateMilAccp;

    @Column(name = "spouse_child_accp_occupational_loading_percent")
    private Double spouseChildAccpOccupationalLoadingPercent;

    @Column(name = "spouse_child_acct_sa")
    private Integer spouseChildAcctSa;

    @Column(name = "spouse_child_sub_acct")
    private Integer spouseChildSubAcct;

    @Column(name = "spouse_child_sub_rate_mil_acct")
    private BigDecimal spouseChildSubRateMilAcct;

    @Column(name = "spouse_child_acct_occupational_loading_percent")
    private Double spouseChildAcctOccupationalLoadingPercent;

    @Column(name = "spouse_child_cill_sa")
    private Integer spouseChildCillSa;

    @Column(name = "spouse_child_sub_cill")
    private Integer spouseChildSubCill;

    @Column(name = "spouse_child_sub_rate_mil_cill")
    private BigDecimal spouseChildSubRateMilCill;

    @Column(name = "spouse_child_cill_occupational_loading_percent")
    private Double spouseChildCillOccupationalLoadingPercent;

    @Column(name = "spouse_child_cilx_sa")
    private Integer spouseChildCilxSa;

    @Column(name = "spouse_child_sub_cilx")
    private Integer spouseChildSubCilx;

    @Column(name = "spouse_child_sub_rate_mil_cilx")
    private Integer spouseChildSubRateMilCilx;

    @Column(name = "spouse_child_cilx_occupational_loading_percent")
    private Double spouseChildCilxOccupationalLoadingPercent;

    @Column(name = "spouse_child_leb_sa")
    private Integer spouseChildLebSa;

    @Column(name = "spouse_child_sub_leb")
    private Integer spouseChildSubLeb;

    @Column(name = "spouse_child_sub_rate_mil_leb")
    private BigDecimal spouseChildSubRateMilLeb;

    @Column(name = "spouse_child_leb_occupational_loading_percent")
    private Double spouseChildLebOccupationalLoadingPercent;

    @Column(name = "spouse_child_ptd_sa")
    private Integer spouseChildPtdSa;

    @Column(name = "spouse_child_sub_ptd")
    private Integer spouseChildSubPtd;

    @Column(name = "spouse_child_sub_rate_mil_ptd")
    private BigDecimal spouseChildSubRateMilPtd;

    @Column(name = "spouse_child_ptd_occupational_loading_percent")
    private Double spouseChildPtdOccupationalLoadingPercent;

    @Column(name = "spouse_child_till_sa")
    private Integer spouseChildTillSa;

    @Column(name = "spouse_child_sub_till")
    private Integer spouseChildSubTill;

    @Column(name = "spouse_child_sub_rate_mil_till")
    private BigDecimal spouseChildSubRateMilTill;

    @Column(name = "spouse_child_till_occupational_loading_percent")
    private Double spouseChildTillOccupationalLoadingPercent;

    @Column(name = "spouse_child_hb_sa")
    private Integer spouseChildHbSa;

    @Column(name = "spouse_child_sub_hb")
    private Integer spouseChildSubHb;

    @Column(name = "spouse_child_sub_rate_mil_hb")
    private BigDecimal spouseChildSubRateMilHb;

    @Column(name = "spouse_child_hb_occupational_loading_percent")
    private Double spouseChildHbOccupationalLoadingPercent;

    @Column(name = "spouse_child_ppd_sa")
    private Integer spouseChildPpdSa;

    @Column(name = "spouse_child_sub_ppd")
    private Integer spouseChildSubPpd;

    @Column(name = "spouse_child_sub_rate_mil_ppd")
    private BigDecimal spouseChildSubRateMilPpd;

    @Column(name = "spouse_child_ppd_occupational_loading_percent")
    private Double spouseChildPpdOccupationalLoadingPercent;

    @Column(name = "spouse_hba_sa")
    private Integer spouseHbaSa;

    @Column(name = "spouse_sub_hba")
    private Integer spouseSubHba;

    @Column(name = "spouse_sub_rate_mil_hba")
    private BigDecimal spouseSubRateMilHba;

    @Column(name = "spouse_hba_occupational_loading_percent")
    private Double spouseHbaOccupationalLoadingPercent;

    // Children 1..5
    @Column(name = "child1_name")
    private String child1Name;
    @Column(name = "child1_dob")
    private LocalDate child1Dob;
    @Column(name = "child1_age")
    private Integer child1Age;
    @Column(name = "child1_hbc")
    private Integer child1Hbc;
    @Column(name = "child1_hbcac")
    private Integer child1Hbcac;
    @Column(name = "child2_name")
    private String child2Name;
    @Column(name = "child2_dob")
    private LocalDate child2Dob;
    @Column(name = "child2_age")
    private Integer child2Age;
    @Column(name = "child2_hbc")
    private String child2Hbc;
    @Column(name = "child2_hbcac")
    private String child2Hbcac;
    @Column(name = "child3_name")
    private String child3Name;
    @Column(name = "child3_dob")
    private LocalDate child3Dob;
    @Column(name = "child3_age")
    private Integer child3Age;
    @Column(name = "child3_hbc")
    private Integer child3Hbc;
    @Column(name = "child3_hbcac")
    private Integer child3Hbcac;

    @Column(name = "child4_name")
    private String child4Name;

    @Column(name = "child4_dob")
    private LocalDate child4Dob;

    @Column(name = "child4_age")
    private Integer child4Age;

    @Column(name = "child4_hbc")
    private Integer child4Hbc;

    @Column(name = "child4_hbcac")
    private Integer child4Hbcac;

    @Column(name = "child5_name")
    private String child5Name;

    @Column(name = "child5_dob")
    private LocalDate child5Dob;

    @Column(name = "child5_age")
    private Integer child5Age;

    @Column(name = "child5_hbc")
    private Integer child5Hbc;
    @Column(name = "child5_hbcac")
    private Integer child5Hbcac;

    // --- Financial Summary ---
    @Column(name = "basic_sum_assured")
    private BigDecimal basicSumAssured;

    @Column(name = "interest_rate")
    private Double interestRate;

    @Column(name = "tpd_premium_life_1")
    private BigDecimal tpdPremiumLife1;

    @Column(name = "tpd_premium_life_2")
    private BigDecimal tpdPremiumLife2;

    @Column(name = "value_today")
    private BigDecimal valueToday;

    @Column(name = "prm_value_today")
    private BigDecimal prmValueToday;

    @Column(name = "bst_value_today")
    private BigDecimal bstValueToday;

    @Column(name = "transaction_amount")
    private BigDecimal transactionAmount;

    @Column(name = "interest_credited")
    private BigDecimal interestCredited;

    @Column(name = "surrender_value")
    private BigDecimal surrenderValue;

    @Column(name = "prm_surrender_value")
    private BigDecimal prmSurrenderValue;

    @Column(name = "bst_surrender_value")
    private BigDecimal bstSurrenderValue;

    @Column(name = "insurance_coverage_period")
    private Integer insuranceCoveragePeriod;

    @Column(name = "operation_date")
    private LocalDate operationDate;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "last_premium_due_date")
    private LocalDate lastPremiumDueDate;

    @Column(name = "premium_escalation_benefit_percentage")
    private Double premiumEscalationBenefitPercentage;

    @Column(name = "refund_value")
    private BigDecimal refundValue;



    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
