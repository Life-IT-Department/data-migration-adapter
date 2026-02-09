package lk.avengers.datamigrationadapter.service.impl;

import jakarta.annotation.PostConstruct;
import lk.avengers.datamigrationadapter.dto.request.PolicyRequestDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyRepository;
import lk.avengers.datamigrationadapter.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationServiceImpl implements MigrationService {

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;
    private final ProductCodeMappingRepository productCodeMappingRepository;
    private final AdvisorCodeMappingRepository advisorCodeMappingRepository;
    private final BranchCodeMappingRepository branchCodeMappingRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final PolicyRepository policyRepository;

    private final List<String> policyList = List.of("ASP/888636", "SCL/1044031", "ULT/348672", "ULE/402719", "ULE/121830", "ULE/274795",
            "ASP/1082429", "SCL/1098458", "ULP/349597", "ASP/1052257", "ULI/756262", "UPR/378844", "UPS/279539", "ULI/809491", "ULP/406538",
            "ULE/400218", "ULE/398701", "ULE/197178", "ULV/714360", "ULV/700310", "UPS/428565", "ASP/1104355", "SCL/1104348", "ULP/198457",
            "ULI/828517", "SCL/900886", "SCL/1013747", "ULI/441816", "ULC/863654", "ULA/242859", "SCL/953141", "SCL/953703", "SCL/960278",
            "SCL/911347", "SCL/955195", "SCL/964197", "SCL/911537", "SCL/955690", "SCL/936039", "SCL/904391", "SCL/922831", "SCL/949701",
            "SCL/925552", "SCL/943654", "SCL/957498", "SCL/913475", "SCL/950410", "SCL/908277", "SCL/914200", "SCL/924779", "SCL/931139",
            "SCL/944652", "ULT/361790", "ULT/362384", "ULT/365627", "ULT/372227", "ULT/371856", "ULT/366419", "ULT/371518", "ULT/373738",
            "ULT/366641", "ULV/896787", "ULV/901215", "ULV/894709", "ULE/360909", "ULE/351064", "ULE/366096", "ULE/373381", "ULE/373878",
            "ULE/358945", "SCL/963207", "SCL/964239", "SCL/924845", "SCL/945246", "SCL/897066", "SCL/908210", "SCL/919498", "SCL/926923",
            "ULT/326371", "ULT/323410", "ULT/323436", "ULT/326355", "ULT/322214", "ULT/325084", "ULT/322370", "ULT/326835", "ULT/326298",
            "ULT/327387", "ULT/323527", "ULT/325217", "ULT/325464", "ULT/325498", "ULT/326959", "ULT/325704", "ULT/326124", "SCL/838805",
            "SCL/834408", "SCL/873695", "SCL/846311", "SCL/893719");

    private List<ProductCodeMappingEntity> productCodeMappingList;
    private List<AdvisorCodeMappingEntity> advisorCodeMappingList;
    private List<BranchCodeMappingEntity> branchCodeMappingList;

    @PostConstruct
    public void init() {
        this.productCodeMappingList = productCodeMappingRepository.findAll();
        this.advisorCodeMappingList = advisorCodeMappingRepository.findAll();
        this.branchCodeMappingList = branchCodeMappingRepository.findAll();

        log.info("Loaded {} PRODUCT_CODE_MAPPING records", productCodeMappingList.size());
        log.info("Loaded {} ADVISOR_CODE_MAPPING records", advisorCodeMappingList.size());
        log.info("Loaded {} BRANCH_CODE_MAPPING records", branchCodeMappingList.size());
    }

    private final List<PolicyRequestDTO> requestDTOList = new ArrayList<>();

    @Override
    public void migratePolicyData(String uuid) {


        policyList.forEach(policy -> {
            Object policyEntity = findPolicyInRepositories(policy);

            if (policyEntity instanceof MainDataReportEntity mainDataReport) {
                // Use mainDataReport with full type safety
                processMainDataReport(mainDataReport, policy);

            } else if (policyEntity instanceof MainDataALHReportEntity alhReport) {
                // Use alhReport with full type safety
                processALHReport(alhReport, policy);

            } else if (policyEntity instanceof ACPPolicyEntity acpPolicy) {
                // Use acpPolicy with full type safety
                processACPPolicy(acpPolicy, policy);

            } else if (policyEntity == null) {
                log.error("Policy {} not found in any repository", policy);
            }

        });
        if (!requestDTOList.isEmpty()) {
            List<PolicyEntity> policyEntityList = requestDTOList.stream()
                    .map(dto -> dto.mapData(PolicyEntity.class))
                    .toList();
            log.info("Existing table truncating in MSSQL DB...");
            policyRepository.truncateTable();
            log.info("Saving data into MSSQL DB...");
            policyRepository.saveAll(policyEntityList);
            log.info("Saved {} policies to the MSSQL DB", policyEntityList.size());
        }
        log.info("Migration completed for policy {}", uuid);
    }

    private void processACPPolicy(ACPPolicyEntity acpPolicy, String policyNo) {

        if (!isEligiblePolicyStatus(acpPolicy.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, acpPolicy.getStatus());
            return;
        }

        PolicyRequestDTO policyRequestDTO = new PolicyRequestDTO();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(acpPolicy.getPlanNo());
        policyRequestDTO.setPoTerm(acpPolicy.getTerm());
        policyRequestDTO.setPoDateOfProposal(acpPolicy.getInception());
        policyRequestDTO.setPoPaymentTerm(acpPolicy.getPremiumPaymentTerm());
        policyRequestDTO.setPoBsa(acpPolicy.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(acpPolicy.getDthSar());
        policyRequestDTO.setPoBasicPremium(acpPolicy.getInsuredModalPremium());
        policyRequestDTO.setPoPremiumType(getPremiumType(acpPolicy.getPremiumPaymentTerm()));
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(acpPolicy.getAgentCode()));
        policyRequestDTO.setPoBeginDate(acpPolicy.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(acpPolicy.getInception()));
        policyRequestDTO.setPoDateUnderwritten(acpPolicy.getInception());
        policyRequestDTO.setPoPremiumDueDate(acpPolicy.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(Integer.parseInt(acpPolicy.getFrequency())));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(acpPolicy.getStatus(), acpPolicy.getLastPremiumDueDate()));
        policyRequestDTO.setPoLastPremiumDueDate(acpPolicy.getLastPremiumDueDate());
        policyRequestDTO.setPoExpirationDate(acpPolicy.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(Integer.parseInt(acpPolicy.getSalesBranchCode())));
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
            policyRequestDTO.setLaAgeAdmitted(getAdmittedAge(acpPolicy.getInception(), contact.getDateOfBirth()).toString());
            policyRequestDTO.setLaAddressCity(contact.getCity());
            policyRequestDTO.setLaOccupation(contact.getOccupation());
            policyRequestDTO.setLaAnb(Integer.parseInt(policyRequestDTO.getLaAgeAdmitted()));
        } else {
            log.error("Contact details not found for policy {}", policyNo);
        }

        // ===== No Spouse (SP) =====

        requestDTOList.add(policyRequestDTO);
    }

    private void processMainDataReport(MainDataReportEntity mainDataReport, String policyNo) {

        if (!isEligiblePolicyStatus(mainDataReport.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, mainDataReport.getStatus());
            return;
        }

        PolicyRequestDTO policyRequestDTO = new PolicyRequestDTO();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(mainDataReport.getPlanNo());
        policyRequestDTO.setPoTerm(mainDataReport.getTerm());
        policyRequestDTO.setPoDateOfProposal(mainDataReport.getInception());
        policyRequestDTO.setPoPaymentTerm(mainDataReport.getPremiumPaymentTerm()); // dispute
        policyRequestDTO.setPoBsa(mainDataReport.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(mainDataReport.getDthSar());
        policyRequestDTO.setPoBasicPremium(mainDataReport.getModalPremium());
        policyRequestDTO.setPoPremiumType(getPremiumType(mainDataReport.getPremiumPaymentTerm())); // dispute
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(mainDataReport.getAgentCode()));
        policyRequestDTO.setPoBeginDate(mainDataReport.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(mainDataReport.getInception()));
        policyRequestDTO.setPoDateUnderwritten(mainDataReport.getInception());
        policyRequestDTO.setPoPremiumDueDate(mainDataReport.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(mainDataReport.getFrequency()));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(mainDataReport.getStatus(), mainDataReport.getLastPremiumDueDate()));
        policyRequestDTO.setPoLastPremiumDueDate(mainDataReport.getLastPremiumDueDate());
        policyRequestDTO.setPoExpirationDate(mainDataReport.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(mainDataReport.getSalesBranchCode()));
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
            policyRequestDTO.setLaAgeAdmitted(getAdmittedAge(mainDataReport.getInception(), contact.getDateOfBirth()).toString()); //check
            policyRequestDTO.setLaAddressCity(contact.getCity());
            policyRequestDTO.setLaOccupation(contact.getOccupation());
            policyRequestDTO.setLaAnb(Integer.parseInt(policyRequestDTO.getLaAgeAdmitted()));
            policyRequestDTO.setLaPrefLanguage(getLanguageChar(contact.getLanguagePreference()));
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
            policyRequestDTO.setSpAnb(getAdmittedAge(mainDataReport.getInception(), mainDataReport.getSpouseChildDob()));
        }
        requestDTOList.add(policyRequestDTO);
    }

    private void processALHReport(MainDataALHReportEntity alhReport, String policyNo) {

        if (!isEligiblePolicyStatus(alhReport.getStatus())) {
            log.info("Skipping policy {} due to status {}", policyNo, alhReport.getStatus());
            return;
        }

        PolicyRequestDTO policyRequestDTO = new PolicyRequestDTO();
        // ===== Policy (PO) =====
        policyRequestDTO.setPoPlanCode(getSoftLogicProductCodeMapping(policyNo));
        policyRequestDTO.setPoPlanVersion(alhReport.getPlanNo());
        policyRequestDTO.setPoTerm(alhReport.getTerm());
        policyRequestDTO.setPoDateOfProposal(alhReport.getInception());
        policyRequestDTO.setPoPaymentTerm(Integer.toString(alhReport.getPremiumPaymentTerm()));
        policyRequestDTO.setPoBsa(alhReport.getBasicSumAssured());
        policyRequestDTO.setPoSumAtRisk(alhReport.getDthSar());
        policyRequestDTO.setPoBasicPremium(alhReport.getModalPremium());
        policyRequestDTO.setPoPremiumType("Regular");
        policyRequestDTO.setPoAdvCode(getAgentCodeMapping(alhReport.getAgentCode()));
        policyRequestDTO.setPoBeginDate(alhReport.getInception());
        policyRequestDTO.setPoPolicyYear(getPolicyYear(alhReport.getInception()));
        policyRequestDTO.setPoDateUnderwritten(alhReport.getInception());
        policyRequestDTO.setPoPremiumDueDate(alhReport.getNextPremium());
        policyRequestDTO.setPoMode(getFrequencyString(alhReport.getFrequency()));
        policyRequestDTO.setPoPolicyStatusCode(getPolicyStatusCode(alhReport.getStatus(), alhReport.getLastPremiumDueDate()));
        policyRequestDTO.setPoLastPremiumDueDate(alhReport.getLastPremiumDueDate());
        policyRequestDTO.setPoExpirationDate(alhReport.getExpiry());
        policyRequestDTO.setPoBranchCode(getBranchCodeMapping(alhReport.getSalesBranchCode()));
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
            policyRequestDTO.setLaAgeAdmitted(getAdmittedAge(alhReport.getInception(), contact.getDateOfBirth()).toString()); //check
            policyRequestDTO.setLaAddressCity(contact.getCity());
            policyRequestDTO.setLaOccupation(contact.getOccupation());
            policyRequestDTO.setLaAnb(Integer.parseInt(policyRequestDTO.getLaAgeAdmitted()));
            policyRequestDTO.setLaPrefLanguage(getLanguageChar(contact.getLanguagePreference()));
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
            policyRequestDTO.setSpAnb(getAdmittedAge(alhReport.getInception(), alhReport.getSpouseDob()));

        }

        requestDTOList.add(policyRequestDTO);
    }

    private boolean isEligiblePolicyStatus(String status) {

        if (status == null) {
            return false;
        }

        return "In Force".equalsIgnoreCase(status)
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
                .orElse(null);
    }

    private String getPolicyStatusCode(String status, LocalDate lastPremiumDueDate) {
        if (status == null || lastPremiumDueDate == null) {
            return "NONE";
        }

        switch (status.trim().toUpperCase()) {
            case "IN FORCE":
                return "INFC";

            case "LAPSED":
                LocalDate today = LocalDate.now();
                return lastPremiumDueDate.isBefore(today.minusMonths(7))
                        ? "ALAP"
                        : "TLAP";

            default:
                return "NONE";
        }
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
        if (agentCode == null || agentCode.isEmpty()) {
            return null;
        }

        return advisorCodeMappingList.stream()
                .filter(mapping -> agentCode.equalsIgnoreCase(mapping.getCode()))
                .map(AdvisorCodeMappingEntity::getAdvCode)
                .findFirst()
                .orElse(null);
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
                mainDataReportRepository.findByProductCodeAndPolicyNo(productCode, policyNo);
        if (mainDataReportOpt.isPresent()) {
            log.info("Policy {} found in MainDataReportRepository", policyNumber);
            return mainDataReportOpt.get();
        }
        // 2. Try mainDataALHReportRepository
        Optional<MainDataALHReportEntity> mainDataALHOpt =
                mainDataALHReportRepository.findByProductCodeAndPolicyNo(productCode, policyNo);
        if (mainDataALHOpt.isPresent()) {
            log.info("Policy {} found in MainDataALHReportRepository", policyNumber);
            return mainDataALHOpt.get();
        }
        // 3. Try acpPolicyRepository
        Optional<ACPPolicyEntity> acpPolicyOpt =
                acpPolicyRepository.findByProductCodeAndPolicyNo(productCode, policyNo.toString());
        if (acpPolicyOpt.isPresent()) {
            log.info("Policy {} found in ACPPolicyRepository", policyNumber);
            return acpPolicyOpt.get();
        }

        // Policy isn't found anywhere
        log.warn("Policy {} not found in any repository", policyNumber);
        return null;
    }


    private PolicyNumberResponseDTO extractPolicyNumber(String policyRef) {

        if (policyRef == null || !policyRef.contains("/")) {
            throw new IllegalArgumentException("Invalid policy reference: " + policyRef);
        }

        String[] parts = policyRef.split("/");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid policy reference format: " + policyRef);
        }

        String productCode = parts[0].trim();
        int policyNo;

        try {
            policyNo = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid policy number: " + parts[1], e);
        }

        return new PolicyNumberResponseDTO(productCode, policyNo);
    }

}
