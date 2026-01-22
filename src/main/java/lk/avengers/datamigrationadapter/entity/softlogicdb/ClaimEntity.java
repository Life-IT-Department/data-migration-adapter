package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", length = 50, nullable = false)
    private String policyNo;

    @Column(name = "claim_no", length = 50, nullable = false)
    private String claimNo;

    @Column(name = "claim_type", length = 50)
    private String claimType;

    @Column(name = "date_of_event")
    private LocalDate dateOfEvent;

    @Column(name = "date_of_intimation")
    private LocalDate dateOfIntimation;

    @Column(name = "patient_admitted")
    private Boolean patientAdmitted;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "cause_of_death", length = 200)
    private String causeOfDeath;

    @Column(name = "nature_of_illness", length = 200)
    private String natureOfIllness;

    @Column(name = "total_claim_amount", precision = 18, scale = 2)
    private BigDecimal totalClaimAmount;

    @Column(name = "total_settled_amount", precision = 18, scale = 2)
    private BigDecimal totalSettledAmount;

    @Column(name = "claim_status", length = 50)
    private String claimStatus;

    @Column(name = "name_of_the_hospital", length = 200)
    private String nameOfTheHospital;

    @Column(name = "done_by", length = 100)
    private String doneBy;

    @Column(name = "total_days")
    private Integer totalDays;

    @Column(name = "date_of_payment")
    private LocalDate dateOfPayment;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "pay_mode", length = 50)
    private String payMode;

    @Column(name = "comments", length = 500)
    private String comments;

    @Column(name = "pay_ins", length = 100)
    private String payIns;

    @Column(name = "dis_date")
    private LocalDate disDate;

    @Column(name = "policy_year")
    private Integer policyYear;

    @Column(name = "bht_no", length = 50)
    private String bhtNo;

    @Column(name = "ailment_code_ri", length = 50)
    private String ailmentCodeRI;

    @Column(name = "voucher_no", length = 50)
    private String voucherNo;

    @Column(name = "cheque_no", length = 50)
    private String chequeNo;
}
