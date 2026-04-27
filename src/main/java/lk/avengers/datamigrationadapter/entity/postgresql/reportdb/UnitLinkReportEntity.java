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
    private BigDecimal bondFundAllocation;
    private BigDecimal balanceFundAllocation;
    private BigDecimal growthFundAllocation;
    private BigDecimal shariaFundAllocation;

    // Units - Regular
    private BigDecimal bondUnitsRegular;
    private BigDecimal balanceUnitsRegular;
    private BigDecimal growthUnitsRegular;
    private BigDecimal shariaUnitsRegular;

    // Units - Topup
    private BigDecimal bondUnitsTopup;
    private BigDecimal balanceUnitsTopup;
    private BigDecimal growthUnitsTopup;
    private BigDecimal shariaUnitsTopup;

    // Unit Prices
    private BigDecimal bondUnitPrice;
    private BigDecimal balanceUnitPrice;
    private BigDecimal growthUnitPrice;
    private BigDecimal shariaUnitPrice;

    // Values - Regular
    private BigDecimal bondValueRegular;
    private BigDecimal balanceValueRegular;
    private BigDecimal growthValueRegular;
    private BigDecimal shariaValueRegular;

    // Values - Topup
    private BigDecimal bondValueTopup;
    private BigDecimal balanceValueTopup;
    private BigDecimal growthValueTopup;
    private BigDecimal shariaValueTopup;

    private BigDecimal totalFundValue;

    private Integer switchingFrequency;

    private String internalFund;

    public <E> E mapData (Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
