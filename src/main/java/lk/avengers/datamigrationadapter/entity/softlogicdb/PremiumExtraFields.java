package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "Premium_Extra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumExtraFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PolicyNo", length = 30)
    private String policyNo;

    @Column(name = "InceptionDate")
    private LocalDate inceptionDate;

    @Column(name = "BeginDate")
    private LocalDate beginDate;

    @Column(name = "PaidCount")
    private Integer paidCount;
}
