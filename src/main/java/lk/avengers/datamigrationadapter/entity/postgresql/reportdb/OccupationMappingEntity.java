package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "occupation_mapping")
public class OccupationMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "ims_code", nullable = false)
    private Integer imsCode;
    @Column(name = "ims_occupation", nullable = false)
    private String imsOccupation;
    @Column(name = "sl_code", nullable = false)
    private Integer slCode;
    @Column(name = "sl_occupation", nullable = false)
    private String slOccupation;
    private Double life;
    private Double adb;
    @Column(name = "tpd_and_ppd")
    private Double tpdAndPpd;
    private Double wop;
    @Column(name = "sli_class")
    private Double sliClass;
}
