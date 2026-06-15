package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Premium_Extra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumExtraFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PolicyNo", length = 30)
    private String policyNo;

    @Column(name = "InceptionDate")
    private LocalDate inceptionDate;

    @Column(name = "PremiumDueDate")
    private LocalDate premiumDueDate;

    @Column(name = "Period")
    private Integer period;

    @Column(name = "PaidCount")
    private Integer paidCount;

    @Column(name = "PremiumsPaidTotal")
    private BigDecimal premiumsPaidTotal;

    @Column(name = "OutstandingTotal")
    private BigDecimal outstandingTotal;

    @Column(name = "Frequency")
    private String frequency;

    @Column(name = "Term")
    private Integer term;

    @Column(name = "ModalPremium")
    private BigDecimal modalPremium;

    @Column(name = "PolicyStatus")
    private String policyStatus;

    @Column(name = "LapsedDate")
    private LocalDate lapsedDate;
}
