package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "policy_list")
public class PolicyListEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "agent")
    private String agent;
    @Column(name = "unit_head")
    private String unitHead;
    @Column(name = "sales_branch")
    private String salesBranch;
    @Column(name = "company_branch")
    private String companyBranch;
    @Column(name = "policy_branch")
    private String policyBranch;
    @Column(name = "introducer")
    private String introducer;
    @Column(name = "supervisor")
    private String supervisor;
    @Column(name = "contract_number") // Mapped from 'Contract'
    private String contract;
    @Column(name = "currency") // Mapped from 'C/Y'
    private String currency;
    @Column(name = "customer_name")
    private String customerName;
    @Column(name = "inception_date")
    private LocalDate inception;
    @Column(name = "frequency_mode")
    private String frequencyMode;
    @Column(name = "status")
    private String status;
    @Column(name = "annual_premium")
    private BigDecimal annualPremium;
    @Column(name = "paid_up_to")
    private LocalDate paidUpTo;
    @Column(name = "unappropriate_balance")
    private BigDecimal unappropriateBalance;
    @Column(name = "request_uuid")
    private String requestUuid;
}
