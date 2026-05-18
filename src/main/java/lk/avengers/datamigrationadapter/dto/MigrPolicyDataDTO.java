package lk.avengers.datamigrationadapter.dto;

import jakarta.persistence.Column;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrPolicyDataDTO {

    private Long id;

    // =========================
    // Life Assured (LA) fields
    // =========================
    private String laPolicyNo;
    private String laTitle;
    private String laFirstName;
    private String laLastName;
    private String laAddress;
    private String laNic;
    private String laSex;
    private LocalDate laDob;
    private Integer laAnb;
    private String laNameWithInitials;
    private String laPhone1;
    private String laPhone2;
    private String laNationality;
    private String laEmail;
    private Boolean laAgeAdmitted;
    private String laAddressCity;
    private String laOccupation;
    private BigDecimal laMonthlyIncome;
    private String laExtNatureOfDuties;
    private Integer laHeight;
    private String laPrefLanguage;
    private Integer laWeight;
    private Boolean laIsPolicyAssign;
    private String laProposalNo;

    // =========================
    // Policy (PO) fields
    // =========================
    private String poPlanCode;
    private String poPlanVersion;
    private Integer poTerm;
    private Integer poPaymentTerm;
    private LocalDate poDateOfProposal;
    private BigDecimal poBsa;
    private BigDecimal poSumAtRisk;
    private BigDecimal poBasicPremium;
    private String poPremiumType;
    private String poAdvCode;
    private LocalDate poBeginDate;
    private Integer poPolicyYear;
    private LocalDate poDateUnderwritten;
    private LocalDate poPremiumDueDate;
    private String poMode;
    private String poPolicyStatusCode;
    private LocalDate poExpirationDate;
    private String poBranchCode;
    private BigDecimal poPremium;
    private BigDecimal poIllusMatuValue;
    private BigDecimal poAdminFee;

    // =========================
    // Spouse (SP) fields
    // =========================
    private String spTitle;
    private String spFirstName;
    private String spLastName;
    private String spNic;
    private String spSex;
    private LocalDate spDob;
    private Integer spAnb;
    private Boolean spAgeAdmitted;
    private Integer spHeight;
    private Integer spWeight;
    private String spOccupation;

    private BigDecimal laHbc;
    private BigDecimal laInpc;
    private BigDecimal laBonus;

    private Integer poInitialDefermentTerm;
    private Integer poInitialRetirementBenefitPayoutTerm;
    private Integer poInitialRetirementPayoutMode;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
