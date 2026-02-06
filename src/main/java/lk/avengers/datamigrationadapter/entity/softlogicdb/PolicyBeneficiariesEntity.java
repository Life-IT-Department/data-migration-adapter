package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policy_beneficiaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyBeneficiariesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "BE_PolicyNo", length = 30)
    private String bePolicyNo;

    @Column(name = "BE_FullName", length = 100)
    private String beFullName;

    @Column(name = "BE_Age")
    private Integer beAge;

    @Column(name = "BE_Sex", length = 1 ,columnDefinition="char")
    private Character beSex;

    @Column(name = "BE_DOB")
    private LocalDate beDob;

    @Column(name = "BE_NIC", length = 20)
    private String beNic;

    @Column(name = "BE_Type", length = 10)
    private String beType;

    @Column(name = "BE_InclusionDate")
    private LocalDate beInclusionDate;

    @Column(name = "BE_Coverage", precision = 18,  scale = 2)
    private BigDecimal beCoverage;

    @Column(name = "BE_IsHB")
    private Boolean beIsHb;
}
