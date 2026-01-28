package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pos_signature_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PosSignatureReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_type", length = 50)
    private String proposalType;

    @Column(name = "proposal_no", length = 50)
    private String proposalNo;

    @Column(name = "policy_no", length = 50)
    private String policyNo;

    @Column(name = "advisor_code", length = 50)
    private String advisorCode;

    @Column(name = "branch", length = 100)
    private String branch;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_address", length = 500)
    private String customerAddress;

    @Column(name = "customer_mobile", length = 20)
    private String customerMobile;

    @Column(name = "proposal_transferred_date")
    private LocalDate proposalTransferredDate;

    @Column(name = "policy_issuance_date")
    private LocalDate policyIssuanceDate;

    @Column(name = "policy_inception_date")
    private LocalDate policyInceptionDate;

    @Column(name = "premium", precision = 18, scale = 2)
    private BigDecimal premium;

    @Column(name = "policy_year")
    private Integer policyYear;

    @Column(name = "basic_commission_on_premium", precision = 18, scale = 2)
    private BigDecimal basicCommissionOnPremium;

    @Column(name = "report_year", nullable = false)
    private Integer year;
}

