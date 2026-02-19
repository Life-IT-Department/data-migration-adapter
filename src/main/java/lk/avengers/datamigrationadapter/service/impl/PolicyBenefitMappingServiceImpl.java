package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.ChildDto;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.dto.RiderCoverColumnDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.BenefitCodeMapperEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyBenefitsEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.MigrPolicyBenefitsID;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.ACPPolicyRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.BenefitCodeMapperRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyBenefitsEntityRepository;
import lk.avengers.datamigrationadapter.service.PolicyBenefitMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
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
    private final MainExcelReader mainExcelReader;

    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public CommonResponseDTO processBenefitCodeMapping() {
        log.info("Benefit code mapping process started. Truncating table");


        policyBenefitsEntityRepository.truncate();

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

        policyList.forEach(policyNo -> {
            String[] policyNoSplit = policyNo.trim().split("/");
            mainDataReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                    .ifPresentOrElse(mainDataReportEntity -> {
                        log.info("Main Data Report data found for Policy No: {}", policyNo);
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
        }
    }

    private void generateBenefitsEntityFromMainData(String policyNo, MainDataReportEntity mainDataReportEntity, String key, RiderCoverColumnDTO value, List<MigrPolicyBenefitsEntity> policyBenefitsEntityList) {
        if (value.getCoverName() != null) {
            Object benefitSumAssuredFieldValue = getSpecificFieldValue(mainDataReportEntity, value.getCoverName());
            MigrPolicyBenefitsEntity benefitsEntity = new MigrPolicyBenefitsEntity();

            if (toBigDecimal(benefitSumAssuredFieldValue).compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal perMilRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverPerMilRate()));
                BigDecimal occupationExtraRate = toBigDecimal(getSpecificFieldValue(mainDataReportEntity, value.getCoverOccupationExtraRate()));

                benefitsEntity.setId(new MigrPolicyBenefitsID(policyNo, key));
                benefitsEntity.setPbCoverage((BigDecimal) benefitSumAssuredFieldValue);
                benefitsEntity.setPbOccuExtra(occupationExtraRate);
                benefitsEntity.setPbExtraMortalityRate(perMilRate);
                benefitsEntity.setPbTerm(mainDataReportEntity.getTerm());
                benefitsEntity.setPbExtraPremium(BigDecimal.ZERO);
                benefitsEntity.setPbPremPortion(BigDecimal.ZERO);
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
        List<MigrPolicyBenefitsEntity> policyBenefitsEntityList = new ArrayList<>();
        mainDataALHReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                .ifPresentOrElse(mainDataALHReportEntity -> {
                    log.info("Main ALH Data Report data found for Policy No: {}", policyNo);

                    if (mainDataALHReportEntity.getDth_Sar().compareTo(BigDecimal.ZERO) > 0) {
                        MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "DTH");
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
                        MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "HB");
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
                        MigrPolicyBenefitsEntity policyBenefitsEntity = getPolicyBenefitForALHData(policyNo, benefitCodeMapperEntityList, mainDataALHReportEntity, "INP");
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
                            policyBenefitsEntity.setPbExtraPremium(BigDecimal.ZERO);policyBenefitsEntity.setPbPremPortion(BigDecimal.ZERO);
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
