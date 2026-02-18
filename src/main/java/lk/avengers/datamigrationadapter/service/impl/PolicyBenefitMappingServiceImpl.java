package lk.avengers.datamigrationadapter.service.impl;

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

        policyList.forEach(policyNo -> {
            String[] policyNoSplit = policyNo.trim().split("/");
            mainDataReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                    .ifPresentOrElse(mainDataReportEntity -> {
                        policyHolderBenefitMap
                                .forEach((key, value) -> generateBenefitsEntityFromMainData(policyNo, mainDataReportEntity, key, value, policyBenefitsEntityList));
                        spouseBenefitMap
                                .forEach((key, value) -> generateBenefitsEntityFromMainData(policyNo, mainDataReportEntity, key, value, policyBenefitsEntityList));

                        addChildBenefitsFromMainData(policyNo, mainDataReportEntity, benefitCodeMapperEntityList, policyBenefitsEntityList);

                    }, () -> getDetailsFromALHMainData(policyNo, policyNoSplit, benefitCodeMapperEntityList));
        });
        policyBenefitsEntityRepository.saveAll(policyBenefitsEntityList);
        return new CommonResponseDTO();
    }

    private void setFieldNamesInMainDataEntityClassLists(List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList, List<String> fieldNamesInMainDataEntityClassList, AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList) {
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
    }

    private void setMainDataBenefitsToHashMaps(List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, AtomicReference<List<String>> policyHolderBenefitFieldNamesInMainDataEntityClassList, AtomicReference<List<String>> spouseBenefitFieldNamesInMainDataEntityClassList, Map<String, RiderCoverColumnDTO> policyHolderBenefitMap, Map<String, RiderCoverColumnDTO> spouseBenefitMap) {
        benefitCodeMapperEntityList.forEach(benefitCodeMapperEntity -> {

            List<String> policyHolderFilterList = policyHolderBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().concat("_").toLowerCase().trim()))
                    .toList();

            List<String> spouseFilterList = spouseBenefitFieldNamesInMainDataEntityClassList.get().stream()
                    .filter(name -> name.toLowerCase().trim().contains(benefitCodeMapperEntity.getAllianzBenefitCode().replaceFirst("^Spouse-", "").concat("_").toLowerCase().trim()))
                    .toList();

            if (!policyHolderFilterList.isEmpty() && !benefitCodeMapperEntity.getAllianzBenefitCode().startsWith("Spouse")) {
                RiderCoverColumnDTO riderCoverColumnDTO = RiderCoverColumnDTO.builder()
                        .coverName(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("_sa")).findFirst().orElse(null))
                        .coverPerMilRate(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("mil")).findFirst().orElse(null))
                        .coverOccupationExtraRate(policyHolderFilterList.stream().filter(name -> name.toLowerCase().contains("occ")).findFirst().orElse(null))
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
    }

    private void addChildBenefitsFromMainData(String policyNo, MainDataReportEntity mainDataReportEntity, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, List<PolicyBenefitsEntity> policyBenefitsEntityList) {
        if (mainDataReportEntity.getChild1Name() != null && !mainDataReportEntity.getChild1Name().isBlank()) {
            if (mainDataReportEntity.getChild1Hbc_() != 0) {
                PolicyBenefitsEntity policyBenefitsEntity = new PolicyBenefitsEntity();
                String softlogicBenefitCode = benefitCodeMapperEntityList
                        .stream()
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-HBC"))
                        .findFirst().orElseThrow(() -> new RuntimeException("No Child-HBC benefit code found in benefit code mapper table for policy no: " + policyNo))
                        .getSoftlogicBenefitCode();
                policyBenefitsEntity.setPbPolicyNo(policyNo);
                policyBenefitsEntity.setPbBenefitCode(softlogicBenefitCode);
                policyBenefitsEntity.setPbCoverage(BigDecimal.valueOf(mainDataReportEntity.getChild1Hbc_()));
                policyBenefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                policyBenefitsEntityList.add(policyBenefitsEntity);
            }
        }
    }

    private void generateBenefitsEntityFromMainData(String policyNo, MainDataReportEntity mainDataReportEntity, String key, RiderCoverColumnDTO value, List<PolicyBenefitsEntity> policyBenefitsEntityList) {
        if (value.getCoverName() != null) {
            Object benefitSumAssuredFieldValue = getSpecificFieldValue(mainDataReportEntity, value.getCoverName());
            PolicyBenefitsEntity benefitsEntity = new PolicyBenefitsEntity();

            if (toBigDecimal(benefitSumAssuredFieldValue).compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal perMilRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverPerMilRate()));
                BigDecimal occupationExtraRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverOccupationExtraRate()));
                benefitsEntity.setPbPolicyNo(policyNo);
                benefitsEntity.setPbBenefitCode(key);
                benefitsEntity.setPbCoverage((BigDecimal) benefitSumAssuredFieldValue);
                benefitsEntity.setPbOccuExtra(occupationExtraRate);
                benefitsEntity.setPbExtraMortalityRate(perMilRate);
                benefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                benefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                policyBenefitsEntityList.add(benefitsEntity);
            }
        }
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

    private void getDetailsFromALHMainData(String policyNo, String[] policyNoSplit, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList) {
        List<PolicyBenefitsEntity> policyBenefitsEntityList = new ArrayList<>();
        mainDataALHReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                .ifPresentOrElse(mainDataALHReportEntity -> {
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
                            policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSubRateMilHb_());
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
                    policyBenefitsEntityRepository.saveAll(policyBenefitsEntityList);
                }, () -> log.warn("No Main Data or ALH Report data found for Policy No: {}", policyNo));
    }

    private void setChildBenefitsFromALHData(String policyNo, List<BenefitCodeMapperEntity> benefitCodeMapperEntityList, ChildDto childDto, MainDataALHReportEntity mainDataALHReportEntity,
                                             List<PolicyBenefitsEntity> policyBenefitsEntityList) {

        if (childDto.getName() != null && !childDto.getName().isBlank()) {

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
                        .filter(benefitCodeMapperEntity -> benefitCodeMapperEntity.getAllianzBenefitCode().equalsIgnoreCase("Child-INPC"))
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
        if (mainDataALHReportEntity.getSpouseDth_Sa().compareTo(BigDecimal.ZERO) > 0) {
            PolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "Spouse-DTH");
            if (policyBenefitsEntity != null) {
                policyBenefitsEntity.setPbOccuExtra(BigDecimal.valueOf(mainDataALHReportEntity.getSpouseDth_OccLoadingPercentage()));
                policyBenefitsEntity.setPbExtraMortalityRate(mainDataALHReportEntity.getSpouseSubRateMilDth_());
                policyBenefitsEntity.setPbCoverage(mainDataALHReportEntity.getSpouseDth_Sa());
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
