package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Migr_PremiumsDue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrPremiumsDue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "period")
    private Integer period;

    @Column(name = "term")
    private Integer term;

    @Column(name = "due_amount", precision = 15, scale = 2)
    private BigDecimal dueAmount;

    @Column(name = "paid_up_date")
    private LocalDate paidUpDate;
}
