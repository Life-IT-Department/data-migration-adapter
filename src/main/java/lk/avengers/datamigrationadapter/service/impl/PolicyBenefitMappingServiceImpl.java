package lk.avengers.datamigrationadapter.service.impl;

import jakarta.persistence.Column;
import lk.avengers.datamigrationadapter.dto.ChildDto;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.dto.RiderCoverColumnDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BenefitCodeMapperEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBenefitsEntity;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ACPPolicyRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.BenefitCodeMapperRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyBenefitsEntityRepository;
import lk.avengers.datamigrationadapter.service.PolicyBenefitMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyBenefitMappingServiceImpl implements PolicyBenefitMappingService {

    private final BenefitCodeMapperRepository benefitCodeMapperRepository;
    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final ACPPolicyRepository acpPolicyRepository;
    private final PolicyBenefitsEntityRepository policyBenefitsEntityRepository;

    private final static List<String> policyList = List.of("ASP/888636", "SCL/1044031", "ULT/348672", "ULE/402719", "ULE/121830", "ULE/274795",
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

    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public CommonResponseDTO processBenefitCodeMapping() {
        log.info("Benefit code mapping process started...");
        List<BenefitCodeMapperEntity> benefitCodeMapperEntityList = benefitCodeMapperRepository.findAll();
        List<PolicyBenefitsEntity> policyBenefitsEntityList = new ArrayList<>();
        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {
            if (benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("DTHAC")) {
                benefitCodeMapperEntity.setAllianzBenefitCode("DTH");
            }
        });

        List<String> fieldNamesInMainDataEntityClassList = getFieldNamesInClass(MainDataReportEntity.class);

        AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList = new AtomicReference<>();
        AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList = new AtomicReference<>();
        AtomicReference<List<String>> childBenefitFieldNamesInMainDataEntityClassList = new AtomicReference<>();


        policyHolderBenefitFieldNamesInMainDataEntityClassList.set(new ArrayList<>());
        spouseBenefitFieldNamesInMainDataEntityClassList.set(new ArrayList<>());
        childBenefitFieldNamesInMainDataEntityClassList.set(new ArrayList<>());

        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {

            policyHolderBenefitFieldNamesInMainDataEntityClassList.get().addAll(fieldNamesInMainDataEntityClassList
                    .stream()
                    .filter(fieldName -> !fieldName.trim().toLowerCase().contains("Spouse".toLowerCase()))
                    .filter(fieldName -> !fieldName.trim().toLowerCase().contains("Child".toLowerCase()))
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList());

            spouseBenefitFieldNamesInMainDataEntityClassList.get().addAll(fieldNamesInMainDataEntityClassList
                    .stream()
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains("Spouse".toLowerCase()))
                    .filter(fieldName -> fieldName.trim().toLowerCase().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList());
        });

        Map<String, RiderCoverColumnDTO> policyHolderBenefitMap = new HashMap<>();
        Map<String, RiderCoverColumnDTO> spouseBenefitMap = new HashMap<>();

        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {
            List<String> filterList = policyHolderBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList();

            List<String> spouseFilterList = spouseBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().replaceFirst("^Spouse-", "").concat("_").toLowerCase().trim()))
                    .toList();

            if (!filterList.isEmpty() && !benefitCodeMapperEntity.getAllianzBenefitCode().startsWith("Spouse")) {
                RiderCoverColumnDTO riderCoverColumnDTO = RiderCoverColumnDTO.builder()
                        .coverName(filterList.stream().filter(name -> name.toLowerCase().contains("_sa")).findFirst().orElse(null))
                        .coverPerMilRate(filterList.stream().filter(name -> name.toLowerCase().contains("mil")).findFirst().orElse(null))
                        .coverOccupationExtraRate(filterList.stream().filter(name -> name.toLowerCase().contains("occ")).findFirst().orElse(null))
                        .build();
                policyHolderBenefitMap.put(benefitCodeMapperEntity.getSoftlogicBenefitCode(), riderCoverColumnDTO);
            }

            if (!spouseFilterList.isEmpty() && benefitCodeMapperEntity.getAllianzBenefitCode().startsWith("Spouse")) {
                RiderCoverColumnDTO spouseRiderCoverColumnDTO = RiderCoverColumnDTO.builder()
                        .coverName(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("_sa")).findFirst().orElse(null))
                        .coverPerMilRate(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("mil")).findFirst().orElse(null))
                        .coverOccupationExtraRate(spouseFilterList.stream().filter(name -> name.toLowerCase().contains("occ")).findFirst().orElse(null))
                        .build();
                spouseBenefitMap.put(benefitCodeMapperEntity.getSoftlogicBenefitCode(), spouseRiderCoverColumnDTO);
            }
        });


        policyList.forEach(policyNo -> {
            AtomicReference<MainDataReportEntity> mainDataReportEntityVal = new AtomicReference<>(new MainDataReportEntity());
            String[] policyNoSplit = policyNo.trim().split("/");
            mainDataReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                    .ifPresentOrElse(mainDataReportEntity -> {
                        mainDataReportEntityVal.set(mainDataReportEntity);
                        log.info("Main Data Report data found for Policy No: {}", policyNo);

                        policyHolderBenefitMap.forEach((key, value) -> {
                            if (value.getCoverName() != null) {
                                Object specificFieldValue = getSpecificFieldValue(mainDataReportEntity, value.getCoverName());
                                log.info("Specific field value for benefit code: {} and field: {} is: {}", key, value.getCoverName(), specificFieldValue);
                                PolicyBenefitsEntity benefitsEntity = new PolicyBenefitsEntity();

                                if (toBigDecimal(specificFieldValue).compareTo(BigDecimal.ZERO) > 0) {

                                    BigDecimal perMilRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverPerMilRate()));
                                    BigDecimal occupationExtraRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverOccupationExtraRate()));

                                    benefitsEntity.setPbPolicyNo(policyNo);
                                    benefitsEntity.setPbBenefitCode(key);
                                    benefitsEntity.setPbCoverage((BigDecimal) specificFieldValue);
                                    benefitsEntity.setPbPremPortion(null);
                                    benefitsEntity.setPbOption(null);
                                    benefitsEntity.setPbOccuExtra(occupationExtraRate);
                                    benefitsEntity.setPbExtraMortalityRate(perMilRate);
                                    benefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                                    benefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                                    policyBenefitsEntityList.add(benefitsEntity);
                                }
                            }
                        });

                        spouseBenefitMap.forEach((key, value) -> {
                            if (value.getCoverName() != null) {
                                Object specificFieldValue = getSpecificFieldValue(mainDataReportEntity, value.getCoverName());
                                log.info("Specific field value for benefit code: {} and field: {} is: {}", key, value.getCoverName(), specificFieldValue);
                                PolicyBenefitsEntity benefitsEntity = new PolicyBenefitsEntity();

                                if (toBigDecimal(specificFieldValue).compareTo(BigDecimal.ZERO) > 0) {

                                    BigDecimal perMilRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverPerMilRate()));
                                    BigDecimal occupationExtraRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverOccupationExtraRate()));

                                    benefitsEntity.setPbPolicyNo(policyNo);
                                    benefitsEntity.setPbBenefitCode(key);
                                    benefitsEntity.setPbCoverage((BigDecimal) specificFieldValue);
                                    benefitsEntity.setPbPremPortion(null);
                                    benefitsEntity.setPbOption(null);
                                    benefitsEntity.setPbOccuExtra(occupationExtraRate);
                                    benefitsEntity.setPbExtraMortalityRate(perMilRate);
                                    benefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                                    benefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                                    policyBenefitsEntityList.add(benefitsEntity);
                                }
                            }
                        });
                    }, () -> getDetailsFromALHMainData(policyNo, policyNoSplit, benefitCodeMapperEntityList));

            if (mainDataReportEntityVal.get().getChild1Name() != null) {
                if (mainDataReportEntityVal.get().getChild1Hbc_() != 0) {
                    PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();

                    policyBenefitsEntity.setPbPolicyNo(policyNo);
                    policyBenefitsEntity.setPbBenefitCode("ZCHB");
                    policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntityVal.get().getChild1Hbc_()));
                    policyBenefitsEntity.setPbPremPortion(null);
                    policyBenefitsEntity.setPbOption(null);
                    policyBenefitsEntity.setPbTerm(mainDataReportEntityVal.get().getTerm());
                    policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);

                    policyBenefitsEntityList.add(policyBenefitsEntity);
                }

            }

            if (mainDataReportEntityVal.get().getChild2Name() != null) {
                if (mainDataReportEntityVal.get().getChild2Hbc_() != 0) {
                    PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();

                    policyBenefitsEntity.setPbPolicyNo(policyNo);
                    policyBenefitsEntity.setPbBenefitCode("ZCHB");
                    policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntityVal.get().getChild2Hbc_()));
                    policyBenefitsEntity.setPbPremPortion(null);
                    policyBenefitsEntity.setPbOption(null);
                    policyBenefitsEntity.setPbTerm(mainDataReportEntityVal.get().getTerm());
                    policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);

                    policyBenefitsEntityList.add(policyBenefitsEntity);
                }
            }

            if (mainDataReportEntityVal.get().getChild3Name() != null) {
                if (mainDataReportEntityVal.get().getChild3Hbc_() != 0) {
                    PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();

                    policyBenefitsEntity.setPbPolicyNo(policyNo);
                    policyBenefitsEntity.setPbBenefitCode("ZCHB");
                    policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntityVal.get().getChild3Hbc_()));
                    policyBenefitsEntity.setPbPremPortion(null);
                    policyBenefitsEntity.setPbOption(null);
                    policyBenefitsEntity.setPbTerm(mainDataReportEntityVal.get().getTerm());
                    policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);

                    policyBenefitsEntityList.add(policyBenefitsEntity);
                }
            }

            if (mainDataReportEntityVal.get().getChild4Name() != null) {
                if (mainDataReportEntityVal.get().getChild4Hbc_() != 0) {
                    PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();

                    policyBenefitsEntity.setPbPolicyNo(policyNo);
                    policyBenefitsEntity.setPbBenefitCode("ZCHB");
                    policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntityVal.get().getChild4Hbc_()));
                    policyBenefitsEntity.setPbPremPortion(null);
                    policyBenefitsEntity.setPbOption(null);
                    policyBenefitsEntity.setPbTerm(mainDataReportEntityVal.get().getTerm());
                    policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);

                    policyBenefitsEntityList.add(policyBenefitsEntity);
                }
            }

            if (mainDataReportEntityVal.get().getChild5Name() != null) {
                if (mainDataReportEntityVal.get().getChild5Hbc_() != 0) {
                    PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();

                    policyBenefitsEntity.setPbPolicyNo(policyNo);
                    policyBenefitsEntity.setPbBenefitCode("ZCHB");
                    policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntityVal.get().getChild5Hbc_()));
                    policyBenefitsEntity.setPbPremPortion(null);
                    policyBenefitsEntity.setPbOption(null);
                    policyBenefitsEntity.setPbTerm(mainDataReportEntityVal.get().getTerm());
                    policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);

                    policyBenefitsEntityList.add(policyBenefitsEntity);
                }
            }

        });

        policyBenefitsEntityRepository.saveAll(policyBenefitsEntityList);

        return new CommonResponseDTO();

    }

    private void getDetailsFromALHMainData(String policyNo, String[] policyNoSplit,List<BenefitCodeMapperEntity> benefitCodeMapperEntityList) {
        List<PolicyBenefitsEntity> policyBenefitsEntityList = new ArrayList<>();

        mainDataALHReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                .ifPresentOrElse(mainDataALHReportEntity -> {
                    //TODO remove this
                    mainDataALHReportEntity.setDth_OccLoadingPercentage(0.0);
                    log.info("Main ALH Data Report data found for Policy No: {}", policyNo);
                    benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {
                        if (benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Spouse-DTHAC")) {
                            benefitCodeMapperEntity.setAllianzBenefitCode("Spouse-DTH");
                        }
                    });
                    if (mainDataALHReportEntity.getDth_Sar().compareTo(BigDecimal.ZERO) > 0) {
                        PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "DTH");
                        if (policyBenefitsEntity != null) {
                            policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getDth_OccLoadingPercentage()));
                            policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSubRateMilDth_());
                            policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getDth_Sar());
                            policyBenefitsEntityList.add(policyBenefitsEntity);
                        } else {
                            log.warn("No DTH benefit code found in benefit code mapper table for policy no: {}", policyNo);
                        }
                    }

                    if (mainDataALHReportEntity.getHb_Sa().compareTo(BigDecimal.ZERO) > 0) {
                        PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "HB");
                        if (policyBenefitsEntity != null) {
                            policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getHb_OccupationalLoadingPercentage()));
                            policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSubRateMilDth_());
                            policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getHb_Sa());
                            policyBenefitsEntityList.add(policyBenefitsEntity);
                        } else {
                            log.warn("No HB benefit code found in benefit code mapper table for policy no: {}", policyNo);
                        }
                    }

                    if (mainDataALHReportEntity.getInpSar().compareTo(BigDecimal.ZERO) > 0) {
                        PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "INP");
                        if (policyBenefitsEntity != null) {
                            policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getInpOccLoadingPercentage()));
                            policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSubRateMilInp());
                            policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getInpSar());
                            policyBenefitsEntityList.add(policyBenefitsEntity);
                        } else {
                            log.warn("No INP benefit code found in benefit code mapper table for policy no: {}", policyNo);
                        }
                    }

                    if (mainDataALHReportEntity.getSpousePin() != null && mainDataALHReportEntity.getSpousePin() != 0) {
                        setSpouseBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, policyBenefitsEntityList);
                    }

                    ChildDto child1Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild1Name())
                            .childHbc(Double.toString(mainDataALHReportEntity.getChild1Hbc_()))
                            .childInpSar(mainDataALHReportEntity.getChild1InpSar())
                            .build();

                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child1Dto, mainDataALHReportEntity, policyBenefitsEntityList);

                    // Child 2
                    ChildDto child2Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild2Name())
                            .childHbc(Double.toString(mainDataALHReportEntity.getChild2Hbc_()))
                            .childInpSar(mainDataALHReportEntity.getChild2InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child2Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 3
                    ChildDto child3Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild3Name())
                            .childHbc(Double.toString(mainDataALHReportEntity.getChild3Hbc_()))
                            .childInpSar(mainDataALHReportEntity.getChild3InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child3Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 4
                    ChildDto child4Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild4Name())
                            .childHbc(Double.toString(mainDataALHReportEntity.getChild4Hbc_()))
                            .childInpSar(mainDataALHReportEntity.getChild4InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child4Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 5
                    ChildDto child5Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild5Name())
                            .childHbc(Double.toString(mainDataALHReportEntity.getChild5Hbc_()))
                            .childInpSar(mainDataALHReportEntity.getChild5InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child5Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 6
                    ChildDto child6Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild6Name())
                            .childHbc(mainDataALHReportEntity.getChild6Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild6InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child6Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 7
                    ChildDto child7Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild7Name())
                            .childHbc(mainDataALHReportEntity.getChild7Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild7InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child7Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 8
                    ChildDto child8Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild8Name())
                            .childHbc(mainDataALHReportEntity.getChild8Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild8InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child8Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 9
                    ChildDto child9Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild9Name())
                            .childHbc(mainDataALHReportEntity.getChild9Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild9InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child9Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 10
                    ChildDto child10Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild10Name())
                            .childHbc(mainDataALHReportEntity.getChild10Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild10InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child10Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 11
                    ChildDto child11Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild11Name())
                            .childHbc(mainDataALHReportEntity.getChild11Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild11InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child11Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 12
                    ChildDto child12Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild12Name())
                            .childHbc(mainDataALHReportEntity.getChild12Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild12InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child12Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 13
                    ChildDto child13Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild13Name())
                            .childHbc(mainDataALHReportEntity.getChild13Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild13InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child13Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 14
                    ChildDto child14Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild14Name())
                            .childHbc(mainDataALHReportEntity.getChild14Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild14InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child14Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 15
                    ChildDto child15Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild15Name())
                            .childHbc(mainDataALHReportEntity.getChild15Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild15InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child15Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 16
                    ChildDto child16Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild16Name())
                            .childHbc(mainDataALHReportEntity.getChild16Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild16InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child16Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 17
                    ChildDto child17Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild17Name())
                            .childHbc(mainDataALHReportEntity.getChild17Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild17InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child17Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 18
                    ChildDto child18Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild18Name())
                            .childHbc(mainDataALHReportEntity.getChild18Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild18InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child18Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 19
                    ChildDto child19Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild19Name())
                            .childHbc(mainDataALHReportEntity.getChild19Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild19InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child19Dto, mainDataALHReportEntity, policyBenefitsEntityList);

// Child 20
                    ChildDto child20Dto = ChildDto.builder()
                            .name(mainDataALHReportEntity.getChild20Name())
                            .childHbc(mainDataALHReportEntity.getChild20Hbc().toPlainString())
                            .childInpSar(mainDataALHReportEntity.getChild20InpSar())
                            .build();
                    setChildBenefitsFromALHData(policyNo, benefitCodeMapperEntityList, child20Dto, mainDataALHReportEntity, policyBenefitsEntityList);

                    policyBenefitsEntityRepository.saveAll(policyBenefitsEntityList);
                }, () -> log.warn("No Main Data ALH Report data found for Policy No: {}", policyNo));
    }

    private void setChildBenefitsFromALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, ChildDto childDto, MainDataALHReportEntity mainDataALHReportEntity,
                                             List<PolicyBenefitsEntity> policyBenefitsEntityList) {

        if (childDto.getName() != null && !childDto.getName().isBlank() && !childDto.getName().isEmpty()) {

            if (childDto.getChildHbc() != null && Double.parseDouble(childDto.getChildHbc()) != 0.0) {
                benefitCodeMapperEntityList.stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-HBC"))
                        .findFirst()
                        .ifPresentOrElse(benefitCodeMapperEntity -> {
                            PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();
                            policyBenefitsEntity.setPbPolicyNo(policyNo);
                            policyBenefitsEntity.setPbBenefitCode(benefitCodeMapperEntity.getSoftlogicBenefitCode());
                            policyBenefitsEntity.setPbCoverage(new BigDecimal(childDto.getChildHbc()));
                            policyBenefitsEntity.setPbTerm(mainDataALHReportEntity.getTerm());
                            policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                            policyBenefitsEntityList.add(policyBenefitsEntity);

                        }, () -> log.warn("No Child-HBC benefit code found in benefit code mapper table for policy no: {}", policyNo));


            }

            if (childDto.getChildInpSar() != null && childDto.getChildInpSar().compareTo(BigDecimal.ZERO) > 0) {
                benefitCodeMapperEntityList.stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-INP"))
                        .findFirst()
                        .ifPresentOrElse(benefitCodeMapperEntity -> {
                            PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();
                            policyBenefitsEntity.setPbPolicyNo(policyNo);
                            policyBenefitsEntity.setPbBenefitCode(benefitCodeMapperEntity.getSoftlogicBenefitCode());
                            policyBenefitsEntity.setPbCoverage(childDto.getChildInpSar());
                            policyBenefitsEntity.setPbTerm(mainDataALHReportEntity.getTerm());
                            policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                            policyBenefitsEntityList.add(policyBenefitsEntity);

                        }, () -> log.warn("No Child-INP benefit code found in benefit code mapper table for policy no: {}", policyNo));


            }


        }
    }

    private void setSpouseBenefitsFromALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, MainDataALHReportEntity mainDataALHReportEntity, List<PolicyBenefitsEntity> policyBenefitsEntityList) {
        if (mainDataALHReportEntity.getSpouseDeathSa().compareTo(BigDecimal.ZERO) > 0) {
            PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-DTH");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseDeathOccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilDeath());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseDeathSa());
                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No Spouse-DTH benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }

        if (mainDataALHReportEntity.getSpouseHbSa().compareTo(BigDecimal.ZERO) > 0) {
            PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-HB");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseHbOccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilHb());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseHbSa());
                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No Spouse-HB benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }

        if (mainDataALHReportEntity.getSpouseInpSar().compareTo(BigDecimal.ZERO) > 0) {
            PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-INP");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseInpOccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilInp());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseInpSar());
                policyBenefitsEntityList.add(policyBenefitsEntity);
            } else {
                log.warn("No INP benefit code found in benefit code mapper table for policy no: {}", policyNo);
            }
        }
    }

    private PolicyBenefitsEntity getPolicyBenefitForALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList,
                                                            MainDataALHReportEntity mainDataALHReportEntity, String allianzBenefitCode) {
        AtomicReference<PolicyBenefitsEntity> benefitsEntity = new AtomicReference<>();

        benefitCodeMapperEntityList.stream()
                .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase(allianzBenefitCode))
                .findFirst().ifPresentOrElse(benefitCodeMapperEntity -> {
                    benefitsEntity.set(PolicyBenefitsEntity.builder()
                            .pbPolicyNo(policyNo.trim())
                            .pbBenefitCode(benefitCodeMapperEntity.getSoftlogicBenefitCode())
                            .pbTerm(mainDataALHReportEntity.getTerm())
                            .pbExtraPremium(BigDecimal.ZERO)
                            .build());
                }, () -> log.warn("No {} benefit code found in benefit code mapper table for policy no: {}", allianzBenefitCode, policyNo));
        return benefitsEntity.get();
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

    private Object getValueByColumnName(Object targetObject, String dbColumnName) {
        // We use AtomicReference to hold the result because
        // variables inside a lambda must be final/effectively final.
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Boolean> found = new AtomicReference<>(false);
        // Iterate over all fields in the class (and parent classes)
        ReflectionUtils.doWithFields(targetObject.getClass(), field -> {
            // Stop searching if we already found it
            if (found.get()) return;

            Column columnAnnotation = field.getAnnotation(Column.class);

            // Check if the @Column name matches your input
            if (columnAnnotation != null && columnAnnotation.name().equalsIgnoreCase(dbColumnName)) {
                // Make private fields readable
                ReflectionUtils.makeAccessible(field);
                // Get the value and store it
                result.set(ReflectionUtils.getField(field, targetObject));
                found.set(true);
            }
        });
        if (!found.get()) {
            throw new RuntimeException("getValueByColumnName Method No field found with @Column(name='" + dbColumnName + "')");
        }
        return result.get();
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

}
