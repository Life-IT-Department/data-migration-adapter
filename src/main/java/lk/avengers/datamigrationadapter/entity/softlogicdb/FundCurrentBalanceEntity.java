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
@Table(name = "Migr_FundCurrentBalance")
public class FundCurrentBalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Policy_No", nullable = false, length = 30)
    private String policyNo;

    @Column(name = "Total_Balance", precision = 18, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "Topup_Balance", precision = 18, scale = 2)
    private BigDecimal topupBalance;

    @Column(name = "PRM_VALUE_TODAY", precision = 18, scale = 2)
    private BigDecimal prmValueToday;
}
