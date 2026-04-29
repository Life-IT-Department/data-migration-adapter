package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "unit_link")
public class UnitLinkReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String policyNumber;
    private String planNumber;
    private String customerName;

    // % Allocations
    @Column(precision = 19, scale = 4)
    private BigDecimal bondFundAllocation;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceFundAllocation;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthFundAllocation;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaFundAllocation;

    // Units - Regular
    @Column(precision = 19, scale = 4)
    private BigDecimal bondUnitsRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceUnitsRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthUnitsRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaUnitsRegular;

    // Units - Topup
    @Column(precision = 19, scale = 4)
    private BigDecimal bondUnitsTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceUnitsTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthUnitsTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaUnitsTopup;

    // Unit Prices
    @Column(precision = 19, scale = 4)
    private BigDecimal bondUnitPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceUnitPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthUnitPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaUnitPrice;

    // Values - Regular
    @Column(precision = 19, scale = 4)
    private BigDecimal bondValueRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceValueRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthValueRegular;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaValueRegular;

    // Values - Topup
    @Column(precision = 19, scale = 4)
    private BigDecimal bondValueTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal balanceValueTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal growthValueTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal shariaValueTopup;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalFundValue;

    private Integer switchingFrequency;

    private String internalFund;

    public <E> E mapData (Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
