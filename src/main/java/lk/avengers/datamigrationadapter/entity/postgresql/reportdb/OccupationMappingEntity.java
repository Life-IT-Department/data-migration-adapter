package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "occupation_mapping")
public class OccupationMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "adb")
    private Double adb;

    @Column(name = "ims_code", nullable = false)
    private Integer imsCode;

    @Column(name = "ims_occupation", nullable = false, length = 255)
    private String imsOccupation;

    @Column(name = "life")
    private Double life;

    @Column(name = "sl_code", nullable = false)
    private Integer slCode;

    @Column(name = "sl_occupation", nullable = false, length = 255)
    private String slOccupation;

    @Column(name = "sli_class")
    private Double sliClass;

    @Column(name = "tpd_and_ppd")
    private Double tpdAndPpd;

    @Column(name = "wop")
    private Double wop;
}