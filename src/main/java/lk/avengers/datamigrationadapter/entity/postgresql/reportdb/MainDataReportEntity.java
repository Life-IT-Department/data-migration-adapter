package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
    @Column(name = "product_code",nullable = false)
    private String productCode;
    private String introducer;
    @Column(name = "full_name")
    private String fullName;
    private String gender;
    private Integer dob;
    @Column(name = "aae")
    private Integer aae;

    @Column(name = "number_of_riders_taken")
    private Integer numberOfRidersTaken;

    @Column(name = "dth_sar")
    private String dthSar;

    //SUB
    @Column(name = "sub_dth")
    private String subDth;
    @Column(name = "sub_rate_mil_dth")
    private String subRateMilDth;
    @Column(name = "dth_occupational_loading_percent")
    private Double dthOccupationalLoadingPercent;

    @Column(name = "accd_sa")
    private String accdSa;
    @Column(name = "sub_accd")
    private String subAccd;
    @Column(name = "sub_rate_mil_accd")
    private String subRateMilAccd;
    @Column(name = "accd_occupational_loading_percent")
    private Double accdOccupationalLoadingPercent;

    @Column(name = "accp_sa")
    private String accpSa;
    @Column(name = "sub_accp")
    private String subAccp;
    @Column(name = "sub_rate_mil_accp")
    private String subRateMilAccp;
    @Column(name = "accp_occupational_loading_percent")
    private Double accpOccupationalLoadingPercent;

    @Column(name = "acct_sa")
    private String acctSa;
    @Column(name = "sub_acct")
    private String subAcct;
    @Column(name = "sub_rate_mil_acct")
    private String subRateMilAcct;
    @Column(name = "acct_occupational_loading_percent")
    private Double acctOccupationalLoadingPercent;

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
    private Integer spouseChildDob;
    @Column(name = "spouse_child_age")
    private Integer spouseChildAge;
    @Column(name = "spouse_death_sa")
    private String spouseDeathSa;
    @Column(name = "spouse_sub_death")
    private String spouseSubDeath;
    @Column(name = "spouse_sub_rate_mil_death")
    private String spouseSubRateMilDeath;
    @Column(name = "spouse_death_occupational_loading_percent")
    private Double spouseDeathOccupationalLoadingPercent;
    @Column(name = "spouse_child_accd_sa")
    private String spouseChildAccdSa;

    @Column(name = "spouse_child_sub_accd")
    private String spouseChildSubAccd;

    @Column(name = "spouse_child_sub_rate_mil_accd")
    private String spouseChildSubRateMilAccd;

    @Column(name = "spouse_child_accd_occupational_loading_percent")
    private Double spouseChildAccdOccupationalLoadingPercent;

    @Column(name = "spouse_child_accp_sa")
    private String spouseChildAccpSa;

    @Column(name = "spouse_child_sub_accp")
    private String spouseChildSubAccp;

    @Column(name = "spouse_child_sub_rate_mil_accp")
    private String spouseChildSubRateMilAccp;

    @Column(name = "spouse_child_accp_occupational_loading_percent")
    private Double spouseChildAccpOccupationalLoadingPercent;

    @Column(name = "spouse_child_acct_sa")
    private String spouseChildAcctSa;

    @Column(name = "spouse_child_sub_acct")
    private String spouseChildSubAcct;

    @Column(name = "spouse_child_sub_rate_mil_acct")
    private String spouseChildSubRateMilAcct;

    @Column(name = "spouse_child_acct_occupational_loading_percent")
    private Double spouseChildAcctOccupationalLoadingPercent;

    @Column(name = "spouse_child_cill_sa")
    private String spouseChildCillSa;

    @Column(name = "spouse_child_sub_cill")
    private String spouseChildSubCill;

    @Column(name = "spouse_child_sub_rate_mil_cill")
    private String spouseChildSubRateMilCill;

    @Column(name = "spouse_child_cill_occupational_loading_percent")
    private Double spouseChildCillOccupationalLoadingPercent;

    @Column(name = "spouse_child_cilx_sa")
    private String spouseChildCilxSa;

    @Column(name = "spouse_child_sub_cilx")
    private String spouseChildSubCilx;

    @Column(name = "spouse_child_sub_rate_mil_cilx")
    private String spouseChildSubRateMilCilx;

    @Column(name = "spouse_child_cilx_occupational_loading_percent")
    private Double spouseChildCilxOccupationalLoadingPercent;

    @Column(name = "spouse_child_leb_sa")
    private String spouseChildLebSa;

    @Column(name = "spouse_child_sub_leb")
    private String spouseChildSubLeb;

    @Column(name = "spouse_child_sub_rate_mil_leb")
    private String spouseChildSubRateMilLeb;

    @Column(name = "spouse_child_leb_occupational_loading_percent")
    private Double spouseChildLebOccupationalLoadingPercent;

    @Column(name = "spouse_child_ptd_sa")
    private String spouseChildPtdSa;

    @Column(name = "spouse_child_sub_ptd")
    private String spouseChildSubPtd;

    @Column(name = "spouse_child_sub_rate_mil_ptd")
    private String spouseChildSubRateMilPtd;

    @Column(name = "spouse_child_ptd_occupational_loading_percent")
    private Double spouseChildPtdOccupationalLoadingPercent;

    @Column(name = "spouse_child_till_sa")
    private String spouseChildTillSa;

    @Column(name = "spouse_child_sub_till")
    private String spouseChildSubTill;

    @Column(name = "spouse_child_sub_rate_mil_till")
    private String spouseChildSubRateMilTill;

    @Column(name = "spouse_child_till_occupational_loading_percent")
    private Double spouseChildTillOccupationalLoadingPercent;

    @Column(name = "spouse_child_hb_sa")
    private String spouseChildHbSa;

    @Column(name = "spouse_child_sub_hb")
    private String spouseChildSubHb;

    @Column(name = "spouse_child_sub_rate_mil_hb")
    private String spouseChildSubRateMilHb;

    @Column(name = "spouse_child_hb_occupational_loading_percent")
    private Double spouseChildHbOccupationalLoadingPercent;

    @Column(name = "spouse_child_ppd_sa")
    private String spouseChildPpdSa;

    @Column(name = "spouse_child_sub_ppd")
    private String spouseChildSubPpd;

    @Column(name = "spouse_child_sub_rate_mil_ppd")
    private String spouseChildSubRateMilPpd;

    @Column(name = "spouse_child_ppd_occupational_loading_percent")
    private Double spouseChildPpdOccupationalLoadingPercent;

    @Column(name = "spouse_hba_sa")
    private String spouseHbaSa;

    @Column(name = "spouse_sub_hba")
    private String spouseSubHba;

    @Column(name = "spouse_sub_rate_mil_hba")
    private String spouseSubRateMilHba;

    @Column(name = "spouse_hba_occupational_loading_percent")
    private Double spouseHbaOccupationalLoadingPercent;

    // Children 1..5
    @Column(name = "child1_name")
    private String child1Name;
    @Column(name = "child1_dob")
    private Integer child1Dob;
    @Column(name = "child1_age")
    private Integer child1Age;
    @Column(name = "child1_hbc")
    private String child1Hbc;
    @Column(name = "child1_hbcac")
    private String child1Hbcac;
    @Column(name = "child2_name")
    private String child2Name;
    @Column(name = "child2_dob")
    private Integer child2Dob;
    @Column(name = "child2_age")
    private Integer child2Age;
    @Column(name = "child2_hbc")
    private String child2Hbc;
    @Column(name = "child2_hbcac")
    private String child2Hbcac;
    @Column(name = "child3_name")
    private String child3Name;
    @Column(name = "child3_dob")
    private Integer child3Dob;
    @Column(name = "child3_age")
    private Integer child3Age;
    @Column(name = "child3_hbc")
    private String child3Hbc;
    @Column(name = "child3_hbcac")
    private String child3Hbcac;

    @Column(name = "child4_name")
    private String child4Name;

    @Column(name = "child4_dob")
    private Integer child4Dob;

    @Column(name = "child4_age")
    private Integer child4Age;

    @Column(name = "child4_hbc")
    private String child4Hbc;

    @Column(name = "child4_hbcac")
    private String child4Hbcac;

    @Column(name = "child5_name")
    private String child5Name;

    @Column(name = "child5_dob")
    private Integer child5Dob;

    @Column(name = "child5_age")
    private Integer child5Age;

    @Column(name = "child5_hbc")
    private String child5Hbc;
    @Column(name = "child5_hbcac")
    private String child5Hbcac;


    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
