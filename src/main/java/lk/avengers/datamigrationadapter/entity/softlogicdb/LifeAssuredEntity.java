package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "life_assured")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LifeAssuredEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no")
    private String policyNo;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "address")
    private String address;

    @Column(name = "nic")
    private String nic;

    @Column(name = "sex")
    private String sex;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "name_with_initials")
    private String nameWithInitials;

    @Column(name = "phone1")
    private String phone1;

    @Column(name = "phone2")
    private String phone2;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "email")
    private String email;

    @Column(name = "age_admited")
    private Integer ageAdmited;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "monthly_income")
    private Double monthlyIncome;

    @Column(name = "ext_nature_of_duties")
    private String extNatureOfDuties;

    @Column(name = "height")
    private Double height;

    @Column(name = "pref_language")
    private String prefLanguage;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "is_policy_assign")
    private Boolean isPolicyAssign;
}
