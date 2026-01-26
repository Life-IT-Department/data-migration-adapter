package lk.avengers.datamigrationadapter.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ACPPolicyRequestDTO {

    // --- Identification & Policy Metadata ---
    private String masterPolicyNo;
    private String policyNo;
    private String certificateNo;
    private String proposalNo;
    private String productCode;
    private String planNo;
    private LocalDate inception;
    private LocalDate expiry;
    private LocalDate issueDate;
    private Integer term;
    private String cy;
    private String premiumPaymentTerm;
    private String status;
    private LocalDate date;
    private LocalDateTime operationDate;
    private String reason;

    // --- Sales & Branch Info ---
    private String salesBranchCode;
    private String salesBranchName;
    private String companyBranchCode;
    private String companyBranchName;
    private String policyBranchCode;
    private String policyBranchName;
    private String agentCode;
    private String introducer;
    private String supervisor;
    private BigDecimal riPercentage;

    // --- Contribution & Premium Details ---
    private String insuredContributionType;
    private BigDecimal insuredModalPremium;
    private BigDecimal companyModalPremium;
    private String frequency;
    private BigDecimal nextPremium;
    private String employerName;
    private String insuranceCategory;
    private BigDecimal minContributionPerc;
    private BigDecimal maxContributionPerc;
    private BigDecimal insuredPremiumInflation;

    // --- Personal Details ---
    private String pin;
    private String masterPin;
    private String title;
    private String fullName;
    private String gender;
    private LocalDate dob;
    private Integer aae;
    private String sarChoice;
    private Integer numberOfRidersTaken;

    // --- Death (DTH) Rider Details ---
    private BigDecimal dthSar;
    private String subDth;
    private BigDecimal subRateMilDth;
    private BigDecimal dthOccupationalLoadingPerc;
    private String dthOccupationClass;
    private BigDecimal dthInsuredCoiShare;

    // --- Accidental Death (ACCD) Rider Details ---
    private BigDecimal accdSa;
    private String subAccd;
    private BigDecimal subRateMilAccd;
    private BigDecimal accdOccupationalLoadingPerc;
    private String accdOccupationClass;
    private BigDecimal accdInsuredCoiShare;

    // --- Accidental Permanent (ACCP) Rider Details ---
    private BigDecimal accpSa;
    private String subAccp;
    private BigDecimal subRateMilAccp;
    private BigDecimal accpOccupationalLoadingPerc;
    private String accpOccupationClass;
    private BigDecimal accpInsuredCoiShare;

    // --- Accidental Total (ACCT) Rider Details ---
    private BigDecimal acctSa;
    private String subAcct;
    private BigDecimal subRateMilAcct;
    private BigDecimal acctOccupationalLoadingPerc;
    private String acctOccupationClass;
    private BigDecimal acctInsuredCoiShare;

    // --- Critical Illness (CILX) Rider Details ---
    private BigDecimal cilxSa;
    private String subCilx;
    private BigDecimal subRateMilCilx;
    private BigDecimal cilxOccupationalLoadingPerc;
    private String cilxOccupationClass;
    private BigDecimal cilxInsuredCoiShare;

    // --- Permanent Total Disability (PTD) Rider Details ---
    private BigDecimal ptdSa;
    private String subPtd;
    private BigDecimal subRateMilPtd;
    private BigDecimal ptdOccupationalLoadingPerc;
    private String ptdOccupationClass;
    private BigDecimal ptdInsuredCoiShare;

    // --- Basic Sums & Values ---
    private String basicSumInsuredFormula;
    private BigDecimal basicSumAssured;
    private BigDecimal basicSumAssuredInflation;
    private BigDecimal insuredValueToday;
    private BigDecimal unvestedPremiumValueToday;
    private BigDecimal vestedPremiumValueToday;
    private BigDecimal insuredTopupValueToday;
    private BigDecimal unvestedTopupValueToday;
    private BigDecimal vestedTopupValueToday;

    // --- Transaction Amounts ---
    private BigDecimal insuredTransactionAmount;
    private BigDecimal unvestedPremiumTransactionAmount;
    private BigDecimal vestedPremiumTransactionAmount;
    private BigDecimal insuredTopupTransactionAmount;
    private BigDecimal unvestedTopupTransactionAmount;
    private BigDecimal vestedTopupTransactionAmount;

    // --- Interest Credited ---
    private BigDecimal insuredInterestCredited;
    private BigDecimal unvestedPremiumInterestCredited;
    private BigDecimal vestedPremiumInterestCredited;
    private BigDecimal insuredTopupInterestCredited;
    private BigDecimal unvestedTopupInterestCredited;
    private BigDecimal vestedTopupInterestCredited;

    // --- Surrender Values ---
    private BigDecimal insuredSurrenderValue;
    private BigDecimal unvestedPremiumSurrenderValue;
    private BigDecimal vestedPremiumSurrenderValue;
    private BigDecimal insuredTopupSurrenderValue;
    private BigDecimal unvestedTopupSurrenderValue;
    private BigDecimal vestedTopupSurrenderValue;

    // --- Final Policy Attributes ---
    private String insuranceCoveragePeriod;
    private BigDecimal pacInsuredShare;
    private LocalDate lastPaymentDate;
    private LocalDate lastPremiumDueDate;
}