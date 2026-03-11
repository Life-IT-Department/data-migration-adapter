package lk.avengers.datamigrationadapter.entity.softlogicdb;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @Column(name = "PR_PolicyNo", length = 30)
    private String policyNo;

    @Column(name = "PR_ReceiptId")
    private Integer receiptId;

    @Column(name = "PR_ChequeNo", length = 30)
    private String chequeNo;

    @Column(name = "PR_Bank", length = 50)
    private String bank;

    @Column(name = "PR_PaymentDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;

    @Column(name = "PR_PaidAmount", precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "PR_ReceiptStatus", length = 10)
    private String receiptStatus;

    @Column(name = "PR_PaymentType", length = 10)
    private String paymentType;

    @Column(name = "PR_PaymentMode", length = 20)
    private String paymentMode;
}
