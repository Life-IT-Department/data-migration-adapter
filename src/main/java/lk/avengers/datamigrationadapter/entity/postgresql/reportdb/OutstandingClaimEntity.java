package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outstanding_claim")
public class OutstandingClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "claim_office_number")
    private String claimOfficeNumber;
    private String product;
    @Column(name = "policy_number")
    private String policyNumber;
    @Column(name = "agent_number")
    private String agentNumber;
    @Column(name = "agent_name", columnDefinition = "TEXT")
    private String agentName;
    @Column(name = "agent_type")
    private String agentType;
    @Column(name = "company_branch")
    private String companyBranch;
    @Column(name = "sales_branch")
    private String salesBranch;
    @Column(name = "occurred_on")
    private LocalDate occurredOn;
    @Column(name = "declared_on")
    private LocalDate declaredOn;
    @Column(name = "occurred")
    private Double occurred;
    @Column(name = "declared")
    private Double declared;
    @Column(name = "last_trn")
    private Double lastTrn;
    @Column(name = "original_claim_amount")
    private Double originalClaimAmount;
    @Column(name = "total_claim_amount")
    private Double totalClaimAmount;
    @Column(name = "total_fees_amount")
    private Double totalFeesAmount;
    @Column(name = "paid_amount")
    private Double paidAmount;
    @Column(name = "paid_fees_amount")
    private Double paidFeesAmount;
    @Column(name = "os_claim_and_fees")
    private Double osClaimAndFees;
    @Column(name = "recovery_amount")
    private Double recoveryAmount;
    @Column(name = "os_recovery")
    private Double osRecovery;
    @Column(name = "policyholder_or_insured")
    private String policyholderOrInsured;
    @Column(name = "claimed_life_assured")
    private String ClaimedLifeAssured;
    @Column(name = "profession_or_activity")
    private String professionOrActivity;
    @Column(name = "district")
    private String district;
    @Column(name = "place_of_claim")
    private String placeOfClaim;
    @Column(name = "cause_of_loss")
    private String causeOfLoss;
    @Column(name = "cause_of_claim")
    private String causeOfClaim;
    @Column(name = "status_notes")
    private String statusNotes;
    @Column(name = "claim_description", columnDefinition = "TEXT")
    private String claimDescription;
    @Column(name = "claim_type")
    private String claimType;
    @Column(name = "underwriting_year")
    private Integer underwritingYear;
    @Column(name = "current_year")
    private Integer currentYear;
    @Column(name = "current_month")
    private Integer currentMonth;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
