package lk.avengers.datamigrationadapter.entity.softlogicdb;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Migr_PremiumsDue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrPremiumsDue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DU_PolicyNo", length = 30)
    private String policyNo;

    @Column(name = "DU_DueDate")
    private LocalDate dueDate;

    @Column(name = "DU_Period")
    private Integer period;

    @Column(name = "DU_Term")
    private Integer term;

    @Column(name = "DU_PaidUp")
    private Boolean paidUp;

    @Column(name = "DU_DueAmount", precision = 18, scale = 2)
    private BigDecimal dueAmount;

    @Column(name = "DU_RowCreatedOn")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate rowCreatedOn;

    @Column(name = "DU_DueStatus", length = 3)
    private String dueStatus;

    @Column(name = "DU_PaidUpDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate paidUpDate;
}
