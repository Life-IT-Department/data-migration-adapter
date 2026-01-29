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
    private Double inpOccLoading;

    @Column(name = "ml_bonus")
    private BigDecimal mlBonus;

    // --- 2. NEW SPOUSE FIELDS ---
    @Column(name = "occupation_spouse")
    private String occupationSpouse;

    // Spouse Death & HB (Might overlap with old Schema, but naming varied slightly)
    @Column(name = "spouse_death_sa")
    private BigDecimal spouseDeathSa;
    @Column(name = "spouse_sub_death")
    private BigDecimal spouseSubDeath;
    @Column(name = "spouse_sub_rate_mil_death")
    private BigDecimal spouseSubRateMilDeath;
    @Column(name = "spouse_death_occupational_loading_percentage")
    private Double spouseDeathOccLoading;

    @Column(name = "spouse_hb_sa")
    private BigDecimal spouseHbSa;
    @Column(name = "spouse_sub_hb")
    private BigDecimal spouseSubHb;
    @Column(name = "spouse_sub_rate_mil_hb")
    private BigDecimal spouseSubRateMilHb;
    @Column(name = "spouse_hb_occupational_loading_percentage")
    private Double spouseHbOccLoading;

    @Column(name = "spouse_bonus")
    private BigDecimal spouseBonus;

    @Column(name = "spouse_inp_sar")
    private BigDecimal spouseInpSar;
    @Column(name = "spouse_sub_inp")
    private BigDecimal spouseSubInp;
    @Column(name = "spouse_sub_rate_mil_inp")
    private BigDecimal spouseSubRateMilInp;
    @Column(name = "spouse_inp_occupational_loading_percentage")
    private Double spouseInpOccLoading;


    // --- 3. CHILDREN (1 to 20) ---
    // Note: Since this file has 20 children and the other had 5,
    // it is cleaner to define them here rather than in the base class.




    // ... (REPEAT THIS PATTERN FOR CHILDREN 3 THROUGH 19) ...

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

    // --- 4. EXTRA FIELDS (If any differ from Base) ---
    @Column(name = "operation_date")
    private LocalDate operationDate;

    // -- audit --
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
