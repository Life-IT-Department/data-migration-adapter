package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.ChildDto;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.dto.RiderCoverColumnDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.ACPPolicyEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BenefitCodeMapperEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyBenefitsEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyBenefitsID;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyData;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ACPPolicyRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.BenefitCodeMapperRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.MigrPolicyRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyBenefitsEntityRepository;
import lk.avengers.datamigrationadapter.service.PolicyBenefitMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyBenefitMappingServiceImpl implements PolicyBenefitMappingService {

    private final BenefitCodeMapperRepository benefitCodeMapperRepository;
    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;
    private final PolicyBenefitsEntityRepository policyBenefitsEntityRepository;
    private final MigrPolicyRepository migrPolicyRepository;

    private final SharedFunction sharedFunction;

    private final MainExcelReader mainExcelReader;
    private static final String SPOUSE = "Spouse";
    private static final String CHILD ="Child";
    private static final String INFLATION_GUARD_BENEFIT = "ZIFG";

    private static final String BASIC_LIFE_COVER = "BLIF";
    private static final String BASIC_LIFE_COVER_SPOUSE = "ZSCB";
    private static final String IN_PATIENT_COVER = "ZINP";
    private static final String IN_PATIENT_COVER_SPOUSE = "ZINS";

    private static final String ADDITIONAL_DEATH_BENEFIT = "ZSTB";
    private static final String CRITICAL_ILLNESS = "ZCIC";
    private static final String FAMILY_INCOME_BENEFIT = "ZFIB";

    private static final String SUWASAHANA = "ASP";

    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public CommonResponseDTO processBenefitCodeMapping() {
        log.info("Benefit code mapping process started. Truncating table");
        policyBenefitsEntityRepository.truncate();
        log.info("Benefits table truncated successfully");

        List<String> successfulPolicyList = new ArrayList<>();

        List<BenefitCodeMapperEntity> benefitCodeMapperEntityList = benefitCodeMapperRepository.findAll();

        List<MigrPolicyBenefitsEntity> policyBenefitsEntityList = new ArrayList<>();
        mapDeathBenefitCode(benefitCodeMapperEntityList);

        List<String> fieldNamesInMainDataEntityClassList = getFieldNamesInClass(MainDataReportEntity.class);

        AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList = new AtomicReference<>();
        AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList = new AtomicReference<>();

        policyHolderBenefitFieldNamesInMainDataEntityClassList.set(new ArrayList<>());
        spouseBenefitFieldNamesInMainDataEntityClassList.set(new ArrayList<>());

        Map<String, RiderCoverColumnDTO> policyHolderBenefitMap = new HashMap<>();
        Map<String, RiderCoverColumnDTO> spouseBenefitMap = new HashMap<>();

        setFieldNamesInMainDataEntityClassLists(benefitCodeMapperEntityList, policyHolderBenefitFieldNamesInMainDataEntityClassList, fieldNamesInMainDataEntityClassList, spouseBenefitFieldNamesInMainDataEntityClassList);
        setMainDataBenefitsToHashMaps(benefitCodeMapperEntityList, policyHolderBenefitFieldNamesInMainDataEntityClassList, spouseBenefitFieldNamesInMainDataEntityClassList, policyHolderBenefitMap, spouseBenefitMap);

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
        policyList.forEach(policyNo -> {
            PolicyNumberResponseDTO extracted = extractPolicyNumberHelper(policyNo);
            String key = extracted.getProductCode() + "/" + extracted.getPolicyNo();

            MainDataReportEntity mainData = mainDataMap.get(key);
            MainDataALHReportEntity alhData = alhMap.get(key);
            ACPPolicyEntity acpData = acpMap.get(key);

            if (mainData != null) {
                if (!sharedFunction.isEligiblePolicyStatus(mainData.getStatus())) {
                    log.info("Skipping policy {} due to status {}", policyNo, mainData.getStatus());
                    return;
                }
                successfulPolicyList.add(policyNo);
                policyHolderBenefitMap.forEach((k, v) -> generateBenefitsEntityFromMainData(policyNo, mainData, k, v, policyBenefitsEntityList));
                spouseBenefitMap.forEach((k, v) -> generateBenefitsEntityFromMainData(policyNo, mainData, k, v, policyBenefitsEntityList));
                addChildBenefitsFromMainData(policyNo, mainData, benefitCodeMapperEntityList, policyBenefitsEntityList);

            } else if (alhData != null) {
                if (!sharedFunction.isEligiblePolicyStatus(alhData.getStatus())) {
                    log.info("Skipping policy {} due to ALH status {}", policyNo, alhData.getStatus());
                    return;
                }
                successfulPolicyList.add(policyNo);
                processALHData(policyNo, alhData, benefitCodeMapperEntityList, policyBenefitsEntityList);

            } else if (acpData != null) {
                if (!sharedFunction.isEligiblePolicyStatus(acpData.getStatus())) {
                    log.info("Skipping policy {} due to ACP status {}", policyNo, acpData.getStatus());
                    return;
                }
                successfulPolicyList.add(policyNo);
                processACPData(policyNo, acpData, benefitCodeMapperEntityList, policyBenefitsEntityList);

            } else {
                log.warn("No data found for policy {}", policyNo);
            }
        });

        if (!policyBenefitsEntityList.isEmpty()) {
            setInPatientSA(successfulPolicyList, policyBenefitsEntityList);
        } else {
            log.warn("No policy benefits found to save. Please check the excel file and try again.");
            return CommonResponseDTO.builder()
                    .message("No Policy benefits Found to save. Please check the excel file and try again.")
                    .status(HttpStatus.BAD_REQUEST.toString())
                    .build();
        }

        log.info("Policy benefits saved successfully. {} policy benefits records were saved from {} policies.",
                policyBenefitsEntityList.size(), successfulPolicyList.size());

        return CommonResponseDTO.builder()
                .message(String.format("%d policy benefits records were saved from %d policies",
                        policyBenefitsEntityList.size(), successfulPolicyList.size()))
                .status(HttpStatus.OK.toString())
                .build();
    }

    private void addChildBenefitsFromMainData(String policyNo, MainDataReportEntity mainDataReportEntity, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {
        if (mainDataReportEntity.getChild1Name() != null && !mainDataReportEntity.getChild1Name().isBlank()) {
            if (mainDataReportEntity.getChild1Hbc_() != 0) {
                MigrPolicyBenefitsEntity policyBenefitsEntity = new MigrPolicyBenefitsEntity();
                String softlogicBenefitCode = benefitCodeMapperEntityList
                        .stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-HBC"))
                        .findFirst().orElseThrow(() -> new RuntimeException("No Child-HBC benefit code found in benefit code mapper table for policy no: " + policyNo))
                        .getSoftlogicBenefitCode();

                policyBenefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, softlogicBenefitCode));
                policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntity.getChild1Hbc_()));
                policyBenefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.ZERO);
                policyBenefitsEntity.setPbExtraMortalityRate(BigDecimal.ZERO);

                policyBenefitsEntityList.add(policyBenefitsEntity);
            }
            if (mainDataReportEntity.getChild1Hbcac_() != 0) {
                MigrPolicyBenefitsEntity policyBenefitsEntity = new MigrPolicyBenefitsEntity();
                String softlogicBenefitCode = benefitCodeMapperEntityList
                        .stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-HBCAC"))
                        .findFirst().orElseThrow(() -> new RuntimeException("No Child-HBCAC benefit code found in benefit code mapper table for policy no: " + policyNo))
                        .getSoftlogicBenefitCode();

                policyBenefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, softlogicBenefitCode));
                policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntity.getChild1Hbcac_()));
                policyBenefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.ZERO);
                policyBenefitsEntity.setPbExtraMortalityRate(BigDecimal.ZERO);

                policyBenefitsEntityList.add(policyBenefitsEntity);
            }
        }
    }

    private void generateBenefitsEntityFromMainData(String policyNo, MainDataReportEntity mainDataReportEntity, String key, RiderCoverColumnDTO value, List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {
        if (value.getCoverName() != null) {
            Object benefitSumAssuredFieldValue = getSpecificFieldValue(mainDataReportEntity, value.getCoverName());
            MigrPolicyBenefitsEntity benefitsEntity = new MigrPolicyBenefitsEntity();

            if(mainDataReportEntity.getPremiumEscalationBenefitPercentage() != 0){
                benefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, INFLATION_GUARD_BENEFIT));
                benefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntity.getPremiumEscalationBenefitPercentage()));
                benefitsEntity.setPbOccuExtra(BigDecimal.ZERO);
                benefitsEntity.setPbExtraMortalityRate(BigDecimal.ZERO);
                benefitsEntity.setPbTerm(mainDataReportEntity.getPremiumPaymentTerm().equalsIgnoreCase("sp") ? 1 : (int) Double.parseDouble(mainDataReportEntity.getPremiumPaymentTerm()));
                benefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                benefitsEntity.setPbPremPortion(BigDecimal.ZERO);

                policyBenefitsEntityList.add(benefitsEntity);
            }

            if (toBigDecimal(benefitSumAssuredFieldValue).compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal perMilRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverPerMilRate()));
                BigDecimal occupationExtraRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverOccupationExtraRate()));
                BigDecimal subStdRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverSubRate()));

                benefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, key));
                benefitsEntity.setPbCoverage((BigDecimal) benefitSumAssuredFieldValue);
                benefitsEntity.setPbOccuExtra(occupationExtraRate);
                benefitsEntity.setPbExtraMortalityRate(subStdRate);
                benefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                benefitsEntity.setPbExtraPremium(perMilRate);
                benefitsEntity.setPbPremPortion(BigDecimal.ZERO);

                policyBenefitsEntityList.add(benefitsEntity);
            }
        }
    }

    private void processACPData(String policyNo, ACPPolicyEntity acpData,
                                List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                                List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {

        Map<String, BigDecimal> benefitsMap = Map.of(
                "DTH", acpData.getDthSar(),
                "ACCD", acpData.getAccdSa(),
                "ACCP", acpData.getAccpSa(),
                "ACCT", acpData.getAcctSa(),
                "CILX", acpData.getCilxSa(),
                "PTD", acpData.getPtdSa()
        );

        Map<String, BigDecimal> occuMap = Map.of(
                "DTH", acpData.getDthOccupationalLoadingPerc(),
                "ACCD", acpData.getAccdOccupationalLoadingPerc(),
                "ACCP", acpData.getAccpOccupationalLoadingPerc(),
                "ACCT", acpData.getAcctOccupationalLoadingPerc(),
                "CILX", acpData.getCilxOccupationalLoadingPerc(),
                "PTD", acpData.getPtdOccupationalLoadingPerc()
        );

        Map<String, BigDecimal> subRateMap = Map.of(
                "DTH", toBigDecimalFromString(acpData.getSubDth()),
                "ACCD", toBigDecimalFromString(acpData.getSubAccd()),
                "ACCP", toBigDecimalFromString(acpData.getSubAccp()),
                "ACCT", toBigDecimalFromString(acpData.getSubAcct()),
                "CILX", toBigDecimalFromString(acpData.getSubCilx()),
                "PTD", toBigDecimalFromString(acpData.getSubPtd())
        );

        Map<String, BigDecimal> extraPremiumMap = Map.of(
                "DTH", acpData.getSubRateMilDth(),
                "ACCD", acpData.getSubRateMilAccd(),
                "ACCP", acpData.getSubRateMilAccp(),
                "ACCT", acpData.getSubRateMilAcct(),
                "CILX", acpData.getSubRateMilCilx(),
                "PTD", acpData.getSubRateMilPtd()
        );

        benefitsMap.forEach((key, coverage) -> {
            if (coverage.compareTo(BigDecimal.ZERO) > 0) {
                MigrPolicyBenefitsEntity benefitsEntity = getPolicyBenefitForACPData(policyNo, benefitCodeMapperEntityList, acpData, key);
                if (benefitsEntity != null) {
                    benefitsEntity.setPbCoverage(coverage);
                    benefitsEntity.setPbOccuExtra(occuMap.get(key));
                    benefitsEntity.setPbExtraMortalityRate(subRateMap.get(key));
                    benefitsEntity.setPbExtraPremium(extraPremiumMap.get(key));
                    policyBenefitsEntityList.add(benefitsEntity);
                } else {
                    log.warn("No {} benefit code found in benefit code mapper table for policy no: {}", key, policyNo);
                }
            }
        });
    }

    private void processALHData(String policyNo, MainDataALHReportEntity alhData,
                                List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                                List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {

        // DTH
        if (alhData.getDth_Sar().compareTo(BigDecimal.ZERO) > 0) {
            addALHBenefit(policyNo, benefitCodeMapperEntityList, alhData, "DTH",
                    alhData.getDth_Sar(),
                    BigDecimal.valueOf(alhData.getDth_OccLoadingPercentage()),
                    BigDecimal.valueOf(alhData.getSubDth_()),
                    alhData.getSubRateMilDth_(),
                    alhData.getTerm(),
                    policyBenefitsEntityList);
        }

        // HB
        if (alhData.getHb_Sa().compareTo(BigDecimal.ZERO) > 0) {
            addALHBenefit(policyNo, benefitCodeMapperEntityList, alhData, "HB",
                    alhData.getHb_Sa(),
                    BigDecimal.valueOf(alhData.getHb_OccupationalLoadingPercentage()),
                    alhData.getSubHb_(),
                    alhData.getSubRateMilHb_(),
                    alhData.getTerm(),
                    policyBenefitsEntityList);
        }

        // INP
        if (alhData.getInpSar().compareTo(BigDecimal.ZERO) > 0) {
            addALHBenefit(policyNo, benefitCodeMapperEntityList, alhData, "INP",
                    alhData.getInpSar(),
                    BigDecimal.valueOf(alhData.getInpOccLoadingPercentage()),
                    alhData.getSubInp(),
                    BigDecimal.valueOf(alhData.getInpOccLoadingPercentage()),
                    alhData.getTerm(),
                    policyBenefitsEntityList);
        }

        // Spouse
        if (alhData.getSpousePin() != null && alhData.getSpousePin() != 0) {
            setSpouseBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, alhData, policyBenefitsEntityList);
        }

        // Children
        ChildDto child1Dto = ChildDto.builder()
                .name(alhData.getChild1Name())
                .childHbc(Double.toString(alhData.getChild1Hbc_()))
                .childInpSar(alhData.getChild1InpSar())
                .build();

        setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child1Dto, alhData, policyBenefitsEntityList);
    }

    private void addALHBenefit(String policyNo,
                               List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                               MainDataALHReportEntity alhData,
                               String allianzCode,
                               BigDecimal coverage,
                               BigDecimal occuExtra,
                               BigDecimal extraMortality,
                               BigDecimal extraPremium,
                               int term,
                               List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {

        MigrPolicyBenefitsEntity benefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, alhData, allianzCode);
        if (benefitsEntity != null) {
            benefitsEntity.setPbCoverage(coverage);
            benefitsEntity.setPbOccuExtra(occuExtra);
            benefitsEntity.setPbExtraMortalityRate(extraMortality);
            benefitsEntity.setPbExtraPremium(extraPremium);
            benefitsEntity.setPbTerm(term);
            policyBenefitsEntityList.add(benefitsEntity);
        } else {
            log.warn("No {} benefit code found in benefit code mapper table for policy no: {}", allianzCode, policyNo);
        }
    }

    private void setChildBenefitsFromALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, ChildDto childDto, MainDataALHReportEntity mainDataALHReportEntity,
                                             List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {

        if (childDto.getName() != null && !childDto.getName().isBlank()) {

            if (childDto.getChildHbc() != null && Double.parseDouble(childDto.getChildHbc()) != 0.0) {
                benefitCodeMapperEntityList.stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-HBC"))
                        .findFirst()
                        .ifPresentOrElse(benefitCodeMapperEntity -> {
                            MigrPolicyBenefitsEntity policyBenefitsEntity = new MigrPolicyBenefitsEntity();

                            policyBenefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, benefitCodeMapperEntity.getSoftlogicBenefitCode()));
                            policyBenefitsEntity.setPbCoverage(new BigDecimal(childDto.getChildHbc()));
                            policyBenefitsEntity.setPbTerm(mainDataALHReportEntity.getTerm());
                            policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbExtraMortalityRate(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbOccuExtra(BigDecimal.ZERO);

                            policyBenefitsEntityList.add(policyBenefitsEntity);
                        }, () -> log.warn("No Child-HBC benefit code found in benefit code mapper table for policy no: {}", policyNo));
            }

            if (childDto.getChildInpSar() != null && childDto.getChildInpSar().compareTo(BigDecimal.ZERO) > 0) {
                benefitCodeMapperEntityList.stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-INPC"))
                        .findFirst()
                        .ifPresentOrElse(benefitCodeMapperEntity -> {
                            MigrPolicyBenefitsEntity policyBenefitsEntity = new MigrPolicyBenefitsEntity();

                            policyBenefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, benefitCodeMapperEntity.getSoftlogicBenefitCode()));
                            policyBenefitsEntity.setPbCoverage(childDto.getChildInpSar());
                            policyBenefitsEntity.setPbTerm(mainDataALHReportEntity.getTerm());
                            policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbExtraMortalityRate(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                            policyBenefitsEntity.setPbOccuExtra(BigDecimal.ZERO);

                            policyBenefitsEntityList.add(policyBenefitsEntity);
                        }, () -> log.warn("No Child-INP benefit code found in benefit code mapper table for policy no: {}", policyNo));
            }
        }
    }

    private void setSpouseBenefitsFromALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, MainDataALHReportEntity mainDataALHReportEntity, List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {
        if (mainDataALHReportEntity.getSpouseDth_Sa().compareTo(BigDecimal.ZERO) > 0) {
            MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-DTH");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseDth_OccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilDth_());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseDth_Sa());
                policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);

                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No Spouse-DTH benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }

        if (mainDataALHReportEntity.getSpouseHbSa().compareTo(BigDecimal.ZERO) > 0) {
            MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-HB");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseHbOccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilHb());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseHbSa());
                policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No Spouse-HB benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }

        if (mainDataALHReportEntity.getSpouseInpSar().compareTo(BigDecimal.ZERO) > 0) {
            MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-INP");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseInpOccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilInp());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseInpSar());
                policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No INP benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }
    }

    private MigrPolicyBenefitsEntity getPolicyBenefitForALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                                                                MainDataALHReportEntity mainDataALHReportEntity, String allianzBenefitCode) {
        AtomicReference<MigrPolicyBenefitsEntity> benefitsEntity = new AtomicReference<>();
        benefitCodeMapperEntityList.stream()
                .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase(allianzBenefitCode))
                .findFirst().ifPresentOrElse(benefitCodeMapperEntity -> {
                    benefitsEntity.set(MigrPolicyBenefitsEntity.builder()
                            .id(new MigrPolicyBenefitsID(policyNo.trim(), benefitCodeMapperEntity.getSoftlogicBenefitCode()))
                            .pbTerm(mainDataALHReportEntity.getTerm())
                            .pbExtraPremium(BigDecimal.ZERO)
                            .pbPremPortion(BigDecimal.ZERO)
                            .build());
                }, () -> log.warn("No {} benefit code found in benefit code mapper table for policy no: {}", allianzBenefitCode, policyNo));
        return benefitsEntity.get();
    }

    private MigrPolicyBenefitsEntity getPolicyBenefitForACPData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                                                                ACPPolicyEntity acpPolicyEntity, String allianzBenefitCode) {
        AtomicReference<MigrPolicyBenefitsEntity> benefitsEntity = new AtomicReference<>();
        benefitCodeMapperEntityList.stream()
                .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase(allianzBenefitCode))
                .findFirst().ifPresentOrElse(benefitCodeMapperEntity -> {
                    benefitsEntity.set(MigrPolicyBenefitsEntity.builder()
                            .id(new MigrPolicyBenefitsID(policyNo.trim(), benefitCodeMapperEntity.getSoftlogicBenefitCode()))
                            .pbTerm(acpPolicyEntity.getTerm())
                            .pbExtraPremium(BigDecimal.ZERO)
                            .pbPremPortion(BigDecimal.ZERO)
                            .build());
                }, () -> log.warn("No {} benefit code found in benefit code mapper table for policy no: {}", allianzBenefitCode, policyNo));
        return benefitsEntity.get();
    }



    private void setFieldNamesInMainDataEntityClassLists(List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList, List<String> fieldNamesInMainDataEntityClassList, AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList) {
        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {

            policyHolderBenefitFieldNamesInMainDataEntityClassList.get().addAll(fieldNamesInMainDataEntityClassList
                    .stream()
                    .filter(fieldName -> !fieldName.trim().toLowerCase().contains(SPOUSE.toLowerCase()))
                    .filter(fieldName -> !fieldName.trim().toLowerCase().contains(CHILD.toLowerCase()))
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList());

            spouseBenefitFieldNamesInMainDataEntityClassList.get().addAll(fieldNamesInMainDataEntityClassList
                    .stream()
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains(SPOUSE.toLowerCase()))
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList());
        });
    }

    private void setMainDataBenefitsToHashMaps(List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList, AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList, Map<String, RiderCoverColumnDTO> policyHolderBenefitMap, Map<String, RiderCoverColumnDTO> spouseBenefitMap) {
        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {

            List<String> policyHolderFilterList = policyHolderBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList();

            List<String> spouseFilterList = spouseBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().replaceFirst("^Spouse-", "").concat("_").toLowerCase().trim()))
                    .toList();

            if (!policyHolderFilterList.isEmpty() && !benefitCodeMapperEntity.getAllianzBenefitCode().toLowerCase().startsWith(SPOUSE.toLowerCase())) {
                RiderCoverColumnDTO riderCoverColumnDTO = RiderCoverColumnDTO.builder()
                        .coverName(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("_sa")).findFirst().orElse(null))
                        .coverPerMilRate(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("mil")).findFirst().orElse(null))
                        .coverOccupationExtraRate(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("occ")).findFirst().orElse(null))
                        .coverSubRate(
                                policyHolderFilterList.stream()
                                        .filter(name -> {
                                            String lower = name.toLowerCase();
                                            return !lower.contains("_sa")
                                                    && !lower.contains("mil")
                                                    && !lower.contains("occ");
                                        })
                                        .findFirst()
                                        .orElse(null)
                        )
                        .build();
                policyHolderBenefitMap.put(benefitCodeMapperEntity.getSoftlogicBenefitCode(), riderCoverColumnDTO);
            }

            if (!spouseFilterList.isEmpty() && benefitCodeMapperEntity.getAllianzBenefitCode().toLowerCase().startsWith(SPOUSE.toLowerCase())) {
                RiderCoverColumnDTO spouseRiderCoverColumnDTO = RiderCoverColumnDTO.builder()
                        .coverName(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("_sa")).findFirst().orElse(null))
                        .coverPerMilRate(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("mil")).findFirst().orElse(null))
                        .coverOccupationExtraRate(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("occ")).findFirst().orElse(null))
                        .coverSubRate(
                                spouseFilterList.stream()
                                        .filter(name -> {
                                            String lower = name.toLowerCase();
                                            return !lower.contains("_sa")
                                                    && !lower.contains("mil")
                                                    && !lower.contains("occ");
                                        })
                                        .findFirst()
                                        .orElse(null)
                        )
                        .build();
                spouseBenefitMap.put(benefitCodeMapperEntity.getSoftlogicBenefitCode(), spouseRiderCoverColumnDTO);
            }
        });
    }

    private void mapDeathBenefitCode(List<BenefitCodeMapperEntity> benefitCodeMapperEntityList) {
        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {
            if (benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("DTHAC")) {
                benefitCodeMapperEntity.setAllianzBenefitCode("DTH");
            }
            if (benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Spouse-DTHAC")) {
                benefitCodeMapperEntity.setAllianzBenefitCode("Spouse-DTH");
            }
        });
    }

    private List<String> getFieldNamesInClass(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        // Spring's doWithFields iterates over all fields in the class hierarchy
        ReflectionUtils.doWithFields(clazz, field -> fieldNames.add(field.getName()));
        return fieldNames;
    }

    private Object getSpecificFieldValue(Object targetObject, String fieldName) {
        // 1. Find the field in the class
        Field field = ReflectionUtils.findFieldIgnoreCase(targetObject.getClass(), fieldName);
        if (field == null) {
            return null; // or throw an exception
        }
        // 2. Make it accessible (crucial for 'private' fields)
        ReflectionUtils.makeAccessible(field);
        // 3. Get the value
        return ReflectionUtils.getField(field, targetObject);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO; // Or return null if you prefer
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) return BigDecimal.ZERO;
            // Remove commas if present (e.g. "1,234.56")
            return new BigDecimal(str.replace(",", ""));
        }

        if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        }

        if (value instanceof Number) {
            // Important: Use .toString() for Doubles to avoid precision errors!
            // new BigDecimal(0.1) -> 0.10000000000000000555...
            // new BigDecimal("0.1") -> 0.1
            return new BigDecimal(value.toString());
        }

        throw new IllegalArgumentException("Cannot convert type " + value.getClass() + " to BigDecimal");
    }

    private BigDecimal toBigDecimalFromString(String value){
        try {
            return (value == null || value.isBlank())
                    ? BigDecimal.ZERO
                    : new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void setInPatientSA(List<String> policyNoList, List<MigrPolicyBenefitsEntity> policyBenefitsEntityList){
        log.info("Setting In Patient Cover for all benefits");
        if(!policyBenefitsEntityList.isEmpty()){
            policyNoList.forEach(policyNo -> {
                if(policyNo.contains(SUWASAHANA)){
                    Optional<MigrPolicyBenefitsEntity> mainInsuredInp = policyBenefitsEntityList.
                            stream().
                            filter(policyBenefit -> policyBenefit.getId().getPbPolicyNo().equalsIgnoreCase(policyNo) &&
                                    policyBenefit.getId().getPbBenefitCode().equalsIgnoreCase(IN_PATIENT_COVER)).findFirst();
                    Optional<MigrPolicyBenefitsEntity> mainInsuredBasicLife = policyBenefitsEntityList.
                            stream().
                            filter(policyBenefit -> policyBenefit.getId().getPbPolicyNo().equalsIgnoreCase(policyNo) &&
                                    policyBenefit.getId().getPbBenefitCode().equalsIgnoreCase(BASIC_LIFE_COVER)).findFirst();

                    Optional<MigrPolicyBenefitsEntity> spouseInp = policyBenefitsEntityList.
                            stream().
                            filter(policyBenefit -> policyBenefit.getId().getPbPolicyNo().equalsIgnoreCase(policyNo) &&
                                    policyBenefit.getId().getPbBenefitCode().equalsIgnoreCase(IN_PATIENT_COVER_SPOUSE)).findFirst();
                    Optional<MigrPolicyBenefitsEntity> spouseBasicLife = policyBenefitsEntityList.
                            stream().
                            filter(policyBenefit -> policyBenefit.getId().getPbPolicyNo().equalsIgnoreCase(policyNo) &&
                                    policyBenefit.getId().getPbBenefitCode().equalsIgnoreCase(BASIC_LIFE_COVER_SPOUSE)).findFirst();

                    if(mainInsuredInp.isPresent() && mainInsuredBasicLife.isPresent()){
                        mainInsuredInp.get().setPbCoverage(mainInsuredBasicLife.get().getPbCoverage());
                    }
                    if(spouseInp.isPresent() && spouseBasicLife.isPresent()){
                        spouseInp.get().setPbCoverage(spouseBasicLife.get().getPbCoverage());
                    }
                }
            });
            log.info("Saving updated benefit data into MSSQL");
            policyBenefitsEntityRepository.saveAll(policyBenefitsEntityList);

            log.info("Completed benefits saving. Setting up SAR");
            setSumAtRisk(policyNoList, policyBenefitsEntityList);
        }
    }

    private void setSumAtRisk(List<String> policyNoList,
                              List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {

        log.info("Setting sum at risk now using the benefit cover values");

        Map<String, List<MigrPolicyBenefitsEntity>> benefitsByPolicy =
                policyBenefitsEntityList.stream()
                        .collect(Collectors.groupingBy(e -> e.getId().getPbPolicyNo()));

        List<MigrPolicyData> allPolicies = findPoliciesInBatches(policyNoList);

        Map<String, MigrPolicyData> policyDataMap = allPolicies.stream()
                .collect(Collectors.toMap(MigrPolicyData::getLaPolicyNo, Function.identity()));

        Set<String> productCodes = new HashSet<>();
        Set<Integer> policyNumbers = new HashSet<>();

        for (String policy : policyNoList) {
            if (policy != null && policy.length() >= 3) {
                String code = policy.replace("/", "").substring(0, 3);
                Integer number = Integer.valueOf(policy.replace("/", "").substring(3));
                productCodes.add(code);
                policyNumbers.add(number);
            }
        }

        Map<String, MainDataReportEntity> mainDataMap =
                mainDataReportRepository.findFiltered(productCodes, policyNumbers).stream()
                        .collect(Collectors.toMap(
                                e -> e.getProductCode() + "/" + e.getPolicyNo(),
                                Function.identity()
                        ));

        List<MigrPolicyData> updatedPolicies = new ArrayList<>();

        for (String policy : policyNoList) {
            MigrPolicyData policyData = policyDataMap.get(policy);
            if (policyData == null) continue;

            String key = null;
            if (policy != null && policy.length() >= 3) {
                String code = policy.replace("/", "").substring(0, 3);
                int number = Integer.parseInt(policy.replace("/", "").substring(3));
                key = code + "/" + number;
            }

            MainDataReportEntity mainData = key != null ? mainDataMap.get(key) : null;

            List<MigrPolicyBenefitsEntity> benefits =
                    benefitsByPolicy.getOrDefault(policy, Collections.emptyList());

            Map<String, BigDecimal> benefitMap = benefits.stream()
                    .filter(b -> b.getId().getPbBenefitCode() != null)
                    .collect(Collectors.toMap(
                            b -> b.getId().getPbBenefitCode().toUpperCase(),
                            b -> safe(b.getPbCoverage()),
                            (a, b) -> a
                    ));

            BigDecimal dthSa  = benefitMap.getOrDefault(BASIC_LIFE_COVER.toUpperCase(), BigDecimal.ZERO);
            BigDecimal trSa   = benefitMap.getOrDefault(ADDITIONAL_DEATH_BENEFIT.toUpperCase(), BigDecimal.ZERO);
            BigDecimal cilxSa = benefitMap.getOrDefault(CRITICAL_ILLNESS.toUpperCase(), BigDecimal.ZERO);
            BigDecimal fibSa  = benefitMap.getOrDefault(FAMILY_INCOME_BENEFIT.toUpperCase(), BigDecimal.ZERO);

            Integer retirementTerm = mainData != null ? mainData.getRetirementBenefitPayoutTerm() : 0;

            policyData.setPoSumAtRisk(
                    getSumAtRiskValue(dthSa, trSa, cilxSa, fibSa, policyData.getPoTerm(), retirementTerm)
            );

            updatedPolicies.add(policyData);
        }

        log.info("Completed calculating sum at risk values. Saving data to policy table...");
        migrPolicyRepository.saveAll(updatedPolicies);
    }

    private BigDecimal getSumAtRiskValue(BigDecimal dthSa, BigDecimal trSa,
                                         BigDecimal cilxSa, BigDecimal fibSa,
                                         Integer term, Integer retirementTerm) {

        BigDecimal half = BigDecimal.valueOf(0.5);

        return safe(dthSa)
                .add(safe(trSa))
                .add(safe(cilxSa).multiply(half))
                .add(safe(fibSa).multiply(half).multiply(BigDecimal.valueOf(term - retirementTerm)));
    }

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private PolicyNumberResponseDTO extractPolicyNumberHelper (String policyRef) {
        return sharedFunction.extractPolicyNumber(policyRef);
    }

    private List<MigrPolicyData> findPoliciesInBatches(List<String> policyNoList) {

        int batchSize = 2000;
        List<MigrPolicyData> result = new ArrayList<>();

        for (int i = 0; i < policyNoList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, policyNoList.size());

            List<String> batch = policyNoList.subList(i, end);

            log.info("Fetching policies batch {} - {}", i + 1, end);

            result.addAll(migrPolicyRepository.findByLaPolicyNoIn(batch));
        }

        log.info("Fetched total {} policies", result.size());

        return result;
    }
}
