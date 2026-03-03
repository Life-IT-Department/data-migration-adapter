package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "premium_details")
@EntityListeners(AuditingEntityListener.class)
public class PremiumDetailsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy_no", nullable = false)
    private Integer policyNo;
    @Column(name = "proposal_no")
    private Integer proposalNo;
    @Column(name = "product_code")
    private String productCode;
    @Column(name = "plan_no")
    private String planNo;
    @Column(name = "inception_date")
    private LocalDate inceptionDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
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
    private String cy;
    @Column(name = "premium_payment_term")
    private Integer premiumPaymentTerm;
    @Column(name = "deferment_term")
    private Integer defermentTerm;
    @Column(name = "retirement_benefit_payout_term")
    private Integer retirementBenefitPayoutTerm;
    @Column(name = "modal_premium")
    private Double modalPremium;
    private Integer frequency;
    @Column(name = "next_premium")
    private LocalDate nextPremium;
    private String status;
    private LocalDate date;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(name = "agent_code")
    private String agentCode;
    private String introducer;
    private String supervisor;
    @Column(name = "ri_percentage")
    private Double riPercentage;
    @Column(name = "policy_year")
    private Integer policyYear;
    @Column(name = "policy_month")
    private Integer policyMonth;
    @Column(name = "modal_premium_amount")
    private Double modalPremiumAmount;
    @Column(name = "allocation_amount")
    private Double allocationAmount;
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    @Column(name = "premium_due_date")
    private LocalDate premiumDueDate;
    @CreatedDate
    private LocalDateTime createdDate;
}