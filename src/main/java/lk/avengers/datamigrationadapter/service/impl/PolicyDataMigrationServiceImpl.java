package lk.avengers.datamigrationadapter.service.impl;

import jakarta.annotation.PostConstruct;
import lk.avengers.datamigrationadapter.dto.MigrPolicyDataDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.FundCurrentBalanceEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyData;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.FundCurrentBalanceEntityRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPolicyRepository;
import lk.avengers.datamigrationadapter.service.PolicyDataMigrationService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyDataMigrationServiceImpl implements PolicyDataMigrationService {

    private static final String DEFAULT_BRANCH_CODE = "6J";

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;
    private final ProductCodeMappingRepository productCodeMappingRepository;
    private final AdvisorCodeMappingRepository advisorCodeMappingRepository;
    private final BranchCodeMappingRepository branchCodeMappingRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final MigrPolicyRepository policyRepository;
    private final OccupationCodeMappingRepository occupationCodeMappingRepository;
    private final FundCurrentBalanceEntityRepository fundCurrentBalanceEntityRepository;
    private final PremiumDetailsRepository premiumDetailsRepository;

    private final MainExcelReader mainExcelReader;

    private List<ProductCodeMappingEntity> productCodeMappingList;
    private List<AdvisorCodeMappingEntity> advisorCodeMappingList;
    private List<BranchCodeMappingEntity> branchCodeMappingList;
    private List<OccupationMappingEntity> occupationMappingList;

    @PostConstruct
    public void init() {
        this.productCodeMappingList = productCodeMappingRepository.findAll();
        this.advisorCodeMappingList = advisorCodeMappingRepository.findAll();
        this.branchCodeMappingList = branchCodeMappingRepository.findAll();
        this.occupationMappingList = occupationCodeMappingRepository.findAll();

        log.info("Loaded {} PRODUCT_CODE_MAPPING records", productCodeMappingList.size());
        log.info("Loaded {} ADVISOR_CODE_MAPPING records", advisorCodeMappingList.size());
        log.info("Loaded {} BRANCH_CODE_MAPPING records", branchCodeMappingList.size());
        log.info("Loaded {} OCCUPATION_CODE_MAPPING records", occupationMappingList.size());
    }

    private final List<MigrPolicyDataDTO> requestDTOList = new ArrayList<>();
    private final List<FundCurrentBalanceEntity> fundCurrentBalanceEntityList = new ArrayList<>();

    @Override
    public void migratePolicyData() {

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        policyList.forEach(policy -> {
            Object policyEntity = findPolicyInRepositories(policy);

            LocalDate premiumDueDate = null;
            String productCode =
                    (policy != null && policy.length() >= 3)
                            ? policy.replace("/", "").substring(0, 3)
                            : null;
            Integer number = (policy != null && policy.length() >= 3)
                    ? Integer.valueOf(policy.replace("/", "").substring(3))
                    : null;

            Optional<PremiumDetailsEntity> premiumDetailsEntityOptional = premiumDetailsRepository.findFirstByPolicyNoAndProductCodeOrderByIdDesc(number, productCode);
            if(premiumDetailsEntityOptional.isPresent()){
                premiumDueDate = premiumDetailsEntityOptional.get().getPremiumDueDate();
            }

            switch (policyEntity) {
                case MainDataReportEntity mainDataReport ->
                    // Use mainDataReport with full type safety
                        processMainDataReport(mainDataReport, policy, premiumDueDate);
                case MainDataALHReportEntity alhReport ->
                    // Use alhReport with full type safety
                        processALHReport(alhReport, policy, premiumDueDate);
                case ACPPolicyEntity acpPolicy ->
                    // Use acpPolicy with full type safety
                        processACPPolicy(acpPolicy, policy, premiumDueDate);
                case null -> log.error("Policy {} not found in any repository", policy);
                default -> {
                }
            }

        });
        if (!requestDTOList.isEmpty()) {
            List<MigrPolicyData> policyEntityList = requestDTOList.stream()
                    .map(dto -> dto.mapData(MigrPolicyData.class))
                    .toList();
            log.info("Existing table truncating in MSSQL DB...");
            policyRepository.truncateTable();
            fundCurrentBalanceEntityRepository.truncate();
            log.info("Saving data into MSSQL DB...");
            policyRepository.saveAll(policyEntityList);
            fundCurrentBalanceEntityRepository.saveAll(fundCurrentBalanceEntityList);
            log.info("Saved {} policies to the MSSQL DB", policyEntityList.size());
        }
        log.info("Migration completed");
    }

    private void processACPPolicy(ACPPolicyEntity acpPolicy, String policyNo, LocalDate premiumDueDate) {

        if (!isEligiblePolicyStatus(acpPolicy.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, acpPolicy.getStatus());
            return;
        }
        MigrPolicyDataDTO policyRequestDTO = new MigrPolicyDataDTO();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(acpPolicy.getPlanNo());
        policyRequestDTO.setPoTerm(acpPolicy.getTerm());
        policyRequestDTO.setPoDateOfProposal(acpPolicy.getInception());
        policyRequestDTO.setPoPaymentTerm(setPaymentTerm(acpPolicy.getPremiumPaymentTerm()));
        policyRequestDTO.setPoBsa(acpPolicy.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(acpPolicy.getDthSar());
        policyRequestDTO.setPoBasicPremium(acpPolicy.getInsuredModalPremium());
        policyRequestDTO.setPoPremiumType(getPremiumType(acpPolicy.getPremiumPaymentTerm()));
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(acpPolicy.getAgentCode()));
        policyRequestDTO.setPoBeginDate(acpPolicy.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(acpPolicy.getInception()));
        policyRequestDTO.setPoDateUnderwritten(acpPolicy.getInception());
        policyRequestDTO.setPoPremiumDueDate(premiumDueDate != null ? premiumDueDate : acpPolicy.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(Integer.parseInt(acpPolicy.getFrequency())));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(acpPolicy.getStatus(), acpPolicy.getLastPremiumDueDate()));
        policyRequestDTO.setPoExpirationDate(acpPolicy.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(Integer.parseInt(acpPolicy.getSalesBranchCode())));
        policyRequestDTO.setPoPremium(acpPolicy.getInsuredModalPremium());
        policyRequestDTO.setPoAdminFee(BigDecimal.ZERO);
        policyRequestDTO.setPoIllusMatuValue(BigDecimal.ZERO);
        // ===== Life Assured (LA) =====
        ContactDetailEntity contact = getContactDetailEntity(policyNo);
        if (contact != null) {
            policyRequestDTO.setLaPolicyNo(policyNo);
            policyRequestDTO.setLaTitle(contact.getTitle());
            policyRequestDTO.setLaFirstName(contact.getFirstName());
            policyRequestDTO.setLaLastName(contact.getLastName());
            policyRequestDTO.setLaAddress(contact.getAddress());
            policyRequestDTO.setLaNic(contact.getNicNumber());
            policyRequestDTO.setLaSex(getSexChar(contact.getGender()));
            policyRequestDTO.setLaDob(contact.getDateOfBirth());
            policyRequestDTO.setLaPhone1(getTenDigitMobile(contact.getMobile()));
            policyRequestDTO.setLaPhone2(getOtherTelephoneNumber(contact.getOtherTelephoneNumber()));
            policyRequestDTO.setLaNationality(contact.getNationality().toUpperCase());
            policyRequestDTO.setLaEmail(getValidatedEmail(contact.getEmailAddress()));
            policyRequestDTO.setLaAgeAdmitted(false);
            policyRequestDTO.setLaAddressCity(extractAddressCity(contact.getCity()));
            policyRequestDTO.setLaOccupation(String.valueOf(getOccupation(contact.getOccupation())));
//            policyRequestDTO.setLaAnb(Integer.parseInt(getAdmittedAge(acpPolicy.getInception(), contact.getDateOfBirth()).toString()));
            policyRequestDTO.setLaAnb(acpPolicy.getAae());
            policyRequestDTO.setLaNameWithInitials(getNameWithInitials(contact.getFirstName(), contact.getLastName()));
            policyRequestDTO.setLaIsPolicyAssign(false);
            policyRequestDTO.setLaWeight(0);
            policyRequestDTO.setLaHeight(0);

            policyRequestDTO.setLaHbc(BigDecimal.ZERO);
            policyRequestDTO.setLaInpc(BigDecimal.ZERO);
            policyRequestDTO.setLaBonus(BigDecimal.ZERO);
        } else {
            log.error("Contact details not found for policy {}", policyNo);
        }

        // ===== No Spouse (SP) =====

        requestDTOList.add(policyRequestDTO);
    }

    public String getNameWithInitials(String firstName, String lastName) {

        if (firstName == null) firstName = "";
        if (lastName == null) lastName = "";

        firstName = firstName.trim();
        lastName = lastName.trim();

        if (lastName.isEmpty()) {
            return "";
        }

        StringBuilder initials = new StringBuilder();

        for (String word : firstName.split("\\s+")) {
            if (!word.isBlank()) {
                initials.append(Character.toUpperCase(word.charAt(0))).append(".");
            }
        }

        String[] lastNameParts = lastName.split("\\s+");

        if (lastNameParts.length == 1) {
            return initials + lastNameParts[0];
        }

        for (int i = 0; i < lastNameParts.length - 1; i++) {
            if (!lastNameParts[i].isBlank()) {
                initials.append(Character.toUpperCase(lastNameParts[i].charAt(0))).append(".");
            }
        }

        String finalSurname = lastNameParts[lastNameParts.length - 1];

        return initials + " " + finalSurname;
    }

    private Integer setPaymentTerm(String paymentTerm) {

        if (paymentTerm == null || paymentTerm.isBlank()) {
            return null; // or throw exception depending on your logic
        }

        paymentTerm = paymentTerm.trim();

        if (paymentTerm.equalsIgnoreCase("SP")) {
            return 1;
        }

        try {
            // Parse as BigDecimal to support decimals
            BigDecimal value = new BigDecimal(paymentTerm);
            return value.intValue();  // removes decimal part safely
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid payment term: " + paymentTerm, ex);
        }
    }

    private void processMainDataReport(MainDataReportEntity mainDataReport, String policyNo, LocalDate premiumDueDate) {

        if (!isEligiblePolicyStatus(mainDataReport.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, mainDataReport.getStatus());
            return;
        }

        MigrPolicyDataDTO policyRequestDTO = new MigrPolicyDataDTO();
        FundCurrentBalanceEntity fundCurrentBalanceEntity = new FundCurrentBalanceEntity();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(mainDataReport.getPlanNo());
        policyRequestDTO.setPoTerm(mainDataReport.getTerm());
        policyRequestDTO.setPoDateOfProposal(mainDataReport.getInception());
        policyRequestDTO.setPoPaymentTerm(setPaymentTerm(mainDataReport.getPremiumPaymentTerm())); // dispute
        policyRequestDTO.setPoBsa(mainDataReport.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(mainDataReport.getDth_Sar());
        policyRequestDTO.setPoBasicPremium(mainDataReport.getModalPremium());
        policyRequestDTO.setPoPremiumType(getPremiumType(mainDataReport.getPremiumPaymentTerm())); // dispute
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(mainDataReport.getAgentCode()));
        policyRequestDTO.setPoBeginDate(mainDataReport.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(mainDataReport.getInception()));
        policyRequestDTO.setPoDateUnderwritten(mainDataReport.getInception());
        policyRequestDTO.setPoPremiumDueDate(premiumDueDate != null ? premiumDueDate : mainDataReport.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(mainDataReport.getFrequency()));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(mainDataReport.getStatus(), mainDataReport.getLastPremiumDueDate()));
        policyRequestDTO.setPoExpirationDate(mainDataReport.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(mainDataReport.getSalesBranchCode()));
        policyRequestDTO.setPoPremium(mainDataReport.getModalPremium());
        policyRequestDTO.setPoAdminFee(BigDecimal.ZERO);
        policyRequestDTO.setPoIllusMatuValue(BigDecimal.ZERO);
        // ===== Life Assured (LA) =====
        ContactDetailEntity contact = getContactDetailEntity(policyNo);
        policyRequestDTO.setLaPolicyNo(policyNo);
        if (contact != null) {
            policyRequestDTO.setLaTitle(contact.getTitle());
            policyRequestDTO.setLaFirstName(contact.getFirstName());
            policyRequestDTO.setLaLastName(contact.getLastName());
            policyRequestDTO.setLaAddress(contact.getAddress());
            policyRequestDTO.setLaNic(contact.getNicNumber());
            policyRequestDTO.setLaSex(getSexChar(contact.getGender()));
            policyRequestDTO.setLaDob(contact.getDateOfBirth());
            policyRequestDTO.setLaPhone1(getTenDigitMobile(contact.getMobile()));
            policyRequestDTO.setLaPhone2(getOtherTelephoneNumber(contact.getOtherTelephoneNumber()));
            policyRequestDTO.setLaNationality(contact.getNationality().toUpperCase());
            policyRequestDTO.setLaEmail(getValidatedEmail(contact.getEmailAddress()));
            policyRequestDTO.setLaAgeAdmitted(false);
            policyRequestDTO.setLaAddressCity(extractAddressCity(contact.getCity()));
            policyRequestDTO.setLaOccupation(String.valueOf(getOccupation(contact.getOccupation())));
//            policyRequestDTO.setLaAnb(Integer.parseInt(getAdmittedAge(mainDataReport.getInception(), contact.getDateOfBirth()).toString()));
            policyRequestDTO.setLaAnb(mainDataReport.getAae());
            policyRequestDTO.setLaPrefLanguage(getLanguageChar(contact.getLanguagePreference()));
            policyRequestDTO.setLaNameWithInitials(getNameWithInitials(contact.getFirstName(), contact.getLastName()));
            policyRequestDTO.setLaIsPolicyAssign(false);
            policyRequestDTO.setLaWeight(0);
            policyRequestDTO.setLaHeight(0);

            policyRequestDTO.setLaHbc(BigDecimal.ZERO);
            policyRequestDTO.setLaInpc(BigDecimal.ZERO);
            policyRequestDTO.setLaBonus(BigDecimal.ZERO);
        } else {
            log.error("Contact details not found for policy {}", policyNo);
        }
        // ===== Spouse (SP) =====
        String spouseName = mainDataReport.getSpouseChildFullName();
        if (spouseName != null && !spouseName.trim().isEmpty()) {
            policyRequestDTO.setSpTitle(mainDataReport.getSpouseChildTitle());
            policyRequestDTO.setSpFirstName(mainDataReport.getSpouseChildFullName());
            policyRequestDTO.setSpSex(getSexChar(mainDataReport.getSpouseChildGender()));
            policyRequestDTO.setSpDob(mainDataReport.getSpouseChildDob());
            policyRequestDTO.setSpAnb(mainDataReport.getSpouseChildAge());
            policyRequestDTO.setSpAgeAdmitted(false);
            policyRequestDTO.setSpHeight(0);
            policyRequestDTO.setSpWeight(0);
        } else {
            policyRequestDTO.setSpSex("");
            policyRequestDTO.setSpAnb(0);
            policyRequestDTO.setSpAgeAdmitted(false);
            policyRequestDTO.setSpHeight(0);
            policyRequestDTO.setSpWeight(0);
        }

        fundCurrentBalanceEntity.setPolicyNo(policyNo);
        fundCurrentBalanceEntity.setTotalBalance(mainDataReport.getValueToday());
        fundCurrentBalanceEntity.setTopupBalance(mainDataReport.getBstValueToday());
        fundCurrentBalanceEntity.setPrmValueToday(mainDataReport.getPrmValueToday());

        fundCurrentBalanceEntityList.add(fundCurrentBalanceEntity);

        requestDTOList.add(policyRequestDTO);
    }

    private void processALHReport(MainDataALHReportEntity alhReport, String policyNo, LocalDate premiumDueDate) {

        if (!isEligiblePolicyStatus(alhReport.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, alhReport.getStatus());
            return;
        }

        MigrPolicyDataDTO policyRequestDTO = new MigrPolicyDataDTO();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(alhReport.getPlanNo());
        policyRequestDTO.setPoTerm(alhReport.getTerm());
        policyRequestDTO.setPoDateOfProposal(alhReport.getInception());
        policyRequestDTO.setPoPaymentTerm(alhReport.getPremiumPaymentTerm());
        policyRequestDTO.setPoBsa(alhReport.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(alhReport.getDth_Sar());
        policyRequestDTO.setPoBasicPremium(alhReport.getModalPremium());
        policyRequestDTO.setPoPremiumType("Regular");
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(alhReport.getAgentCode()));
        policyRequestDTO.setPoBeginDate(alhReport.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(alhReport.getInception()));
        policyRequestDTO.setPoDateUnderwritten(alhReport.getInception());
        policyRequestDTO.setPoPremiumDueDate(premiumDueDate != null ? premiumDueDate : alhReport.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(alhReport.getFrequency()));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(alhReport.getStatus(), alhReport.getLastPremiumDueDate()));
        policyRequestDTO.setPoExpirationDate(alhReport.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(alhReport.getSalesBranchCode()));
        policyRequestDTO.setPoPremium(alhReport.getModalPremium());
        policyRequestDTO.setPoAdminFee(BigDecimal.ZERO);
        policyRequestDTO.setPoIllusMatuValue(BigDecimal.ZERO);
        // ===== Life Assured (LA) =====
        ContactDetailEntity contact = getContactDetailEntity(policyNo);
        if (contact != null) {
            policyRequestDTO.setLaPolicyNo(policyNo);
            policyRequestDTO.setLaTitle(contact.getTitle());
            policyRequestDTO.setLaFirstName(contact.getFirstName());
            policyRequestDTO.setLaLastName(contact.getLastName());
            policyRequestDTO.setLaAddress(contact.getAddress());
            policyRequestDTO.setLaNic(contact.getNicNumber());
            policyRequestDTO.setLaSex(getSexChar(contact.getGender()));
            policyRequestDTO.setLaDob(contact.getDateOfBirth());
            policyRequestDTO.setLaPhone1(getTenDigitMobile(contact.getMobile()));
            policyRequestDTO.setLaPhone2(getOtherTelephoneNumber(contact.getOtherTelephoneNumber()));
            policyRequestDTO.setLaNationality(contact.getNationality().toUpperCase());
            policyRequestDTO.setLaEmail(getValidatedEmail(contact.getEmailAddress()));
            policyRequestDTO.setLaAgeAdmitted(false);
            policyRequestDTO.setLaAddressCity(extractAddressCity(contact.getCity()));
            policyRequestDTO.setLaOccupation(String.valueOf(getOccupation(contact.getOccupation())));
//            policyRequestDTO.setLaAnb(Integer.parseInt(getAdmittedAge(alhReport.getInception(), contact.getDateOfBirth()).toString()));
            policyRequestDTO.setLaAnb(alhReport.getAae());
            policyRequestDTO.setLaPrefLanguage(getLanguageChar(contact.getLanguagePreference()));
            policyRequestDTO.setLaNameWithInitials(getNameWithInitials(contact.getFirstName(), contact.getLastName()));
            policyRequestDTO.setLaIsPolicyAssign(false);
            policyRequestDTO.setLaWeight(0);
            policyRequestDTO.setLaHeight(0);

            policyRequestDTO.setLaHbc(alhReport.getHb_Sa());
            policyRequestDTO.setLaInpc(alhReport.getInpSar());
            policyRequestDTO.setLaBonus(alhReport.getMlBonus());
        } else {
            log.error("Contact details not found for policy {}", policyNo);
        }
        // ===== Spouse (SP) =====
        String spouseName = alhReport.getSpouseFullName();
        if (spouseName != null && !spouseName.trim().isEmpty()) {
            policyRequestDTO.setSpTitle(alhReport.getSpouseTitle());
            policyRequestDTO.setSpFirstName(alhReport.getSpouseFullName());
            policyRequestDTO.setSpSex(getSexChar(alhReport.getSpouseGender()));
            policyRequestDTO.setSpDob(alhReport.getSpouseDob());
            policyRequestDTO.setSpAnb(alhReport.getSpouseAge());
            policyRequestDTO.setSpAgeAdmitted(false);
            policyRequestDTO.setSpHeight(0);
            policyRequestDTO.setSpWeight(0);
        } else {
            policyRequestDTO.setSpSex("");
            policyRequestDTO.setSpAnb(0);
            policyRequestDTO.setSpAgeAdmitted(false);
            policyRequestDTO.setSpHeight(0);
            policyRequestDTO.setSpWeight(0);
        }

        requestDTOList.add(policyRequestDTO);
    }

    private boolean isEligiblePolicyStatus(String status) {

        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("In Force")
                || "Lapsed".equalsIgnoreCase(status);
    }


    private String getLanguageChar(String languagePreference) {
        if (languagePreference == null || languagePreference.isBlank()) {
            return "N";
        }
        return switch (languagePreference.trim().toLowerCase()) {
            case "english" -> "E";
            case "sinhala" -> "S";
            case "tamil" -> "T";
            default -> "N";
        };
    }


    private String getPremiumType(String premiumPaymentTerm) {
        if (premiumPaymentTerm == null || premiumPaymentTerm.isBlank()) {
            return "None";
        }

        // SP → Single
        if (premiumPaymentTerm.equalsIgnoreCase("SP")) {
            return "Single";
        }

        // Any numeric value → Regular (10, 10.0, 15.0)
        try {
            Double.parseDouble(premiumPaymentTerm);
            return "Regular";
        } catch (NumberFormatException e) {
            // Not a number
        }

        return "None";
    }

    private String getTenDigitMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        // Remove any spaces just in case
        String sanitized = mobile.trim();
        // Only digits allowed
        if (!sanitized.matches("\\d+")) {
            return null;
        }
        int length = sanitized.length();

        if (length == 9) {
            // Add 0 at the front
            return "0" + sanitized;
        } else if (length == 10 && sanitized.startsWith("0")) {
            // Already valid
            return sanitized;
        } else {
            // Invalid number (too short or too long)
            return null;
        }
    }

    private String getSexChar(String gender) {
        if (gender.equals("Female")) {
            return "F";
        } else if (gender.equals("Male")) {
            return "M";
        }
        return "T";
    }

    private Integer getAdmittedAge(LocalDate inception, LocalDate dateOfBirth) {
        if (inception == null || dateOfBirth == null) {
            return null;
        }

        return Period.between(dateOfBirth, inception).getYears();
    }


    private String getValidatedEmail(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            return null;
        }
        String sanitized = emailAddress.trim();
        // Basic regex for email validation
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (sanitized.matches(emailRegex)) {
            return sanitized;
        }
        // invalid email
        return null;
    }

    private String getOtherTelephoneNumber(String otherTelephoneNumber) {
        if (otherTelephoneNumber == null || otherTelephoneNumber.trim().isEmpty()) {
            return null;
        }
        // Remove spaces just in case
        String sanitized = otherTelephoneNumber.trim();
        // Regex: starts with 0, followed by 9 digits (0-9), total 10 digits
        if (sanitized.matches("0\\d{9}")) {
            return sanitized;
        }
        // Invalid number
        return null;
    }

    private ContactDetailEntity getContactDetailEntity(String policyNo) {
        if (policyNo == null || policyNo.trim().isEmpty()) {
            return null;
        }
        PolicyNumberResponseDTO policyNumber = extractPolicyNumber(policyNo);

        return contactDetailRepository
                .findByProductAndPolicyNo(
                        policyNumber.getProductCode(),
                        policyNumber.getPolicyNo().toString()
                )
                .orElse(null);
    }

    private String getBranchCodeMapping(Integer salesBranchCode) {
        if (salesBranchCode == null) {
            return null;
        }
        return branchCodeMappingList.stream()
                .filter(mapping -> mapping.getAllianzbranchcode() != null && salesBranchCode.equals(mapping.getAllianzbranchcode()))
                .map(BranchCodeMappingEntity::getSlbranchcode)
                .findFirst()
                .orElse(DEFAULT_BRANCH_CODE);
    }

    private String getPolicyStatusCode(String status, LocalDate lastPremiumDueDate) {
        if (status == null || lastPremiumDueDate == null) {
            return "NONE";
        }

        return switch (status.trim().toUpperCase()) {
            case "IN FORCE" -> "INFC";
            case "LAPSED" -> {
                LocalDate today = LocalDate.now();
                yield lastPremiumDueDate.isBefore(today.minusMonths(7))
                        ? "ALAP"
                        : "TLAP";
            }
            default -> "NONE";
        };
    }

    private Integer getOccupation(String allianzOccupation){
        if(allianzOccupation == null){
            return null;
        }
        return occupationMappingList.stream()
                .filter(occupationMappingEntity -> occupationMappingEntity.getImsOccupation().trim().equalsIgnoreCase(allianzOccupation))
                .map(OccupationMappingEntity::getSlCode)
                .findFirst()
                .orElse(0);
    }

    private String getFrequencyString(Integer frequency) {
        if (frequency == null) {
            return null;
        }

        return switch (frequency) {
            case 1, 5 -> "YRL";
            case 2 -> "HYR";
            case 3 -> "QRT";
            case 4 -> "MNT";
            default -> null;
        };
    }

    private Integer getPolicyYear(LocalDate inception) {
        if (inception == null) {
            return null;
        }
        return inception.getYear();
    }


    private String getAgentCodeMapping(String agentCode) {

        if (agentCode == null || agentCode.isBlank()) {
            return null;
        }

        List<String> advCodes = advisorCodeMappingList.stream()
                .filter(m -> agentCode.equalsIgnoreCase(m.getCode()))
                .map(AdvisorCodeMappingEntity::getAdvCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (advCodes.isEmpty()) {
            return DEFAULT_BRANCH_CODE.concat("0");
        }

        if (advCodes.size() == 1) {
            return advCodes.getFirst();
        }

        return advCodes.stream()
                .map(code -> Map.entry(code, extractTrailingNumber(code)))
                .filter(e -> e.getValue() != null && e.getValue() > 100)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_BRANCH_CODE.concat("0"));
    }

    private Integer extractTrailingNumber(String code) {
        if (code == null) return null;

        code = code.trim();

        Matcher m = Pattern.compile("(\\d+)$").matcher(code);
        if (!m.find()) return null;

        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }



    private String getSoftLogicProductCodeMapping(String policyNo) {

        PolicyNumberResponseDTO policyNumber = extractPolicyNumber(policyNo);
        String productCode = policyNumber.getProductCode();

        return productCodeMappingList.stream()
                .filter(mapping -> mapping.getProductCode().equalsIgnoreCase(productCode))
                .map(ProductCodeMappingEntity::getSlProductCode)
                .findFirst()
                .orElse(null);
    }


    private Object findPolicyInRepositories(String policyNumber) {

        PolicyNumberResponseDTO extracted = extractPolicyNumber(policyNumber);
        String productCode = extracted.getProductCode();
        Integer policyNo = extracted.getPolicyNo();

        // 1. Try mainDataReportRepository
        Optional<MainDataReportEntity> mainDataReportOpt =
                mainDataReportRepository.findFirstByProductCodeAndPolicyNo(productCode, policyNo);
        if (mainDataReportOpt.isPresent()) {
            log.info("Policy {} found in MainDataReportRepository", policyNumber);
            return mainDataReportOpt.get();
        }
        // 2. Try mainDataALHReportRepository
        Optional<MainDataALHReportEntity> mainDataALHOpt =
                mainDataALHReportRepository.findFirstByProductCodeAndPolicyNo(productCode, policyNo);
        if (mainDataALHOpt.isPresent()) {
            log.info("Policy {} found in MainDataALHReportRepository", policyNumber);
            return mainDataALHOpt.get();
        }
        // 3. Try acpPolicyRepository
        Optional<ACPPolicyEntity> acpPolicyOpt =
                acpPolicyRepository.findFirstByProductCodeAndPolicyNo(productCode, policyNo.toString());
        if (acpPolicyOpt.isPresent()) {
            log.info("Policy {} found in ACPPolicyRepository", policyNumber);
            return acpPolicyOpt.get();
        }

        // Policy isn't found anywhere
        log.warn("Policy {} not found in any repository", policyNumber);
        return null;
    }


//    private PolicyNumberResponseDTO extractPolicyNumber(String policyRef) {
//
//        if (policyRef == null || !policyRef.contains("/")) {
//            throw new IllegalArgumentException("Invalid policy reference: " + policyRef);
//        }
//
//        String[] parts = policyRef.split("/");
//
//        if (parts.length != 2) {
//            throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
//        }
//
//        String productCode = parts[0].trim();
//        int policyNo;
//
//        try {
//            policyNo = Integer.parseInt(parts[1].trim());
//        } catch (NumberFormatException e) {
//            throw new IllegalArgumentException("Invalid policy number: " + parts[1], e);
//        }
//
//        return new PolicyNumberResponseDTO(productCode, policyNo);
//    }

    private PolicyNumberResponseDTO extractPolicyNumber(String policyRef) {

        if (policyRef == null || policyRef.isBlank()) {
            throw new IllegalArgumentException("Policy reference cannot be null or empty");
        }

        policyRef = policyRef.trim();

        String productCode;
        int policyNo;

        // Case 1: Format with slash (ASP/1082429)
        if (policyRef.contains("/")) {

            String[] parts = policyRef.split("/");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
            }

            productCode = parts[0].trim();

            try {
                policyNo = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid policy number: " + parts[1], e);
            }

        }
        // Case 2: Format without slash (ULF527879)
        else {

            // Expect: 3 letters + digits
            if (!policyRef.matches("[A-Z]{3}\\d+")) {
                throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
            }

            productCode = policyRef.substring(0, 3);

            try {
                policyNo = Integer.parseInt(policyRef.substring(3));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid policy number: " + policyRef, e);
            }
        }

        return new PolicyNumberResponseDTO(productCode, policyNo);
    }

    private String extractAddressCity(String addressCity){
        if (addressCity != null && addressCity.contains(",")) {
            try{
                return addressCity.split(",")[1].trim();
            } catch (IndexOutOfBoundsException ex){
                return addressCity;
            }
        }
        return addressCity;
    }
}
