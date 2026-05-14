package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "main_data_alh_report")
@Setter
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
public class MainDataALHReportEntity extends BaseMainData{
    @Column(name = "occupation_main_life")
    private String occupationMainLife;
    @Column(name = "inp_sar")
    private BigDecimal inpSar;
    @Column(name = "sub_inp")
    private BigDecimal subInp;
    @Column(name = "sub_rate_mil_inp")
    private BigDecimal subRateMilInp;
    @Column(name = "inp_occupational_loading_percentage")
    private Double inpOccLoadingPercentage;
    @Column(name = "premium_payment_term")
    private Integer premiumPaymentTerm;
    @Column(name = "ml_bonus")
    private BigDecimal mlBonus;
    // ---  SPOUSE INFO ---
    @Column(name = "spouse_pin")
    private Integer spousePin;
    @Column(name = "spouse_title")
    private String spouseTitle;
    @Column(name = "spouse_full_name")
    private String spouseFullName;
    @Column(name = "spouse_gender")
    private String spouseGender;
    @Column(name = "spouse_dob")
    private LocalDate spouseDob;
    @Column(name = "spouse_age")
    private Integer spouseAge;
    @Column(name = "occupation_spouse")
    private String occupationSpouse;
    // Spouse Death
    @Column(name = "spouse_death_sa")
    private BigDecimal spouseDth_Sa;
    @Column(name = "spouse_sub_death")
    private Integer spouseSubDth_;
    @Column(name = "spouse_sub_rate_mil_death")
    private BigDecimal spouseSubRateMilDth_;
    @Column(name = "spouse_death_occupational_loading_percentage")
    private Double spouseDth_OccLoadingPercentage;
    // Spouse HB
    @Column(name = "spouse_hb_sa")
    private BigDecimal spouseHbSa;
    @Column(name = "spouse_sub_hb")
    private BigDecimal spouseSubHb;
    @Column(name = "spouse_sub_rate_mil_hb")
    private BigDecimal spouseSubRateMilHb;
    @Column(name = "spouse_hb_occupational_loading_percentage")
    private Double spouseHbOccLoadingPercentage;
    @Column(name = "spouse_bonus")
    private BigDecimal spouseBonus;
    @Column(name = "spouse_inp_sar")
    private BigDecimal spouseInpSar;
    @Column(name = "spouse_sub_inp")
    private BigDecimal spouseSubInp;
    @Column(name = "spouse_sub_rate_mil_inp")
    private BigDecimal spouseSubRateMilInp;
    @Column(name = "spouse_inp_occupational_loading_percentage")
    private Double spouseInpOccLoadingPercentage;
    // --- 3. CHILDREN (1 to 20) ---
    // --- CHILD 1 ---
    @Column(name = "child1_inp_sar")
    private BigDecimal child1InpSar;
    @Column(name = "child1_bonus")
    private BigDecimal child1Bonus;
    // --- CHILD 2 ---
    @Column(name = "child2_inp_sar")
    private BigDecimal child2InpSar;
    @Column(name = "child2_bonus")
    private BigDecimal child2Bonus;
    // --- CHILD 3 ---
    @Column(name = "child3_inp_sar")
    private BigDecimal child3InpSar;
    @Column(name = "child3_bonus")
    private BigDecimal child3Bonus;
    // --- CHILD 4 ---
    @Column(name = "child4_inp_sar")
    private BigDecimal child4InpSar;
    @Column(name = "child4_bonus")
    private BigDecimal child4Bonus;
    // --- CHILD 5 ---
    @Column(name = "child5_inp_sar")
    private BigDecimal child5InpSar;
    @Column(name = "child5_bonus")
    private BigDecimal child5Bonus;
    // --- CHILD 6 ---
    @Column(name = "child6_name")
    private String child6Name;
    @Column(name = "child6_dob")
    private LocalDate child6Dob;
    @Column(name = "child6_age")
    private Integer child6Age;
    @Column(name = "child6_hbc")
    private BigDecimal child6Hbc;
    @Column(name = "child6_inp_sar")
    private BigDecimal child6InpSar;
    @Column(name = "child6_bonus")
    private BigDecimal child6Bonus;
    // --- CHILD 7 ---
    @Column(name = "child7_name")
    private String child7Name;
    @Column(name = "child7_dob")
    private LocalDate child7Dob;
    @Column(name = "child7_age")
    private Integer child7Age;
    @Column(name = "child7_hbc")
    private BigDecimal child7Hbc;
    @Column(name = "child7_inp_sar")
    private BigDecimal child7InpSar;
    @Column(name = "child7_bonus")
    private BigDecimal child7Bonus;
    // --- CHILD 8 ---
    @Column(name = "child8_name")
    private String child8Name;
    @Column(name = "child8_dob")
    private LocalDate child8Dob;
    @Column(name = "child8_age")
    private Integer child8Age;
    @Column(name = "child8_hbc")
    private BigDecimal child8Hbc;
    @Column(name = "child8_inp_sar")
    private BigDecimal child8InpSar;
    @Column(name = "child8_bonus")
    private BigDecimal child8Bonus;
    // --- CHILD 9 ---
    @Column(name = "child9_name")
    private String child9Name;
    @Column(name = "child9_dob")
    private LocalDate child9Dob;
    @Column(name = "child9_age")
    private Integer child9Age;
    @Column(name = "child9_hbc")
    private BigDecimal child9Hbc;
    @Column(name = "child9_inp_sar")
    private BigDecimal child9InpSar;
    @Column(name = "child9_bonus")
    private BigDecimal child9Bonus;
    // --- CHILD 10 ---
    @Column(name = "child10_name")
    private String child10Name;
    @Column(name = "child10_dob")
    private LocalDate child10Dob;
    @Column(name = "child10_age")
    private Integer child10Age;
    @Column(name = "child10_hbc")
    private BigDecimal child10Hbc;
    @Column(name = "child10_inp_sar")
    private BigDecimal child10InpSar;
    @Column(name = "child10_bonus")
    private BigDecimal child10Bonus;
    // --- CHILD 11 ---
    @Column(name = "child11_name")
    private String child11Name;
    @Column(name = "child11_dob")
    private LocalDate child11Dob;
    @Column(name = "child11_age")
    private Integer child11Age;
    @Column(name = "child11_hbc")
    private BigDecimal child11Hbc;
    @Column(name = "child11_inp_sar")
    private BigDecimal child11InpSar;
    @Column(name = "child11_bonus")
    private BigDecimal child11Bonus;
    // --- CHILD 12 ---
    @Column(name = "child12_name")
    private String child12Name;
    @Column(name = "child12_dob")
    private LocalDate child12Dob;
    @Column(name = "child12_age")
    private Integer child12Age;
    @Column(name = "child12_hbc")
    private BigDecimal child12Hbc;
    @Column(name = "child12_inp_sar")
    private BigDecimal child12InpSar;
    @Column(name = "child12_bonus")
    private BigDecimal child12Bonus;
    // --- CHILD 13 ---
    @Column(name = "child13_name")
    private String child13Name;
    @Column(name = "child13_dob")
    private LocalDate child13Dob;
    @Column(name = "child13_age")
    private Integer child13Age;
    @Column(name = "child13_hbc")
    private BigDecimal child13Hbc;
    @Column(name = "child13_inp_sar")
    private BigDecimal child13InpSar;
    @Column(name = "child13_bonus")
    private BigDecimal child13Bonus;
    // --- CHILD 14 ---
    @Column(name = "child14_name")
    private String child14Name;
    @Column(name = "child14_dob")
    private LocalDate child14Dob;
    @Column(name = "child14_age")
    private Integer child14Age;
    @Column(name = "child14_hbc")
    private BigDecimal child14Hbc;
    @Column(name = "child14_inp_sar")
    private BigDecimal child14InpSar;
    @Column(name = "child14_bonus")
    private BigDecimal child14Bonus;
    // --- CHILD 15 ---
    @Column(name = "child15_name")
    private String child15Name;
    @Column(name = "child15_dob")
    private LocalDate child15Dob;
    @Column(name = "child15_age")
    private Integer child15Age;
    @Column(name = "child15_hbc")
    private BigDecimal child15Hbc;
    @Column(name = "child15_inp_sar")
    private BigDecimal child15InpSar;
    @Column(name = "child15_bonus")
    private BigDecimal child15Bonus;
    // --- CHILD 16 ---
    @Column(name = "child16_name")
    private String child16Name;
    @Column(name = "child16_dob")
    private LocalDate child16Dob;
    @Column(name = "child16_age")
    private Integer child16Age;
    @Column(name = "child16_hbc")
    private BigDecimal child16Hbc;
    @Column(name = "child16_inp_sar")
    private BigDecimal child16InpSar;
    @Column(name = "child16_bonus")
    private BigDecimal child16Bonus;
    // --- CHILD 17 ---
    @Column(name = "child17_name")
    private String child17Name;
    @Column(name = "child17_dob")
    private LocalDate child17Dob;
    @Column(name = "child17_age")
    private Integer child17Age;
    @Column(name = "child17_hbc")
    private BigDecimal child17Hbc;
    @Column(name = "child17_inp_sar")
    private BigDecimal child17InpSar;
    @Column(name = "child17_bonus")
    private BigDecimal child17Bonus;
    // --- CHILD 18 ---
    @Column(name = "child18_name")
    private String child18Name;
    @Column(name = "child18_dob")
    private LocalDate child18Dob;
    @Column(name = "child18_age")
    private Integer child18Age;
    @Column(name = "child18_hbc")
    private BigDecimal child18Hbc;
    @Column(name = "child18_inp_sar")
    private BigDecimal child18InpSar;
    @Column(name = "child18_bonus")
    private BigDecimal child18Bonus;
    // --- CHILD 19 ---
    @Column(name = "child19_name")
    private String child19Name;
    @Column(name = "child19_dob")
    private LocalDate child19Dob;
    @Column(name = "child19_age")
    private Integer child19Age;
    @Column(name = "child19_hbc")
    private BigDecimal child19Hbc;
    @Column(name = "child19_inp_sar")
    private BigDecimal child19InpSar;
    @Column(name = "child19_bonus")
    private BigDecimal child19Bonus;
    // --- CHILD 20 ---
    @Column(name = "child20_name")
    private String child20Name;
    @Column(name = "child20_dob")
    private LocalDate child20Dob;
    @Column(name = "child20_age")
    private Integer child20Age;
    @Column(name = "child20_hbc")
    private BigDecimal child20Hbc;
    @Column(name = "child20_inp_sar")
    private BigDecimal child20InpSar;
    @Column(name = "child20_bonus")
    private BigDecimal child20Bonus;

    // New DQ Fields
    // --- CHILD 1 GENDER ---
    @Column(name = "child1_gender")
    private String child1Gender;

    // --- CHILD 2 GENDER ---
    @Column(name = "child2_gender")
    private String child2Gender;

    // --- CHILD 3 GENDER ---
    @Column(name = "child3_gender")
    private String child3Gender;

    // --- CHILD 4 GENDER ---
    @Column(name = "child4_gender")
    private String child4Gender;

    // --- CHILD 5 GENDER ---
    @Column(name = "child5_gender")
    private String child5Gender;

    // --- CHILD 6 GENDER ---
    @Column(name = "child6_gender")
    private String child6Gender;

    // --- CHILD 7 GENDER ---
    @Column(name = "child7_gender")
    private String child7Gender;

    // --- CHILD 8 GENDER ---
    @Column(name = "child8_gender")
    private String child8Gender;

    // --- CHILD 9 GENDER ---
    @Column(name = "child9_gender")
    private String child9Gender;

    // --- CHILD 10 GENDER ---
    @Column(name = "child10_gender")
    private String child10Gender;

    // --- CHILD 11 GENDER ---
    @Column(name = "child11_gender")
    private String child11Gender;

    // --- CHILD 12 GENDER ---
    @Column(name = "child12_gender")
    private String child12Gender;

    // --- CHILD 13 GENDER ---
    @Column(name = "child13_gender")
    private String child13Gender;

    // --- CHILD 14 GENDER ---
    @Column(name = "child14_gender")
    private String child14Gender;

    // --- CHILD 15 GENDER ---
    @Column(name = "child15_gender")
    private String child15Gender;

    // --- CHILD 16 GENDER ---
    @Column(name = "child16_gender")
    private String child16Gender;

    // --- CHILD 17 GENDER ---
    @Column(name = "child17_gender")
    private String child17Gender;

    // --- CHILD 18 GENDER ---
    @Column(name = "child18_gender")
    private String child18Gender;

    // --- CHILD 19 GENDER ---
    @Column(name = "child19_gender")
    private String child19Gender;

    // --- CHILD 20 GENDER ---
    @Column(name = "child20_gender")
    private String child20Gender;

    // --- SPOUSE ID CARD NUMBER ---
    @Column(name = "spouse_id_card_number")
    private String spouseIdCardNumber;


    // -- audit --
    @CreatedDate
    @Column(name = "sys_date")
    private LocalDateTime sysDate;
}
