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
@Table(name = "rejected_claim")
public class RejectedClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "policy")
    private String policy;
    @Column(name = "claim_no")
    private String claimNo;
    @Column(name = "sales_agency")
    private String salesAgency;
    @Column(name = "company_agency")
    private String companyAgency;
    @Column(name = "agent")
    private String agent;
    @Column(name = "claim_type")
    private String claimType;
    @Column(name = "occurred_date")
    private LocalDate occurredDate;
    @Column(name = "declared_date")
    private LocalDate declaredDate;
    @Column(name = "policyholder_name")
    private String policyholderName;
    @Column(name = "claimant_name")
    private String claimantName;
    @Column(name = "rider")
    private String rider;
    @Column(name = "rejected_date")
    private LocalDate rejectedDate;
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    @Column(name = "claimed_amount")
    private Double claimedAmount;
    @Column(name = "expert_fee")
    private Double expertFee;
    @Column(name = "paid_fee")
    private Double paidFee;
    @Column(name = "curr")
    private String curr;
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
