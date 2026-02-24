package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fund_current_balance")
public class FundCurrentBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 30)
    private String policyNo;

    @Column(name = "total_balance", precision = 18, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "topup_balance", precision = 18, scale = 2)
    private BigDecimal topupBalance;
}
