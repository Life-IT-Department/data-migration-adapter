package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "paid_claim")
public class PaidClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "claim_off_no")
    private String claimOfficeNumber;
    @Column(name = "product")
    private String product;
    @Column(name = "policy_no")
    private String policyNo;
    @Column(name = "agent_no")
    private String agentNo;
    @Column(name = "agent_name", columnDefinition = "TEXT")
    private String agentName;
    @Column(name = "company_branch")
    private String companyBranch;
    @Column(name = "sales_branch")
    private String salesBranch;
    @Column(name = "branch_code")
    private Integer branchCode;
    @Column(name = "sum_assured_of_the_benefit")
    private Double sumAssuredOfTheBenefit;
    @Column(name = "mcfp")
    private Double mcfp;
    @Column(name = "medical_or_non_medical")
    private Boolean medicalOrNonMedical;
    @Column(name = "uw_decision")
    private Double uwDecision;
    @Column(name = "occurred_on")
    private LocalDate occurredOn;
    @Column(name = "declared_on")
    private LocalDate declaredOn;
    @Column(name = "occurred")
    private Double occurred;
    @Column(name = "declared")
    private Double declared;
    @Column(name = "original_claim_amt")
    private Double originalClaimAmt;
    @Column(name = "total_claim_amt")
    private Double totalClaimAmt;
    @Column(name = "total_fees_amt")
    private Double totalFeesAmt;
    @Column(name = "trn_date")
    private LocalDate trnDate;
    @Column(name = "trn_status")
    private String trnStatus;
    @Column(name = "trn_amount_cy")
    private Double trnAmountCy;
    @Column(name = "trn_amount_lc")
    private Double trnAmountLc;
    @Column(name = "payee_pin")
    private String payeePin;
    @Column(name = "policy_holder")
    private String policyHolder;
    @Column(name = "claimed_life_assured")
    private String claimedLifeAssured;
    @Column(name = "claimant_name")
    private String claimantName;
    @Column(name = "profession_code")
    private Long professionCode;
    @Column(name = "profession_or_activity")
    private String professionOrActivity;
    @Column(name = "no_of_previous_claims")
    private Integer noOfPreviousClaims;
    @Column(name = "previous_claims_total_settlement")
    private Double previousClaimsTotalSettlement;
    @Column(name = "previous_claim_numbers", columnDefinition = "TEXT")
    private String previousClaimNumbers;
    @Column(name = "life_assured_current_age")
    private Integer lifeAssuredCurrentAge;
    @Column(name = "long_risk_no")
    private Integer longRiskNo;
    @Column(name = "gender")
    private String gender;
    @Column(name = "policy_age_at_claim_date")
    private Integer policyAgeAtClaimDate;
    @Column(name = "no_of_days_hospitalized_standard")
    private Integer noOfDaysHospitalizedStandard;
    @Column(name = "no_of_days_hospitalized_icu")
    private Integer noOfDaysHospitalizedIcu;
    @Column(name = "policy_holder_address", columnDefinition = "TEXT")
    private String policyHolderAddress;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "district")
    private String district;
    @Column(name = "place_of_claim")
    private String placeOfClaim;
    @Column(name = "cause_of_loss", columnDefinition = "TEXT")
    private String causeOfLoss;
    @Column(name = "cause_of_claim", columnDefinition = "TEXT")
    private String causeOfClaim;
    @Column(name = "status_notes", columnDefinition = "TEXT")
    private String statusNotes;
    @Column(name = "claim_description", columnDefinition = "TEXT")
    private String claimDescription;
    @Column(name = "underwriting_year")
    private Integer underwritingYear;
    @Column(name = "expert")
    private Integer expert;
    @Column(name = "pay_to")
    private String payTo;
    @Column(name = "x_gracia")
    private String xGracia;
    @Column(name = "hcp_name")
    private String hcpName;
    @Column(name = "claim_type")
    private String claimType;
    @Column(name = "policy_status")
    private String policyStatus;
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
