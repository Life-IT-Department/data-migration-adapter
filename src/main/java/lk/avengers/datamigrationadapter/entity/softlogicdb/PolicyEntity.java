package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policy")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 50, unique = true)
    private String policyNo;

    @Column(name = "plancode", length = 50)
    private String planCode;

    @Column(name = "term")
    private Integer term;

    @Column(name = "payment_term")
    private Integer paymentTerm;

    @Column(name = "date_of_proposal")
    private LocalDate dateOfProposal;

    @Column(name = "bsa", precision = 18, scale = 2)
    private BigDecimal bsa;

    @Column(name = "sum_at_risk", precision = 18, scale = 2)
    private BigDecimal sumAtRisk;

    @Column(name = "basic_premium", precision = 18, scale = 2)
    private BigDecimal basicPremium;

    @Column(name = "premium_type", length = 20)
    private String premiumType;

    @Column(name = "adv_code", length = 50)
    private String advCode;

    @Column(name = "begin_date")
    private LocalDate beginDate;

    @Column(name = "policy_year")
    private Integer policyYear;

    @Column(name = "date_underwritten")
    private LocalDate dateUnderwritten;

    @Column(name = "premium_due_date")
    private LocalDate premiumDueDate;

    @Column(name = "mode", length = 20)
    private String mode;

    @Column(name = "policy_status_code", length = 20)
    private String policyStatusCode;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "premium", precision = 18, scale = 2)
    private BigDecimal premium;

    @Column(name = "illus_matu_value", precision = 18, scale = 2)
    private BigDecimal illusMatuValue;

    @Column(name = "admin_fee", precision = 18, scale = 2)
    private BigDecimal adminFee;

    @Column(name = "total_fund_balance", precision = 18, scale = 2)
    private BigDecimal totalFundBalance;
}
