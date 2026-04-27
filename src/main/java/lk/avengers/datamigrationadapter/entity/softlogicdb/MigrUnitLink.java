package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Migr_UnitLink")
public class MigrUnitLink {

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
}

