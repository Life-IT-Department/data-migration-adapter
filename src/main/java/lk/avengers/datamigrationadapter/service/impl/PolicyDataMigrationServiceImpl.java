package lk.avengers.datamigrationadapter.service.impl;

import jakarta.annotation.PostConstruct;
import lk.avengers.datamigrationadapter.dto.MigrPolicyDataDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.entity.softlogicdb.ExtraFields;
import lk.avengers.datamigrationadapter.entity.softlogicdb.FundCurrentBalanceEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyData;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PremiumExtraFields;
import lk.avengers.datamigrationadapter.mapper.ExtraFieldsMapper;
import lk.avengers.datamigrationadapter.mapper.FundCurrentBalanceMapper;
import lk.avengers.datamigrationadapter.mapper.PolicyMapper;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.*;
import lk.avengers.datamigrationadapter.repository.softlogicdb.ExtraFieldsRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.FundCurrentBalanceEntityRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPolicyRepository;
import lk.avengers.datamigrationadapter.service.PolicyDataMigrationService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final ExtraFieldsRepository extraFieldsRepository;
    private final PolicyListRepository policyListRepository;

    private final MainExcelReader mainExcelReader;
    private final SharedFunction sharedFunction;

    private static final int BATCH_SIZE = 1000;

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

    @Override
    public void migratePolicyData() {

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        List<PolicyNumberResponseDTO> extractedPolicies = policyList.stream()
                .map(this::extractPolicyNumberHelper)
                .toList();

        Set<String> productCodes = extractedPolicies.stream()
                .map(PolicyNumberResponseDTO::getProductCode)
                .collect(Collectors.toSet());

        Set<Integer> policyNos = extractedPolicies.stream()
                .map(PolicyNumberResponseDTO::getPolicyNo)
                .collect(Collectors.toSet());

        log.info("Preloading required data...");

        Map<String, MainDataReportEntity> mainDataMap =
                mainDataReportRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, MainDataALHReportEntity> alhMap =
                mainDataALHReportRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, ACPPolicyEntity> acpMap =
                acpPolicyRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, ContactDetailEntity> contactMap =
                contactDetailRepository.findFiltered(productCodes, policyNos).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProduct() + "/" + e.getPolicyNo(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        Map<String, PolicyListEntity> policyMap =
                policyListRepository.findFiltered(policyList).stream()
                                .collect(Collectors.toMap(
                                        PolicyListEntity::getContract,
                                    Function.identity(),
                                        (a,b) -> a
                                ));

        log.info("Preloading completed");

        policyRepository.truncateTable();
        fundCurrentBalanceEntityRepository.truncate();
        extraFieldsRepository.truncate();

        List<MigrPolicyData> policyBatch = new ArrayList<>();
        List<FundCurrentBalanceEntity> fundBatch = new ArrayList<>();
        List<ExtraFields> extraBatch = new ArrayList<>();

        int count = 0;

        for (String policy : policyList) {

            PolicyNumberResponseDTO extracted = sharedFunction.extractPolicyNumber(policy);
            String key = extracted.getProductCode() + "/" + extracted.getPolicyNo();

            MainDataReportEntity mainData = mainDataMap.get(key);
            MainDataALHReportEntity alh = alhMap.get(key);
            ACPPolicyEntity acp = acpMap.get(key);
            ContactDetailEntity contact = contactMap.get(key);
            PolicyListEntity polList = policyMap.get(key);

            LocalDate premiumDueDate = polList != null ? polList.getPaidUpTo() : null;

            if (mainData != null) {
                processMainDataFast(mainData, policy, premiumDueDate, contact, policyBatch, fundBatch, extraBatch);
            } else if (alh != null) {
                processALHFast(alh, policy, premiumDueDate, contact, policyBatch, extraBatch);
            } else if (acp != null) {
                String policyNoWithCertNo = policy.concat("/").concat(acp.getCertificateNo());
                processACPFast(acp, policyNoWithCertNo, premiumDueDate, contact, policyBatch, fundBatch, extraBatch);
            } else {
                log.warn("Policy {} not found", policy);
            }

            count++;

            // 📦 Batch logging
            if (count % BATCH_SIZE == 0) {

                log.info("Processed {} policies so far. Saving batch...", count);

                long batchStart = System.currentTimeMillis();

                saveBatch(policyBatch, fundBatch, extraBatch);

                long batchEnd = System.currentTimeMillis();

                log.info("Batch saved. Time taken: {} ms", (batchEnd - batchStart));
            }

            if (count % BATCH_SIZE == 0) {
                saveBatch(policyBatch, fundBatch, extraBatch);
            }
        }

        log.info("Final batch save...");
        saveBatch(policyBatch, fundBatch, extraBatch);

        log.info("Migration completed. Total processed: {}", count);
    }


    private <T> void processPolicyFast(
            T entity,
            String policyNo,
            LocalDate premiumDueDate,
            ContactDetailEntity contact,
            List<MigrPolicyData> policyBatch,
            List<FundCurrentBalanceEntity> fundBatch,
            List<ExtraFields> extraBatch,
            PolicyMapper<T> policyMapper,
            ExtraFieldsMapper<T> extraMapper,
            FundCurrentBalanceMapper<T> fundMapper,
            Function<T, String> statusExtractor,
            Function<T, Integer> aaeExtractor
    ) {

        if (!sharedFunction.isEligiblePolicyStatus(statusExtractor.apply(entity))) {
            log.info("Skipping policy {} due to status {}", policyNo, statusExtractor.apply(entity));
            return;
        }

        MigrPolicyDataDTO dto = new MigrPolicyDataDTO();

        // ===== Policy Mapping =====
        policyMapper.map(dto, entity, policyNo, premiumDueDate);

        // ===== Life Assured =====
        mapLifeAssured(dto, contact, policyNo, aaeExtractor.apply(entity));

        // ===== Convert + Add =====
        policyBatch.add(dto.mapData(MigrPolicyData.class));

        // ===== Extra Fields =====
        ExtraFields extra = new ExtraFields();
        extraMapper.map(extra, entity, policyNo);
        extraBatch.add(extra);

        // ===== Fund =====
        if (fundMapper != null) {
            FundCurrentBalanceEntity fund = new FundCurrentBalanceEntity();
            fundMapper.map(fund, entity);
            fundBatch.add(fund);
        }
    }

    private void processACPFast(
            ACPPolicyEntity e,
            String policyNo,
            LocalDate premiumDueDate,
            ContactDetailEntity contact,
            List<MigrPolicyData> policyBatch,
            List<FundCurrentBalanceEntity> fundBatch,
            List<ExtraFields> extraBatch
    ) {

        processPolicyFast(
                e, policyNo, premiumDueDate, contact,
                policyBatch, fundBatch, extraBatch,

                (dto, entity, polNo, dueDate) -> {
                    dto.setPoPlanCode(getSoftLogicProductCodeMapping(polNo));
                    dto.setPoPlanVersion(entity.getPlanNo());
                    dto.setPoTerm(entity.getTerm());
                    dto.setPoDateOfProposal(entity.getInception());
                    dto.setPoPaymentTerm(setPaymentTerm(entity.getPremiumPaymentTerm()));
                    dto.setPoBsa(entity.getBasicSumAssured());
                    dto.setPoSumAtRisk(entity.getDthSar());
                    dto.setPoBasicPremium(getModalPremium(entity.getInsuredModalPremium()));
                    dto.setPoPremiumType(getPremiumType(entity.getPremiumPaymentTerm()));
                    dto.setPoAdvCode(getAgentCodeMapping(entity.getAgentCode()));
                    dto.setPoBeginDate(entity.getInception());
                    dto.setPoPolicyYear(getPolicyYear(entity.getInception()));
                    dto.setPoDateUnderwritten(entity.getInception());
                    dto.setPoPremiumDueDate(dueDate != null ? dueDate : entity.getNextPremium());
                    dto.setPoMode(getFrequencyString(Integer.parseInt(entity.getFrequency())));
                    dto.setPoPolicyStatusCode(getPolicyStatusCode(entity.getStatus(), entity.getLastPremiumDueDate()));
                    dto.setPoExpirationDate(entity.getExpiry());
                    dto.setPoBranchCode(getBranchCodeMapping(Integer.parseInt(entity.getSalesBranchCode())));
                    dto.setPoPremium(entity.getInsuredModalPremium());
                    dto.setPoAdminFee(BigDecimal.ZERO);
                    dto.setPoIllusMatuValue(BigDecimal.ZERO);

                    mapSpouse(dto, null, null, null, null, 0, null);
                },

                (extra, entity, polNo) -> {
                    extra.setPolicyNo(polNo);
                    extra.setProposalNo(entity.getProductCode() + "/" + entity.getProposalNo());
                    extra.setCompanyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setCompanyBranchName(entity.getCompanyBranchName());
                    extra.setPolicyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setPolicyBranchName(entity.getCompanyBranchName());

                    extra.setInsuranceCoveragePeriod(Integer.valueOf(entity.getInsuranceCoveragePeriod()));
                    extra.setTotalPremiumAllocation(BigDecimal.ZERO);
                    extra.setBankName(contact.getSalesBranch());
                },

                (fund, entity) -> {
                    fund.setPolicyNo(policyNo);
                    fund.setTotalBalance(entity.getInsuredValueToday());
                    fund.setTopupBalance(entity.getInsuredTopupValueToday());
                    fund.setPrmValueToday(entity.getInsuredValueToday());
                },

                ACPPolicyEntity::getStatus,
                ACPPolicyEntity::getAae
        );
    }

    private void processMainDataFast(
            MainDataReportEntity e,
            String policyNo,
            LocalDate premiumDueDate,
            ContactDetailEntity contact,
            List<MigrPolicyData> policyBatch,
            List<FundCurrentBalanceEntity> fundBatch,
            List<ExtraFields> extraBatch
    ) {

        processPolicyFast(
                e, policyNo, premiumDueDate, contact,
                policyBatch, fundBatch, extraBatch,

                (dto, entity, polNo, dueDate) -> {

                    dto.setPoPlanCode(getSoftLogicProductCodeMapping(polNo));
                    dto.setPoPlanVersion(entity.getPlanNo());
                    dto.setPoTerm(entity.getTerm());
                    dto.setPoDateOfProposal(entity.getInception());
                    dto.setPoPaymentTerm(setPaymentTerm(String.valueOf(entity.getInitialPremiumPaymentTerm())));
                    dto.setPoBsa(entity.getBasicSumAssured());
                    dto.setPoSumAtRisk(entity.getDth_Sar());
                    dto.setPoBasicPremium(getModalPremium(entity.getModalPremium()));
                    dto.setPoPremiumType(getPremiumType(String.valueOf(entity.getInitialPremiumPaymentTerm())));
                    dto.setPoAdvCode(getAgentCodeMapping(entity.getAgentCode()));
                    dto.setPoBeginDate(entity.getInception());
                    dto.setPoPolicyYear(getPolicyYear(entity.getInception()));
                    dto.setPoDateUnderwritten(entity.getInception());
                    dto.setPoPremiumDueDate(dueDate != null ? dueDate : entity.getNextPremium());
                    dto.setPoMode(getFrequencyString(entity.getFrequency()));
                    dto.setPoPolicyStatusCode(getPolicyStatusCode(entity.getStatus(), entity.getLastPremiumDueDate()));
                    dto.setPoExpirationDate(entity.getExpiry());
                    dto.setPoBranchCode(getBranchCodeMapping(entity.getSalesBranchCode()));
                    dto.setPoPremium(entity.getModalPremium());
                    dto.setPoAdminFee(BigDecimal.ZERO);
                    dto.setPoIllusMatuValue(BigDecimal.ZERO);
                    dto.setPoInitialDefermentTerm(entity.getInitialDefermentTerm());
                    dto.setPoInitialRetirementBenefitPayoutTerm(entity.getInitialRetirementBenefitPayoutTerm());
                    dto.setPoInitialRetirementPayoutMode((int) Double.parseDouble(entity.getRetirementPayoutMode()));

                    mapSpouse(dto,
                            entity.getSpouseChildFullName(),
                            entity.getSpouseChildTitle(),
                            entity.getSpouseChildGender(),
                            entity.getSpouseChildDob(),
                            entity.getSpouseChildAge(),
                            entity.getSpouseIdCardNumber());
                },

                (extra, entity, polNo) -> {
                    extra.setPolicyNo(polNo);
                    extra.setProposalNo(entity.getProductCode() + "/" + entity.getProposalNo());
                    extra.setCompanyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setCompanyBranchName(entity.getCompanyBranchName());
                    extra.setPolicyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setPolicyBranchName(entity.getCompanyBranchName());

                    extra.setInterestRate(BigDecimal.valueOf(entity.getInterestRate()));
                    extra.setTpdPremiumLife1(entity.getTpdPremiumLife1());
                    extra.setTpdPremiumLife2(entity.getTpdPremiumLife2());
                    extra.setInterestCredited(entity.getInterestCredited());
                    extra.setSurrenderValue(entity.getSurrenderValue());
                    extra.setPrmSurrenderValue(entity.getPrmSurrenderValue());
                    extra.setBstSurrenderValue(entity.getBstSurrenderValue());

                    extra.setInsuranceCoveragePeriod(entity.getInsuranceCoveragePeriod());
                    extra.setTotalPremiumAllocation(entity.getTransactionAmount());
                    extra.setBankName(contact.getSalesBranch());
                },

                (fund, entity) -> {
                    fund.setPolicyNo(policyNo);
                    fund.setTotalBalance(entity.getValueToday());
                    fund.setTopupBalance(entity.getBstValueToday());
                    fund.setPrmValueToday(entity.getPrmValueToday());
                },

                MainDataReportEntity::getStatus,
                MainDataReportEntity::getAae
        );
    }

    private void processALHFast(
            MainDataALHReportEntity e,
            String policyNo,
            LocalDate premiumDueDate,
            ContactDetailEntity contact,
            List<MigrPolicyData> policyBatch,
            List<ExtraFields> extraBatch
    ) {

        processPolicyFast(
                e,
                policyNo,
                premiumDueDate,
                contact,
                policyBatch,
                new ArrayList<>(), // ALH has no fund mapping
                extraBatch,

                // ===== Policy Mapper =====
                (dto, entity, polNo, dueDate) -> {

                    dto.setPoPlanCode(getSoftLogicProductCodeMapping(polNo));
                    dto.setPoPlanVersion(entity.getPlanNo());
                    dto.setPoTerm(entity.getTerm());
                    dto.setPoDateOfProposal(entity.getInception());
                    dto.setPoPaymentTerm(entity.getPremiumPaymentTerm());
                    dto.setPoBsa(entity.getBasicSumAssured());
                    dto.setPoSumAtRisk(entity.getDth_Sar());
                    dto.setPoBasicPremium(getModalPremium(entity.getModalPremium()));
                    dto.setPoPremiumType("Regular");
                    dto.setPoAdvCode(getAgentCodeMapping(entity.getAgentCode()));
                    dto.setPoBeginDate(entity.getInception());
                    dto.setPoPolicyYear(getPolicyYear(entity.getInception()));
                    dto.setPoDateUnderwritten(entity.getInception());
                    dto.setPoPremiumDueDate(dueDate != null ? dueDate : entity.getNextPremium());
                    dto.setPoMode(getFrequencyString(entity.getFrequency()));
                    dto.setPoPolicyStatusCode(getPolicyStatusCode(entity.getStatus(), entity.getLastPremiumDueDate()));
                    dto.setPoExpirationDate(entity.getExpiry());
                    dto.setPoBranchCode(getBranchCodeMapping(entity.getSalesBranchCode()));
                    dto.setPoPremium(entity.getModalPremium());
                    dto.setPoAdminFee(BigDecimal.ZERO);
                    dto.setPoIllusMatuValue(BigDecimal.ZERO);

                    // ===== Spouse =====
                    mapSpouse(dto,
                            entity.getSpouseFullName(),
                            entity.getSpouseTitle(),
                            entity.getSpouseGender(),
                            entity.getSpouseDob(),
                            entity.getSpouseAge(),
                            entity.getSpouseIdCardNumber());

                    // ===== ALH-specific LA fields =====
                    dto.setLaHbc(entity.getHb_Sa());
                    dto.setLaInpc(entity.getInpSar());
                    dto.setLaBonus(entity.getMlBonus());
                },

                // ===== Extra Fields Mapper =====
                (extra, entity, polNo) -> {
                    extra.setPolicyNo(polNo);
                    extra.setProposalNo(entity.getProductCode() + "/" + entity.getProposalNo());
                    extra.setCompanyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setCompanyBranchName(entity.getCompanyBranchName());
                    extra.setPolicyBranchCode(String.valueOf(entity.getCompanyBranchCode()));
                    extra.setPolicyBranchName(entity.getCompanyBranchName());

                    // ALH defaults
                    extra.setInterestRate(BigDecimal.ZERO);
                    extra.setTpdPremiumLife1(BigDecimal.ZERO);
                    extra.setTpdPremiumLife2(BigDecimal.ZERO);
                    extra.setInterestCredited(BigDecimal.ZERO);
                    extra.setSurrenderValue(BigDecimal.ZERO);
                    extra.setPrmSurrenderValue(BigDecimal.ZERO);
                    extra.setBstSurrenderValue(BigDecimal.ZERO);

                    extra.setInsuranceCoveragePeriod(entity.getInsuranceCoveragePeriod());
                    extra.setTotalPremiumAllocation(BigDecimal.ZERO);
                    extra.setBankName(contact.getSalesBranch());
                },

                // ===== No Fund Mapper =====
                null,

                // ===== Status extractor =====
                MainDataALHReportEntity::getStatus,

                // ===== AAE extractor =====
                MainDataALHReportEntity::getAae
        );
    }


    private void mapLifeAssured(MigrPolicyDataDTO dto, ContactDetailEntity contact, String policyNo, int aae) {
        if (contact == null) {
            log.error("Contact details not found for policy {}", policyNo);
            return;
        }
        dto.setLaPolicyNo(policyNo);
        dto.setLaTitle(contact.getTitle());
        dto.setLaFirstName(contact.getFirstName());
        dto.setLaLastName(
                contact.getLastName() == null || contact.getLastName().trim().isEmpty()
                        ? contact.getFirstName()
                        : contact.getLastName()
        );
        dto.setLaAddress(contact.getAddress());
        dto.setLaNic(contact.getNicNumber());
        dto.setLaSex(getSexChar(contact.getGender()));
        dto.setLaDob(contact.getDateOfBirth());
        dto.setLaPhone1(getTenDigitMobile(contact.getMobile()));
        dto.setLaPhone2(getOtherTelephoneNumber(contact.getOtherTelephoneNumber()));
        dto.setLaNationality(contact.getNationality().toUpperCase());
        dto.setLaEmail(getValidatedEmail(contact.getEmailAddress()));
        dto.setLaAgeAdmitted(false);
        dto.setLaAddressCity(extractAddressCity(contact.getCity()));
        dto.setLaOccupation(String.valueOf(getOccupation(contact.getOccupation())));
        dto.setLaAnb(aae);
        dto.setLaPrefLanguage(getLanguageChar(contact.getLanguagePreference()));
        dto.setLaNameWithInitials(getNameWithInitials(contact.getFirstName(), contact.getLastName()));
        dto.setLaIsPolicyAssign(false);
        dto.setLaWeight(0);
        dto.setLaHeight(0);
    }

    private void mapSpouse(MigrPolicyDataDTO dto, String name, String title, String gender, LocalDate dob, int age, String nic) {
        if (name != null && !name.trim().isEmpty()) {
            dto.setSpTitle(title);
            dto.setSpFirstName(name);
            dto.setSpSex(getSexChar(gender));
            dto.setSpDob(dob);
            dto.setSpAnb(age);
            dto.setSpNic(nic);
        } else {
            dto.setSpSex("");
            dto.setSpAnb(0);
        }
        dto.setSpAgeAdmitted(false);
        dto.setSpHeight(0);
        dto.setSpWeight(0);
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

        String sanitized = mobile.trim();

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
        PolicyNumberResponseDTO policyNumber = sharedFunction.extractPolicyNumber(policyNo);

        return contactDetailRepository
                .findFirstByProductAndPolicyNo(
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
            case "AMENDED" -> "AMND";
            case "IN FORCE" -> "INFC";
            case "BORN DEAD" -> "BRND";
            case "CANCELLED", "CANCELLED / PAID UP" -> "CNLD";
            case "DECEASED", "DECEASED / PAID UP" -> "PRMD";
            case "DISABILITY CLAIM" -> "DCLM";
            case "EXPIRED", "EXPIRED / PAID UP" -> "EXPD";
            case "SURRENDED", "SURRENDED / PAID UP" -> "SRND";
            case "SUSPENDED" -> "SPND";
            case "WAITING FOR CANCELLATION" -> "CNLD";
            case "IN FORCE / PAID UP", "WAITING FOR PAID UP" -> "PDUP";
            case "LAPSED" -> "LPSD";
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

        PolicyNumberResponseDTO policyNumber = sharedFunction.extractPolicyNumber(policyNo);
        String productCode = policyNumber.getProductCode();

        return productCodeMappingList.stream()
                .filter(mapping -> mapping.getProductCode().equalsIgnoreCase(productCode))
                .map(ProductCodeMappingEntity::getSlProductCode)
                .findFirst()
                .orElse(null);
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

    private BigDecimal getModalPremium(BigDecimal insuredModalPremium){
        return insuredModalPremium.setScale(0, RoundingMode.DOWN);
    }

    private void saveBatch(List<MigrPolicyData> policyBatch,
                           List<FundCurrentBalanceEntity> fundBatch,
                           List<ExtraFields> extraBatch) {

        if (!policyBatch.isEmpty()) {
            policyRepository.saveAll(policyBatch);
            policyBatch.clear();
        }

        if (!fundBatch.isEmpty()) {
            fundCurrentBalanceEntityRepository.saveAll(fundBatch);
            fundBatch.clear();
        }

        if (!extraBatch.isEmpty()) {
            extraFieldsRepository.saveAll(extraBatch);
            extraBatch.clear();
        }
    }

    private PolicyNumberResponseDTO extractPolicyNumberHelper (String policyRef) {
        return sharedFunction.extractPolicyNumber(policyRef);
    }
}

