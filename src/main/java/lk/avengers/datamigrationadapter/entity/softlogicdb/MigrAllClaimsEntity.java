package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Migr_AllClaims")
public class MigrAllClaimsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CL_PolicyNo", length = 30)
    private String clPolicyNo;

    @Column(name = "CL_ClaimNo", length = 30)
    private String clClaimNo;

    @Column(name = "CL_ClaimType", length = 40)
    private String clClaimType;

    @Column(name = "CL_DateofEvent")
    private LocalDate clDateofEvent;

    @Column(name = "CL_DateofIntimation")
    private LocalDate clDateofIntimation;

    @Column(name = "CL_PatientAdmitted", length = 100)
    private String clPatientAdmitted;

    @Column(name = "CL_RelationShip", length = 10)
    private String clRelationShip;

    @Column(name = "CL_CauseofDeath", length = 255)
    private String clCauseofDeath;

    @Column(name = "CL_NatureofIllnuss", length = 255)
    private String clNatureofIllnuss;

    @Column(name = "CL_TotalClaimAmount", precision = 18, scale = 2)
    private BigDecimal clTotalClaimAmount;

    @Column(name = "CL_TotalSettledAmount", precision = 18, scale = 2)
    private BigDecimal clTotalSettledAmount;

    @Column(name = "CL_ClaimStatus", length = 1)
    private String clClaimStatus;

    @Column(name = "CL_NameOftheHospital", length = 255)
    private String clNameOftheHospital;

    @Column(name = "CL_DoneBy", length = 30)
    private String clDoneBy;

    @Column(name = "CL_TotalDays", precision = 9, scale = 2)
    private BigDecimal clTotalDays;

    @Column(name = "CL_DateofPayment")
    private LocalDate clDateofPayment;

    @Column(name = "CL_ApprovedBy", length = 30)
    private String clApprovedBy;

    @Column(name = "CL_ApprovedDate")
    private LocalDate clApprovedDate;

    @Column(name = "CL_PayMode", length = 10)
    private String clPayMode;

    @Column(name = "CL_Comments", columnDefinition = "TEXT")
    private String clComments;

    @Column(name = "CL_PayIns", length = 100)
    private String clPayIns;

    @Column(name = "CL_DisDate")
    private LocalDate clDisDate;

    @Column(name = "CL_PolicyYear")
    private Integer clPolicyYear;

    @Column(name = "CL_Child_Id")
    private String clChildId;
}
