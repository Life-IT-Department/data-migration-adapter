package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiariesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", length = 50, nullable = false)
    private String policyNo;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sex", length = 10)
    private String sex;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "nic", length = 20, unique = true)
    private String nic;

    @Column(name = "[type]", length = 50)
    private String type;

    @Column(name = "inclusion_date")
    private LocalDate inclusionDate;

    @Column(name = "coverage", precision = 18, scale = 2)
    private BigDecimal coverage;

    @Column(name = "is_hb")
    private Boolean isHB;
}
