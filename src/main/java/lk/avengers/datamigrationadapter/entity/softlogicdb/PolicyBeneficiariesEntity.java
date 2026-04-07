package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Migr_PolicyBeneficieries_New")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyBeneficiariesEntity {

    @Id
    @Column(name = "AutoID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer autoId;

    @Column(name = "BE_PolicyNo", length = 30)
    private String bePolicyNo;

    @Column(name = "BE_FullName", columnDefinition = "VARCHAR(MAX)")
    private String beFullName;

    @Column(name = "BE_Age")
    private Integer beAge;

    @Column(name = "BE_Sex", length = 1 ,columnDefinition="char")
    private Character beSex='M';

    @Column(name = "BE_DOB")
    private LocalDate beDob;

    @Column(name = "BE_NIC", length = 20)
    private String beNic;

    @Column(name = "BE_Type", length = 10)
    private String beType;

    @Column(name = "BE_InclusionDate")
    private LocalDate beInclusionDate;

    @Column(name = "BE_Coverage", precision = 18,  scale = 2)
    private BigDecimal beCoverage=BigDecimal.ZERO;

    @Column(name = "BE_IsHB")
    private Boolean beIsHb;

    // Remove when running actual list

    @Column(name = "Child_Code")
    private Integer childCode;

    @Column(name = "Basic_Sum_Assured")
    private BigDecimal basicSumAssured;

    @Column(name = "HB_SA")
    private BigDecimal hbcSa;

    @Column(name = "INP_SA")
    private BigDecimal inpcSa;

    @Column(name = "Bonus")
    private BigDecimal bonus;
}
