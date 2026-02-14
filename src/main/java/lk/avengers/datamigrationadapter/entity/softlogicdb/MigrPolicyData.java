package lk.avengers.datamigrationadapter.entity.softlogicdb;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Migr_PolicyData")
public class MigrPolicyData {

    @Column(name = "PolicyId", length = 30)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    // Life Assured (LA) fields
    @Column(name = "LA_PolicyNo", length = 30)
    private String laPolicyNo;

    @Column(name = "LA_Title", length = 10)
    private String laTitle;

    @Column(name = "LA_FirstName", length = 100)
    private String laFirstName;

    @Column(name = "LA_LastName", length = 100)
    private String laLastName;

    @Column(name = "LA_Address", length = 100)
    private String laAddress;

    @Column(name = "LA_NIC", length = 20)
    private String laNic;

    @Column(name = "LA_Sex", length = 1)
    private String laSex;

    @Column(name = "LA_DOB")
    private LocalDate laDob;

    @Column(name = "LA_ANB")
    private Integer laAnb;

    @Column(name = "LA_NameWithInitials", length = 100)
    private String laNameWithInitials;

    @Column(name = "LA_Phone1", length = 10)
    private String laPhone1;

    @Column(name = "LA_Phone2", length = 10)
    private String laPhone2;

    @Column(name = "LA_Nationality", length = 20)
    private String laNationality;

    @Column(name = "LA_email", length = 30)
    private String laEmail;

    @Column(name = "LA_AgeAdmited")
    private Boolean laAgeAdmitted;

    @Column(name = "LA_AddressCity", length = 20)
    private String laAddressCity;

    @Column(name = "LA_Occupation", length = 30)
    private String laOccupation;

    @Column(name = "LA_monthlyIncome", precision = 18, scale = 2)
    private BigDecimal laMonthlyIncome;

    @Column(name = "LA_Ext_natureof_Duties", length = 30)
    private String laExtNatureOfDuties;

    @Column(name = "LA_Height")
    private Integer laHeight;

    @Column(name = "LA_PrefLanguage", length = 1)
    private String laPrefLanguage;

    @Column(name = "LA_Weight")
    private Integer laWeight;

    @Column(name = "LA_IsPolicyAssign")
    private Boolean laIsPolicyAssign = false;

    @Column(name = "LA_ProposalNo", length = 30)
    private String laProposalNo;

    // Policy (PO) fields
    @Column(name = "PO_plancode", length = 4)
    private String poPlanCode;

    @Column(name = "PO_PlanVersion", length = 10)
    private String poPlanVersion;

    @Column(name = "PO_term")
    private Integer poTerm;

    @Column(name = "PO_PaymentTerm")
    private Integer poPaymentTerm;

    @Column(name = "PO_DateofProposal")
    private LocalDate poDateOfProposal;

    @Column(name = "PO_BSA", precision = 18, scale = 2)
    private BigDecimal poBsa;

    @Column(name = "PO_SumAtRisk", precision = 18, scale = 2)
    private BigDecimal poSumAtRisk;

    @Column(name = "PO_BasicPremium", precision = 18, scale = 2)
    private BigDecimal poBasicPremium;

    @Column(name = "PO_PremiumType", length = 10)
    private String poPremiumType;

    @Column(name = "PO_AdvCode", length = 10)
    private String poAdvCode;

    @Column(name = "PO_BeginDate")
    private LocalDate poBeginDate;

    @Column(name = "PO_PolicyYear")
    private Integer poPolicyYear;

    @Column(name = "PO_DateUnderwritten")
    private LocalDate poDateUnderwritten;

    @Column(name = "PO_PremiumDueDate")
    private LocalDate poPremiumDueDate;

    @Column(name = "PO_Mode", length = 4)
    private String poMode;

    @Column(name = "PO_PolicyStatusCode", length = 4)
    private String poPolicyStatusCode;

    @Column(name = "PO_ExpirationDate")
    private LocalDate poExpirationDate;

    @Column(name = "PO_BranchCode", length = 2)
    private String poBranchCode;

    @Column(name = "PO_Premium", precision = 18, scale = 2)
    private BigDecimal poPremium;

    @Column(name = "PO_IllusMatuValue", precision = 18, scale = 2)
    private BigDecimal poIllusMatuValue;

    @Column(name = "PO_adminFee", precision = 18, scale = 2)
    private BigDecimal poAdminFee;

    // Spouse (SP) fields
    @Column(name = "SP_Title", length = 10)
    private String spTitle;

    @Column(name = "SP_FirstName", length = 100)
    private String spFirstName;

    @Column(name = "SP_LastName", length = 100)
    private String spLastName;

    @Column(name = "SP_NIC", length = 20)
    private String spNic;

    @Column(name = "SP_Sex", length = 1)
    private String spSex;

    @Column(name = "SP_DOB")
    private LocalDate spDob;

    @Column(name = "SP_ANB")
    private Integer spAnb;

    @Column(name = "SP_AgeAdmited")
    private Boolean spAgeAdmitted;

    @Column(name = "SP_Height")
    private Integer spHeight;

    @Column(name = "SP_Weight")
    private Integer spWeight;

    @Column(name = "SP_Occupation", length = 30)
    private String spOccupation;
}
