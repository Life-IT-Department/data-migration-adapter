package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "main_data_report")
@Setter
@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
public class MainDataReportEntity extends BaseMainData {
    @Column(name = "deferment_term")
    private Integer defermentTerm;
    @Column(name = "retirement_benefit_payout_term")
    private Integer retirementBenefitPayoutTerm;
    @Column(name = "status_date")
    private LocalDate statusDate;
    @Column(name = "accd_sa")
    private BigDecimal accdSa;
    @Column(name = "sub_accd")
    private BigDecimal subAccd;
    @Column(name = "sub_rate_mil_accd")
    private BigDecimal subRateMilAccd;
    @Column(name = "accd_occupational_loading_percent")
    private Double accdOccupationalLoadingPercent;

    @Column(name = "accp_sa")
    private BigDecimal accpSa;
    @Column(name = "sub_accp")
    private BigDecimal subAccp;
    @Column(name = "sub_rate_mil_accp")
    private BigDecimal subRateMilAccp;
    @Column(name = "accp_occupational_loading_percent")
    private Double accpOccupationalLoadingPercent;

    @Column(name = "acct_sa")
    private BigDecimal acctSa;
    @Column(name = "sub_acct")
    private BigDecimal subAcct;
    @Column(name = "sub_rate_mil_acct")
    private BigDecimal subRateMilAcct;
    @Column(name = "acct_occupational_loading_percent")
    private Double acctOccupationalLoadingPercent;

    // CILL
    @Column(name = "cill_sa")
    private BigDecimal cillSa;
    @Column(name = "sub_cill")
    private BigDecimal subCill;
    @Column(name = "sub_rate_mil_cill")
    private BigDecimal subRateMilCill;
    @Column(name = "cill_occupational_loading_percentage")
    private Double cillOccupationalLoadingPercentage;
    // CILX
    @Column(name = "cilx_sa")
    private BigDecimal cilxSa;
    @Column(name = "sub_cilx")
    private BigDecimal subCilx;
    @Column(name = "sub_rate_mil_cilx")
    private BigDecimal subRateMilCilx;
    @Column(name = "cilx_occupational_loading_percentage")
    private Double cilxOccupationalLoadingPercentage;
    // FIB
    @Column(name = "fib_sa")
    private BigDecimal fibSa;
    @Column(name = "sub_fib")
    private BigDecimal subFib;
    @Column(name = "sub_rate_mil_fib")
    private BigDecimal subRateMilFib;
    @Column(name = "fib_occupational_loading_percentage")
    private Double fibOccupationalLoadingPercentage;
    // FIBT
    @Column(name = "fibt_sa")
    private BigDecimal fibtSa;
    @Column(name = "sub_fibt")
    private BigDecimal subFibt;
    @Column(name = "sub_rate_mil_fibt")
    private BigDecimal subRateMilFibt;
    @Column(name = "fibt_occupational_loading_percentage")
    private Double fibtOccupationalLoadingPercentage;
    // FSEB
    @Column(name = "fseb_sa")
    private BigDecimal fsebSa;
    @Column(name = "sub_fseb")
    private BigDecimal subFseb;
    @Column(name = "sub_rate_mil_fseb")
    private BigDecimal subRateMilFseb;
    @Column(name = "fseb_occupational_loading_percentage")
    private Double fsebOccupationalLoadingPercentage;
    // LEB
    @Column(name = "leb_sa")
    private BigDecimal lebSa;
    @Column(name = "sub_leb")
    private BigDecimal subLeb;
    @Column(name = "sub_rate_mil_leb")
    private BigDecimal subRateMilLeb;
    @Column(name = "leb_occupational_loading_percentage")
    private Double lebOccupationalLoadingPercentage;
    // PTD
    @Column(name = "ptd_sa")
    private BigDecimal ptdSa;
    @Column(name = "sub_ptd")
    private BigDecimal subPtd;
    @Column(name = "sub_rate_mil_ptd")
    private BigDecimal subRateMilPtd;
    @Column(name = "ptd_occupational_loading_percentage")
    private Double ptdOccupationalLoadingPercentage;
    // TILL
    @Column(name = "till_sa")
    private BigDecimal tillSa;
    @Column(name = "sub_till")
    private BigDecimal subTill;
    @Column(name = "sub_rate_mil_till")
    private BigDecimal subRateMilTill;
    @Column(name = "till_occupational_loading_percentage")
    private Double tillOccupationalLoadingPercentage;
    // TR
    @Column(name = "tr_sa")
    private BigDecimal trSa;
    @Column(name = "sub_tr")
    private BigDecimal subTr;
    @Column(name = "sub_rate_mil_tr")
    private BigDecimal subRateMilTr;
    @Column(name = "tr_occupational_loading_percentage")
    private Double trOccupationalLoadingPercentage;
    // WOPA
    @Column(name = "wopa_sa")
    private BigDecimal wopaSa;
    @Column(name = "sub_wopa")
    private BigDecimal subWopa;
    @Column(name = "sub_rate_mil_wopa")
    private BigDecimal subRateMilWopa;
    @Column(name = "wopa_occupational_loading_percentage")
    private Double wopaOccupationalLoadingPercentage;
    // WOPC
    @Column(name = "wopc_sa")
    private BigDecimal wopcSa;
    @Column(name = "sub_wopc")
    private BigDecimal subWopc;
    @Column(name = "sub_rate_mil_wopc")
    private BigDecimal subRateMilWopc;
    @Column(name = "wopc_occupational_loading_percentage")
    private Double wopcOccupationalLoadingPercentage;
    // WOPD
    @Column(name = "wopd_sa")
    private BigDecimal wopdSa;
    @Column(name = "sub_wopd")
    private BigDecimal subWopd;
    @Column(name = "sub_rate_mil_wopd")
    private BigDecimal subRateMilWopd;
    @Column(name = "wopd_occupational_loading_percentage")
    private Double wopdOccupationalLoadingPercentage;
    // FSEBA
    @Column(name = "fseba_sa")
    private BigDecimal fsebaSa;
    @Column(name = "sub_fseba")
    private BigDecimal subFseba;
    @Column(name = "sub_rate_mil_fseba")
    private BigDecimal subRateMilFseba;
    @Column(name = "fseba_occupational_loading_percentage")
    private Double fsebaOccupationalLoadingPercentage;
    // HBA
    @Column(name = "hba_sa")
    private BigDecimal hbaSa;
    @Column(name = "sub_hba")
    private BigDecimal subHba;
    @Column(name = "sub_rate_mil_hba")
    private BigDecimal subRateMilHba;
    @Column(name = "hba_occupational_loading_percentage")
    private Double hbaOccupationalLoadingPercentage;
    // HBAC
    @Column(name = "hbac_sa")
    private BigDecimal hbacSa;
    @Column(name = "sub_hbac")
    private BigDecimal subHbac;
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
    private BigDecimal spouseDeathSa;
    @Column(name = "spouse_sub_death")
    private BigDecimal spouseSubDeath;
    @Column(name = "spouse_sub_rate_mil_death")
    private BigDecimal spouseSubRateMilDeath;
    @Column(name = "spouse_death_occupational_loading_percent")
    private Double spouseDeathOccupationalLoadingPercent;
    @Column(name = "spouse_child_accd_sa")
    private BigDecimal spouseChildAccdSa;
    @Column(name = "spouse_child_sub_accd")
    private BigDecimal spouseChildSubAccd;
    @Column(name = "spouse_child_sub_rate_mil_accd")
    private BigDecimal spouseChildSubRateMilAccd;
    @Column(name = "spouse_child_accd_occupational_loading_percent")
    private Double spouseChildAccdOccupationalLoadingPercent;
    @Column(name = "spouse_child_accp_sa")
    private BigDecimal spouseChildAccpSa;
    @Column(name = "spouse_child_sub_accp")
    private BigDecimal spouseChildSubAccp;
    @Column(name = "spouse_child_sub_rate_mil_accp")
    private BigDecimal spouseChildSubRateMilAccp;
    @Column(name = "spouse_child_accp_occupational_loading_percent")
    private Double spouseChildAccpOccupationalLoadingPercent;
    @Column(name = "spouse_child_acct_sa")
    private BigDecimal spouseChildAcctSa;
    @Column(name = "spouse_child_sub_acct")
    private BigDecimal spouseChildSubAcct;
    @Column(name = "spouse_child_sub_rate_mil_acct")
    private BigDecimal spouseChildSubRateMilAcct;
    @Column(name = "spouse_child_acct_occupational_loading_percent")
    private Double spouseChildAcctOccupationalLoadingPercent;
    @Column(name = "spouse_child_cill_sa")
    private BigDecimal spouseChildCillSa;
    @Column(name = "spouse_child_sub_cill")
    private BigDecimal spouseChildSubCill;
    @Column(name = "spouse_child_sub_rate_mil_cill")
    private BigDecimal spouseChildSubRateMilCill;
    @Column(name = "spouse_child_cill_occupational_loading_percent")
    private Double spouseChildCillOccupationalLoadingPercent;
    @Column(name = "spouse_child_cilx_sa")
    private BigDecimal spouseChildCilxSa;
    @Column(name = "spouse_child_sub_cilx")
    private BigDecimal spouseChildSubCilx;
    @Column(name = "spouse_child_sub_rate_mil_cilx")
    private BigDecimal spouseChildSubRateMilCilx;
    @Column(name = "spouse_child_cilx_occupational_loading_percent")
    private Double spouseChildCilxOccupationalLoadingPercent;
    @Column(name = "spouse_child_leb_sa")
    private BigDecimal spouseChildLebSa;
    @Column(name = "spouse_child_sub_leb")
    private BigDecimal spouseChildSubLeb;
    @Column(name = "spouse_child_sub_rate_mil_leb")
    private BigDecimal spouseChildSubRateMilLeb;
    @Column(name = "spouse_child_leb_occupational_loading_percent")
    private Double spouseChildLebOccupationalLoadingPercent;
    @Column(name = "spouse_child_ptd_sa")
    private BigDecimal spouseChildPtdSa;
    @Column(name = "spouse_child_sub_ptd")
    private BigDecimal spouseChildSubPtd;
    @Column(name = "spouse_child_sub_rate_mil_ptd")
    private BigDecimal spouseChildSubRateMilPtd;
    @Column(name = "spouse_child_ptd_occupational_loading_percent")
    private Double spouseChildPtdOccupationalLoadingPercent;
    @Column(name = "spouse_child_till_sa")
    private BigDecimal spouseChildTillSa;
    @Column(name = "spouse_child_sub_till")
    private BigDecimal spouseChildSubTill;
    @Column(name = "spouse_child_sub_rate_mil_till")
    private BigDecimal spouseChildSubRateMilTill;
    @Column(name = "spouse_child_till_occupational_loading_percent")
    private Double spouseChildTillOccupationalLoadingPercent;
    @Column(name = "spouse_child_hb_sa")
    private BigDecimal spouseChildHbSa;
    @Column(name = "spouse_child_sub_hb")
    private BigDecimal spouseChildSubHb;
    @Column(name = "spouse_child_sub_rate_mil_hb")
    private BigDecimal spouseChildSubRateMilHb;
    @Column(name = "spouse_child_hb_occupational_loading_percent")
    private Double spouseChildHbOccupationalLoadingPercent;
    @Column(name = "spouse_child_ppd_sa")
    private BigDecimal spouseChildPpdSa;
    @Column(name = "spouse_child_sub_ppd")
    private BigDecimal spouseChildSubPpd;
    @Column(name = "spouse_child_sub_rate_mil_ppd")
    private BigDecimal spouseChildSubRateMilPpd;
    @Column(name = "spouse_child_ppd_occupational_loading_percent")
    private Double spouseChildPpdOccupationalLoadingPercent;
    @Column(name = "spouse_hba_sa")
    private BigDecimal spouseHbaSa;
    @Column(name = "spouse_sub_hba")
    private BigDecimal spouseSubHba;
    @Column(name = "spouse_sub_rate_mil_hba")
    private BigDecimal spouseSubRateMilHba;
    @Column(name = "spouse_hba_occupational_loading_percent")
    private Double spouseHbaOccupationalLoadingPercent;
    // Children
    @Column(name = "child1_hbcac")
    private Integer child1Hbcac;
    @Column(name = "child2_hbcac")
    private String child2Hbcac;
    @Column(name = "child3_hbcac")
    private Integer child3Hbcac;
    @Column(name = "child4_hbcac")
    private Integer child4Hbcac;
    @Column(name = "child5_hbcac")
    private Integer child5Hbcac;
    // --- Financial Summary ---
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
    @Column(name = "premium_payment_term")
    private String premiumPaymentTerm;
    @Column(name = "premium_escalation_benefit_percentage")
    private Double premiumEscalationBenefitPercentage;
    // -- audit --
    @CreatedDate
    @Column(name = "sys_date")
    private LocalDateTime sysDate;
}
