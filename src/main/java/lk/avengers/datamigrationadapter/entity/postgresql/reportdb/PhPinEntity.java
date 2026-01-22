package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contact_detail")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhPinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product")
    private String product;

    @Column(name = "policy_no")
    private String policyNo;

    @Column(name = "pin")
    private String pin;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "current_age")
    private Integer currentAge;

    @Column(name = "nic")
    private String nic;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "other_telephone")
    private String otherTelephone;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "agent_code")
    private String agentCode;

    @Column(name = "sales_branch")
    private String salesBranch;

    @Column(name = "total_premiums_paid")
    private BigDecimal totalPremiumsPaid;

    @Column(name = "outstanding")
    private BigDecimal outstanding;

    @Column(name = "modal_premium_without_handling_fee")
    private BigDecimal modalPremiumWithoutHandlingFee;

    @Column(name = "policy_inception_date")
    private LocalDate policyInceptionDate;

    @Column(name = "policy_issue_date")
    private LocalDate policyIssueDate;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "next_premium_due_date")
    private LocalDate nextPremiumDueDate;

    @Column(name = "last_premium_payment_due_date")
    private LocalDate lastPremiumPaymentDueDate;

    @Column(name = "policy_expiry_date")
    private LocalDate policyExpiryDate;

    @Column(name = "term")
    private Integer term;

    @Column(name = "lapsed_date")
    private LocalDate lapsedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "language_preference")
    private String languagePreference;
}
