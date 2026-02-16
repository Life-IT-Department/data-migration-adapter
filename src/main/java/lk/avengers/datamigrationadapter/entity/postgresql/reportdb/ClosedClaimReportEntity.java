package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "closed_claim")
public class ClosedClaimReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no")
    private String policyNo;

    @Column(name = "claim_no")
    private String claimNo;

    @Column(name = "sales_branch")
    private String salesBranch;

    @Column(name = "company_agency")
    private String companyAgency;

    @Column(name = "agent_code")
    private String agentCode;

    @Column(name = "agent_name", columnDefinition = "text")
    private String agentName;

    @Column(name = "policy_holder", columnDefinition = "text")
    private String policyHolder;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "type_of_claim")
    private String typeOfClaim;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "declaration_date")
    private LocalDate declarationDate;

    @Column(name = "status_date")
    private LocalDate statusDate;

    @Column(name = "claim_amount")
    private Double claimAmount;

    @Column(name = "pre_last_evaluation")
    private Double preLastEvaluation;

    @Column(name = "evaluation_amount")
    private Double evaluationAmount;

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "current_year")
    private Integer currentYear;

    @Column(name = "current_month")
    private Integer currentMonth;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
