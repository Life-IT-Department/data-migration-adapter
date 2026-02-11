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
    private BigDecimal accd_Sa;
    @Column(name = "sub_accd")
    private BigDecimal subAccd_;
    @Column(name = "sub_rate_mil_accd")
    private BigDecimal subRateMilAccd_;
    @Column(name = "accd_occupational_loading_percent")
    private Double accd_OccupationalLoadingPercent;

    @Column(name = "accp_sa")
    private BigDecimal accp_Sa;
    @Column(name = "sub_accp")
    private BigDecimal subAccp_;
    @Column(name = "sub_rate_mil_accp")
    private BigDecimal subRateMilAccp_;
    @Column(name = "accp_occupational_loading_percent")
    private Double accp_OccupationalLoadingPercent;

    @Column(name = "acct_sa")
    private BigDecimal acct_Sa;
    @Column(name = "sub_acct")
    private BigDecimal subAcct_;
    @Column(name = "sub_rate_mil_acct")
    private BigDecimal subRateMilAcct_;
    @Column(name = "acct_occupational_loading_percent")
    private Double acct_OccupationalLoadingPercent;

    // CILL
    @Column(name = "cill_sa")
    private BigDecimal cill_Sa;
    @Column(name = "sub_cill")
    private BigDecimal subCill_;
    @Column(name = "sub_rate_mil_cill")
    private BigDecimal subRateMilCill_;
    @Column(name = "cill_occupational_loading_percentage")
    private Double cill_OccupationalLoadingPercentage;
    // CILX
    @Column(name = "cilx_sa")
    private BigDecimal cilx_Sa;
    @Column(name = "sub_cilx")
    private BigDecimal subCilx_;
    @Column(name = "sub_rate_mil_cilx")
    private BigDecimal subRateMilCilx_;
    @Column(name = "cilx_occupational_loading_percentage")
    private Double cilx_OccupationalLoadingPercentage;
    // FIB
    @Column(name = "fib_sa")
    private BigDecimal fib_Sa;
    @Column(name = "sub_fib")
    private BigDecimal subFib_;
    @Column(name = "sub_rate_mil_fib")
    private BigDecimal subRateMilFib_;
    @Column(name = "fib_occupational_loading_percentage")
    private Double fib_OccupationalLoadingPercentage;
    // FIBT
    @Column(name = "fibt_sa")
    private BigDecimal fibt_Sa;
    @Column(name = "sub_fibt")
    private BigDecimal subFibt_;
    @Column(name = "sub_rate_mil_fibt")
    private BigDecimal subRateMilFibt_;
    @Column(name = "fibt_occupational_loading_percentage")
    private Double fibt_OccupationalLoadingPercentage;
    // FSEB
    @Column(name = "fseb_sa")
    private BigDecimal fseb_Sa;
    @Column(name = "sub_fseb")
    private BigDecimal subFseb_;
    @Column(name = "sub_rate_mil_fseb")
    private BigDecimal subRateMilFseb_;
    @Column(name = "fseb_occupational_loading_percentage")
    private Double fseb_OccupationalLoadingPercentage;
    // LEB
    @Column(name = "leb_sa")
    private BigDecimal leb_Sa;
    @Column(name = "sub_leb")
    private BigDecimal subLeb_;
    @Column(name = "sub_rate_mil_leb")
    private BigDecimal subRateMilLeb_;
    @Column(name = "leb_occupational_loading_percentage")
    private Double leb_OccupationalLoadingPercentage;
    // PTD
    @Column(name = "ptd_sa")
    private BigDecimal ptd_Sa;
    @Column(name = "sub_ptd")
    private BigDecimal subPtd_;
    @Column(name = "sub_rate_mil_ptd")
    private BigDecimal subRateMilPtd_;
    @Column(name = "ptd_occupational_loading_percentage")
    private Double ptd_OccupationalLoadingPercentage;
    // TILL
    @Column(name = "till_sa")
    private BigDecimal till_Sa;
    @Column(name = "sub_till")
    private BigDecimal subTill_;
    @Column(name = "sub_rate_mil_till")
    private BigDecimal subRateMilTill_;
    @Column(name = "till_occupational_loading_percentage")
    private Double till_OccupationalLoadingPercentage;
    // TR
    @Column(name = "tr_sa")
    private BigDecimal tr_Sa;
    @Column(name = "sub_tr")
    private BigDecimal subTr_;
    @Column(name = "sub_rate_mil_tr")
    private BigDecimal subRateMilTr_;
    @Column(name = "tr_occupational_loading_percentage")
    private Double tr_OccupationalLoadingPercentage;
    // WOPA
    @Column(name = "wopa_sa")
    private BigDecimal wopa_Sa;
    @Column(name = "sub_wopa")
    private BigDecimal subWopa_;
    @Column(name = "sub_rate_mil_wopa")
    private BigDecimal subRateMilWopa_;
    @Column(name = "wopa_occupational_loading_percentage")
    private Double wopa_OccupationalLoadingPercentage;
    // WOPC
    @Column(name = "wopc_sa")
    private BigDecimal wopc_Sa;
    @Column(name = "sub_wopc")
    private BigDecimal subWopc_;
    @Column(name = "sub_rate_mil_wopc")
    private BigDecimal subRateMilWopc_;
    @Column(name = "wopc_occupational_loading_percentage")
    private Double wopc_OccupationalLoadingPercentage;
    // WOPD
    @Column(name = "wopd_sa")
    private BigDecimal wopd_Sa;
    @Column(name = "sub_wopd")
    private BigDecimal subWopd_;
    @Column(name = "sub_rate_mil_wopd")
    private BigDecimal subRateMilWopd_;
    @Column(name = "wopd_occupational_loading_percentage")
    private Double wopd_OccupationalLoadingPercentage;
    // FSEBA
    @Column(name = "fseba_sa")
    private BigDecimal fseba_Sa;
    @Column(name = "sub_fseba")
    private BigDecimal subFseba_;
    @Column(name = "sub_rate_mil_fseba")
    private BigDecimal subRateMilFseba_;
    @Column(name = "fseba_occupational_loading_percentage")
    private Double fseba_OccupationalLoadingPercentage;
    // HBA
    @Column(name = "hba_sa")
    private BigDecimal hba_Sa;
    @Column(name = "sub_hba")
    private BigDecimal subHba_;
    @Column(name = "sub_rate_mil_hba")
    private BigDecimal subRateMilHba_;
    @Column(name = "hba_occupational_loading_percentage")
    private Double hba_OccupationalLoadingPercentage;
    // HBAC
    @Column(name = "hbac_sa")
    private BigDecimal hbac_Sa;
    @Column(name = "sub_hbac")
    private BigDecimal subHbac_;
    @Column(name = "sub_rate_mil_hbac")
    private BigDecimal subRateMilHbac_;
    @Column(name = "hbac_occupational_loading_percentage")
    private Double hbac_OccupationalLoadingPercentage;
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
    //spouse Death
    @Column(name = "spouse_death_sa")
    private BigDecimal spouseDeath_Sa;
    @Column(name = "spouse_sub_death")
    private BigDecimal spouseSubDeath_;
    @Column(name = "spouse_sub_rate_mil_death")
    private BigDecimal spouseSubRateMilDeath_;
    @Column(name = "spouse_death_occupational_loading_percent")
    private Double spouseDeath_OccupationalLoadingPercent;

    @Column(name = "spouse_child_accd_sa")
    private BigDecimal spouseChildAccd_Sa;
    @Column(name = "spouse_child_sub_accd")
    private BigDecimal spouseChildSubAccd_;
    @Column(name = "spouse_child_sub_rate_mil_accd")
    private BigDecimal spouseChildSubRateMilAccd_;
    @Column(name = "spouse_child_accd_occupational_loading_percent")
    private Double spouseChildAccd_OccupationalLoadingPercent;

    @Column(name = "spouse_child_accp_sa")
    private BigDecimal spouseChildAccp_Sa;
    @Column(name = "spouse_child_sub_accp")
    private BigDecimal spouseChildSubAccp_;
    @Column(name = "spouse_child_sub_rate_mil_accp")
    private BigDecimal spouseChildSubRateMilAccp_;
    @Column(name = "spouse_child_accp_occupational_loading_percent")
    private Double spouseChildAccp_OccupationalLoadingPercent;

    @Column(name = "spouse_child_acct_sa")
    private BigDecimal spouseChildAcct_Sa;
    @Column(name = "spouse_child_sub_acct")
    private BigDecimal spouseChildSubAcct_;
    @Column(name = "spouse_child_sub_rate_mil_acct")
    private BigDecimal spouseChildSubRateMilAcct_;
    @Column(name = "spouse_child_acct_occupational_loading_percent")
    private Double spouseChildAcct_OccupationalLoadingPercent;

    @Column(name = "spouse_child_cill_sa")
    private BigDecimal spouseChildCill_Sa;
    @Column(name = "spouse_child_sub_cill")
    private BigDecimal spouseChildSubCill_;
    @Column(name = "spouse_child_sub_rate_mil_cill")
    private BigDecimal spouseChildSubRateMilCill_;
    @Column(name = "spouse_child_cill_occupational_loading_percent")
    private Double spouseChildCill_OccupationalLoadingPercent;

    @Column(name = "spouse_child_cilx_sa")
    private BigDecimal spouseChildCilx_Sa;
    @Column(name = "spouse_child_sub_cilx")
    private BigDecimal spouseChildSubCilx_;
    @Column(name = "spouse_child_sub_rate_mil_cilx")
    private BigDecimal spouseChildSubRateMilCilx_;
    @Column(name = "spouse_child_cilx_occupational_loading_percent")
    private Double spouseChildCilx_OccupationalLoadingPercent;

    @Column(name = "spouse_child_leb_sa")
    private BigDecimal spouseChildLeb_Sa;
    @Column(name = "spouse_child_sub_leb")
    private BigDecimal spouseChildSubLeb_;
    @Column(name = "spouse_child_sub_rate_mil_leb")
    private BigDecimal spouseChildSubRateMilLeb_;
    @Column(name = "spouse_child_leb_occupational_loading_percent")
    private Double spouseChildLeb_OccupationalLoadingPercent;

    @Column(name = "spouse_child_ptd_sa")
    private BigDecimal spouseChildPtd_Sa;
    @Column(name = "spouse_child_sub_ptd")
    private BigDecimal spouseChildSubPtd_;
    @Column(name = "spouse_child_sub_rate_mil_ptd")
    private BigDecimal spouseChildSubRateMilPtd_;
    @Column(name = "spouse_child_ptd_occupational_loading_percent")
    private Double spouseChildPtd_OccupationalLoadingPercent;

    @Column(name = "spouse_child_till_sa")
    private BigDecimal spouseChildTill_Sa;
    @Column(name = "spouse_child_sub_till")
    private BigDecimal spouseChildSubTill_;
    @Column(name = "spouse_child_sub_rate_mil_till")
    private BigDecimal spouseChildSubRateMilTill_;
    @Column(name = "spouse_child_till_occupational_loading_percent")
    private Double spouseChildTill_OccupationalLoadingPercent;

    @Column(name = "spouse_child_hb_sa")
    private BigDecimal spouseChildHb_Sa;
    @Column(name = "spouse_child_sub_hb")
    private BigDecimal spouseChildSubHb_;
    @Column(name = "spouse_child_sub_rate_mil_hb")
    private BigDecimal spouseChildSubRateMilHb_;
    @Column(name = "spouse_child_hb_occupational_loading_percent")
    private Double spouseChildHb_OccupationalLoadingPercent;

    @Column(name = "spouse_child_ppd_sa")
    private BigDecimal spouseChildPpd_Sa;
    @Column(name = "spouse_child_sub_ppd")
    private BigDecimal spouseChildSubPpd_;
    @Column(name = "spouse_child_sub_rate_mil_ppd")
    private BigDecimal spouseChildSubRateMilPpd_;
    @Column(name = "spouse_child_ppd_occupational_loading_percent")
    private Double spouseChildPpd_OccupationalLoadingPercent;

    @Column(name = "spouse_hba_sa")
    private BigDecimal spouseHba_Sa;
    @Column(name = "spouse_sub_hba")
    private BigDecimal spouseSubHba_;
    @Column(name = "spouse_sub_rate_mil_hba")
    private BigDecimal spouseSubRateMilHba_;
    @Column(name = "spouse_hba_occupational_loading_percent")
    private Double spouseHba_OccupationalLoadingPercent;
    // Children
    @Column(name = "child1_hbcac")
    private Integer child1Hbcac_;
    @Column(name = "child2_hbcac")
    private String child2Hbcac_;
    @Column(name = "child3_hbcac")
    private Integer child3Hbcac_;
    @Column(name = "child4_hbcac")
    private Integer child4Hbcac_;
    @Column(name = "child5_hbcac")
    private Integer child5Hbcac_;
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
