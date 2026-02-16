package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "declared_claim")
public class DeclaredClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "claim_office_number")
    private String claimOfficeNumber;
    private String product;
    @Column(name = "policy_no", columnDefinition = "TEXT")
    private String policyNo;
    @Column(name = "agent_number", columnDefinition = "TEXT")
    private String agentNumber;
    @Column(name = "agent_name", columnDefinition = "TEXT")
    private String agentName;
    @Column(name = "company_branch", columnDefinition = "TEXT")
    private String companyBranch;
    @Column(name = "sales_branch")
    private String salesBranch;
    @Column(name = "occurred_date")
    private LocalDate occurredDate;
    @Column(name = "declared_date")
    private LocalDate declaredDate;
    private Double occurred;
    private Double declared;
    @Column(name = "last_trn")
    private Double lastTrn;
    @Column(name = "original_claim_amt")
    private Double originalClaimAmt;
    @Column(name = "total_claim_amt")
    private Double totalClaimAmt;
    @Column(name = "total_fees_amt")
    private Double totalFeesAmt;
    @Column(name = "paid_amount")
    private Double paidAmount;
    @Column(name = "paid_fees_amount")
    private Double paidFeesAmount;
    @Column(name = "os_claim_amt")
    private Double osClaimAmt;
    @Column(name = "recovered_amt")
    private Double recoveredAmt;
    @Column(name = "os_recovery")
    private Double osRecovery;
    @Column(name = "policy_holder_title")
    private String policyHolderTitle;
    @Column(name = "policyholder_or_insured", columnDefinition = "TEXT")
    private String policyholderOrInsured;
    @Column(name = "claimed_title")
    private String claimedTitle;
    @Column(name = "claimed_life_assured", columnDefinition = "TEXT")
    private String claimedLifeAssured;
    @Column(name = "claim_closed_date")
    private LocalDate claimClosedDate;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "profession_or_activity")
    private String professionOrActivity;
    private String district;
    @Column(name = "place_of_claims")
    private String placeOfClaims;
    @Column(name = "cause_of_loss", columnDefinition = "TEXT")
    private String causeOfLoss;
    @Column(name = "cause_of_claims", columnDefinition = "TEXT")
    private String causeOfClaims;
    @Column(name = "status_notes", columnDefinition = "TEXT")
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
