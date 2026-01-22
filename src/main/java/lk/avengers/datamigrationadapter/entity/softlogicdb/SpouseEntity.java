package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "spouse")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "nic", length = 20, unique = true)
    private String nic;

    @Column(name = "sex", length = 10)
    private String sex;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "anb")
    private Integer anb;

    @Column(name = "age_admitted")
    private Integer ageAdmitted;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "occupation", length = 100)
    private String occupation;
}
