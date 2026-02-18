package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class MigrPolicyBenefitsID implements Serializable {

    @Column(name = "PB_PolicyNo", length = 30)
    private String pbPolicyNo;

    @Column(name = "PB_BenefitCode", length = 4)
    private String pbBenefitCode;
}
