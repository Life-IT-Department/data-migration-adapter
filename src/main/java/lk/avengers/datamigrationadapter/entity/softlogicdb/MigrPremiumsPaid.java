package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Migr_PremiumsPaid")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrPremiumsPaid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "receipt_id", length = 100)
    private String receiptId;

    @Column(name = "cheque_no", length = 100)
    private String chequeNo;

    @Column(name = "bank", length = 150)
    private String bank;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "paid_amount", precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "receipt_status", length = 50)
    private String receiptStatus;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;
}
