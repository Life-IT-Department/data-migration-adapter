package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "premium")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PremiumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", length = 50, nullable = false)
    private String policyNo;

    @Column(name = "receipt_no", length = 50)
    private String receiptNo;

    @Column(name = "premium_paid_cheque_no", length = 50)
    private String premiumPaidChequeNo;

    @Column(name = "premium_paid_bank", length = 100)
    private String premiumPaidBank;

    @Column(name = "premium_payment_date")
    private LocalDate premiumPaymentDate;

    @Column(name = "premium_paid_amount", precision = 18, scale = 2)
    private BigDecimal premiumPaidAmount;

    @Column(name = "premium_receipt_status", length = 20)
    private String premiumReceiptStatus;

    @Column(name = "premium_payment_type", length = 20)
    private String premiumPaymentType;

    @Column(name = "premium_payment_mode", length = 20)
    private String premiumPaymentMode;
}
