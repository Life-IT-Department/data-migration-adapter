package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseMainData {
    // --- 1. COMMON IDENTIFIERS ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_no", nullable = false)
    private Integer policyNo;
    @Column(name = "proposal_no",nullable = false)
    private Integer proposalNo;
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
    private Integer salesBranchCode;
    @Column(name = "sales_branch_name")
    private String salesBranchName;
    @Column(name = "company_branch_code")
    private Integer companyBranchCode;
    @Column(name = "company_branch_name")
    private String companyBranchName;
    @Column(name = "policy_branch_code")
    private Integer policyBranchCode;
    @Column(name = "policy_branch_name")
    private String policyBranchName;
    private Integer term;
    @Column(name = "cy")
    private String cy;
    @Column(name = "modal_premium")
    private BigDecimal modalPremium;
    private Integer frequency;
    @Column(name = "next_premium")
    private LocalDate nextPremium;
    private String reason;
    private String status;
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
    private LocalDate dob;
    private Integer aae;
    @Column(name = "sar_choice")
    private String sarChoice;
    @Column(name = "number_of_riders_taken")
    private Integer numberOfRidersTaken;
    //SUB
    @Column(name = "sub_dth")
    private Integer subDth;
    @Column(name = "sub_rate_mil_dth")
    private BigDecimal subRateMilDth;
    @Column(name = "dth_occupational_loading_percent")
    private Double dthOccupationalLoadingPercent;
    //
    private LocalDate date;
    // DTH
    @Column(name = "dth_sar")
    private BigDecimal dthSar;
    @Column(name = "dth_occupational_loading_percentage")
    private Double dthOccLoadingPercentage;
    // COMMON HB (Hospital Benefit?)
    @Column(name = "hb_sa")
    private BigDecimal hbSa;
    @Column(name = "sub_hb")
    private BigDecimal subHb;
    @Column(name = "sub_rate_mil_hb")
    private BigDecimal subRateMilHb;
    @Column(name = "hb_occupational_loading_percentage")
    private Double hbOccupationalLoadingPercentage;
    // --- COMMON Financial Summary ---
    @Column(name = "basic_sum_assured")
    private BigDecimal basicSumAssured;
    @Column(name = "insurance_coverage_period")
    private Integer insuranceCoveragePeriod;
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;
    @Column(name = "last_premium_due_date")
    private LocalDate lastPremiumDueDate;
    @Column(name = "refund_value")
    private BigDecimal refundValue;
    // COMMON Children info
    @Column(name = "child1_name")
    private String child1Name;
    @Column(name = "child1_dob")
    private LocalDate child1Dob;
    @Column(name = "child1_age")
    private Integer child1Age;
    @Column(name = "child1_hbc")
    private Integer child1Hbc;
    @Column(name = "child2_name")
    private String child2Name;
    @Column(name = "child2_dob")
    private LocalDate child2Dob;
    @Column(name = "child2_age")
    private Integer child2Age;
    @Column(name = "child2_hbc")
    private String child2Hbc;
    @Column(name = "child3_name")
    private String child3Name;
    @Column(name = "child3_dob")
    private LocalDate child3Dob;
    @Column(name = "child3_age")
    private Integer child3Age;
    @Column(name = "child3_hbc")
    private Integer child3Hbc;
    @Column(name = "child4_name")
    private String child4Name;
    @Column(name = "child4_dob")
    private LocalDate child4Dob;
    @Column(name = "child4_age")
    private Integer child4Age;
    @Column(name = "child4_hbc")
    private Integer child4Hbc;
    @Column(name = "child5_name")
    private String child5Name;
    @Column(name = "child5_dob")
    private LocalDate child5Dob;
    @Column(name = "child5_age")
    private Integer child5Age;
    @Column(name = "child5_hbc")
    private Integer child5Hbc;
    //
    @Column(name = "operation_date")
    private LocalDate operationDate;
}