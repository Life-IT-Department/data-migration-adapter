package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.ChildDto;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.dto.response.PolicyNumberResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBeneficiariesEntity;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyBeneficiariesRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.service.PolicyBeneficiariesMappingService;
import lk.avengers.datamigrationadapter.util.MainExcelReader;
import lk.avengers.datamigrationadapter.util.SharedFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyBeneficiariesMappingServiceImpl implements PolicyBeneficiariesMappingService {

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final PolicyBeneficiariesRepository policyBeneficiariesRepository;
    private final MainExcelReader mainExcelReader;
    private final SharedFunction sharedFunction;

    private final static String SPOUSE_TYPE = "Spouse";
    private final static String CHILD_TYPE = "Child";

    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public CommonResponseDTO mapBeneficiaries() {
        log.info("Beneficiaries mapping process started. Truncating table");
        policyBeneficiariesRepository.truncate();
        log.info("Truncating Table process completed");

        List<String> policyList = mainExcelReader.readPolicyNumbers();

        List<String> successfulPolicyList = new ArrayList<>();

        // ================= PRELOAD =================
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

        // ================= PROCESS =================
        List<PolicyBeneficiariesEntity> allBeneficiariesEntityList = new ArrayList<>();

        for (String policyNo : policyList) {

            PolicyNumberResponseDTO extracted = extractPolicyNumberHelper(policyNo);
            String key = extracted.getProductCode() + "/" + extracted.getPolicyNo();

            MainDataReportEntity mainData = mainDataMap.get(key);
            MainDataALHReportEntity alhData = alhMap.get(key);

            // ===== MAIN DATA =====
            if (mainData != null) {

                if (!sharedFunction.isEligiblePolicyStatus(mainData.getStatus())) {
                    log.info("Skipping policy {} due to status {}", policyNo, mainData.getStatus());
                    continue;
                }
                successfulPolicyList.add(policyNo);
                PolicyBeneficiariesEntity spouse = getSpouseDetailsFromMainData(mainData, policyNo);
                List<PolicyBeneficiariesEntity> children = getChildrenFromMainData(mainData, policyNo);

                if (spouse != null) {
                    children.add(spouse);
                }

                allBeneficiariesEntityList.addAll(children);
                continue;
            }

            // ===== ALH DATA =====
            if (alhData != null) {

                if (!sharedFunction.isEligiblePolicyStatus(alhData.getStatus())) {
                    log.info("Skipping policy {} due to ALH status {}", policyNo, alhData.getStatus());
                    continue;
                }
                successfulPolicyList.add(policyNo);
                PolicyBeneficiariesEntity spouse = getSpouseDetailsFromALHMainData(alhData, policyNo);
                List<PolicyBeneficiariesEntity> children = getChildrenFromALHMainData(alhData, policyNo);

                if (spouse != null) {
                    children.add(spouse);
                }

                allBeneficiariesEntityList.addAll(children);
                continue;
            }

            // ===== NO DATA =====
            log.warn("No Main Data or ALH data found for Policy No: {}", policyNo);
        }

        // ================= SAVE =================
        if (!allBeneficiariesEntityList.isEmpty()) {
            saveInBatches(allBeneficiariesEntityList, policyBeneficiariesRepository);
        } else {
            log.warn("No beneficiaries records found to save. Please check the excel file and try again.");
            return CommonResponseDTO.builder()
                    .message("No beneficiaries records found to save. Please check the excel file and try again.")
                    .status(HttpStatus.BAD_REQUEST.toString())
                    .build();
        }

        log.info("Policy beneficiaries saved successfully. {} policy beneficiaries records were saved from {} policies.",
                allBeneficiariesEntityList.size(), successfulPolicyList.size());

        return CommonResponseDTO.builder()
                .message(String.format("%d beneficiaries records were saved from %d policies",
                        allBeneficiariesEntityList.size(), successfulPolicyList.size()))
                .status(HttpStatus.OK.toString())
                .build();
    }

    private PolicyBeneficiariesEntity getSpouseDetailsFromALHMainData(MainDataALHReportEntity mainDataALHReportEntity, String policyNo) {
        // Spouse
        if (mainDataALHReportEntity.getSpousePin() != 0) {
            PolicyBeneficiariesEntity policyBeneficiariesEntity = new PolicyBeneficiariesEntity();
            policyBeneficiariesEntity.setBePolicyNo(policyNo);
            policyBeneficiariesEntity.setBeFullName(mainDataALHReportEntity.getSpouseFullName());
            policyBeneficiariesEntity.setBeAge(mainDataALHReportEntity.getSpouseAge());
            policyBeneficiariesEntity.setBeSex(getSex(mainDataALHReportEntity.getSpouseGender()));
            policyBeneficiariesEntity.setBeDob(mainDataALHReportEntity.getSpouseDob());
            policyBeneficiariesEntity.setBeType(SPOUSE_TYPE);
            policyBeneficiariesEntity.setBeNic(mainDataALHReportEntity.getSpouseIdCardNumber());

            java.math.BigDecimal spouseHbSa = mainDataALHReportEntity.getSpouseHbSa();
            policyBeneficiariesEntity.setBeIsHb(spouseHbSa != null && spouseHbSa.compareTo(java.math.BigDecimal.ZERO) != 0);

            policyBeneficiariesEntity.setChildCode(0);
            policyBeneficiariesEntity.setInpcSa(mainDataALHReportEntity.getSpouseInpSar());
            policyBeneficiariesEntity.setHbcSa(mainDataALHReportEntity.getSpouseHbSa());
            policyBeneficiariesEntity.setBonus(mainDataALHReportEntity.getSpouseBonus());
            policyBeneficiariesEntity.setBasicSumAssured(mainDataALHReportEntity.getBasicSumAssured());

            policyBeneficiariesEntity.setBePin(mainDataALHReportEntity.getSpousePin());

            return policyBeneficiariesEntity;
        }
        return null;
    }

    private PolicyBeneficiariesEntity getSpouseDetailsFromMainData(MainDataReportEntity mainDataReportEntity, String policyNo) {
        // Spouse
        if (mainDataReportEntity.getSpouseChildPin() != 0) {
            PolicyBeneficiariesEntity policyBeneficiariesEntity = new PolicyBeneficiariesEntity();
            policyBeneficiariesEntity.setBePolicyNo(policyNo);
            policyBeneficiariesEntity.setBeFullName(mainDataReportEntity.getSpouseChildFullName());
            policyBeneficiariesEntity.setBeAge(mainDataReportEntity.getSpouseChildAge());
            policyBeneficiariesEntity.setBeSex(getSex(mainDataReportEntity.getSpouseChildGender()));
            policyBeneficiariesEntity.setBeDob(mainDataReportEntity.getSpouseChildDob());
            policyBeneficiariesEntity.setBeType(SPOUSE_TYPE);
            policyBeneficiariesEntity.setBeNic(mainDataReportEntity.getSpouseIdCardNumber());
            policyBeneficiariesEntity.setBeInclusionDate(mainDataReportEntity.getSpouseDth_InclusionDate());

            java.math.BigDecimal spouseHbSa = mainDataReportEntity.getSpouseChildHb_Sa();
            policyBeneficiariesEntity.setBeIsHb(spouseHbSa != null && spouseHbSa.compareTo(java.math.BigDecimal.ZERO) != 0);

            policyBeneficiariesEntity.setChildCode(0);
            policyBeneficiariesEntity.setInpcSa(BigDecimal.ZERO);
            policyBeneficiariesEntity.setHbcSa(BigDecimal.ZERO);
            policyBeneficiariesEntity.setBonus(BigDecimal.ZERO);
            policyBeneficiariesEntity.setBasicSumAssured(mainDataReportEntity.getBasicSumAssured());

            policyBeneficiariesEntity.setBePin(mainDataReportEntity.getSpouseChildPin());

            return policyBeneficiariesEntity;
        }
        return null;
    }

    private List<PolicyBeneficiariesEntity> getChildrenFromMainData(MainDataReportEntity mainDataReportEntity, String policyNumber) {
        // Children
        List<PolicyBeneficiariesEntity> beneficiariesEntityList = new ArrayList<>();
        List<ChildDto> childDtoList = new ArrayList<>();

        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild1Name())
                .dob(mainDataReportEntity.getChild1Dob())
                .age(mainDataReportEntity.getChild1Age())
                .gender(mainDataReportEntity.getChild1Gender())
                .childHbc(String.valueOf(mainDataReportEntity.getChild1Hbc_()))

                .childCode(1)
                .inpcSa(BigDecimal.ZERO)
                .hbcSa(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .basicSumAssured(mainDataReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild2Name())
                .dob(mainDataReportEntity.getChild2Dob())
                .age(mainDataReportEntity.getChild2Age())
                .gender(mainDataReportEntity.getChild2Gender())
                .childHbc(String.valueOf(mainDataReportEntity.getChild2Hbc_()))

                .childCode(2)
                .inpcSa(BigDecimal.ZERO)
                .hbcSa(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .basicSumAssured(mainDataReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild3Name())
                .dob(mainDataReportEntity.getChild3Dob())
                .age(mainDataReportEntity.getChild3Age())
                .gender(mainDataReportEntity.getChild3Gender())
                .childHbc(String.valueOf(mainDataReportEntity.getChild3Hbc_()))

                .childCode(3)
                .inpcSa(BigDecimal.ZERO)
                .hbcSa(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .basicSumAssured(mainDataReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild4Name())
                .dob(mainDataReportEntity.getChild4Dob())
                .age(mainDataReportEntity.getChild4Age())
                .gender(mainDataReportEntity.getChild4Gender())
                .childHbc(String.valueOf(mainDataReportEntity.getChild4Hbc_()))

                .childCode(4)
                .inpcSa(BigDecimal.ZERO)
                .hbcSa(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .basicSumAssured(mainDataReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild5Name())
                .dob(mainDataReportEntity.getChild5Dob())
                .age(mainDataReportEntity.getChild5Age())
                .gender(mainDataReportEntity.getChild5Gender())
                .childHbc(String.valueOf(mainDataReportEntity.getChild5Hbc_()))

                .childCode(5)
                .inpcSa(BigDecimal.ZERO)
                .hbcSa(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .basicSumAssured(mainDataReportEntity.getBasicSumAssured())

                .build());

        setDataToPolicyBeneficiariesList(childDtoList, policyNumber, beneficiariesEntityList);
        return beneficiariesEntityList;
    }

    private List<PolicyBeneficiariesEntity> getChildrenFromALHMainData(MainDataALHReportEntity mainDataALHReportEntity, String policyNumber) {
        // Children
        List<PolicyBeneficiariesEntity> beneficiariesEntityList = new ArrayList<>();
        List<ChildDto> childDtoList = new ArrayList<>();

        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild1Name())
                .dob(mainDataALHReportEntity.getChild1Dob())
                .age(mainDataALHReportEntity.getChild1Age())
                .gender(mainDataALHReportEntity.getChild1Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild1Hbc_()))

                .childCode(1)
                .inpcSa(mainDataALHReportEntity.getChild1InpSar())
                .hbcSa(BigDecimal.valueOf(mainDataALHReportEntity.getChild1Hbc_()))
                .bonus(mainDataALHReportEntity.getChild1Bonus())
                .basicSumAssured(mainDataALHReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild2Name())
                .dob(mainDataALHReportEntity.getChild2Dob())
                .age(mainDataALHReportEntity.getChild2Age())
                .gender(mainDataALHReportEntity.getChild2Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild2Hbc_()))

                .childCode(2)
                .inpcSa(mainDataALHReportEntity.getChild2InpSar())
                .hbcSa(BigDecimal.valueOf(mainDataALHReportEntity.getChild2Hbc_()))
                .bonus(mainDataALHReportEntity.getChild2Bonus())
                .basicSumAssured(mainDataALHReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild3Name())
                .dob(mainDataALHReportEntity.getChild3Dob())
                .age(mainDataALHReportEntity.getChild3Age())
                .gender(mainDataALHReportEntity.getChild3Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild3Hbc_().intValue()))

                .childCode(3)
                .inpcSa(mainDataALHReportEntity.getChild3InpSar())
                .hbcSa(BigDecimal.valueOf(mainDataALHReportEntity.getChild3Hbc_()))
                .bonus(mainDataALHReportEntity.getChild3Bonus())
                .basicSumAssured(mainDataALHReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild4Name())
                .dob(mainDataALHReportEntity.getChild4Dob())
                .age(mainDataALHReportEntity.getChild4Age())
                .gender(mainDataALHReportEntity.getChild4Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild4Hbc_().intValue()))

                .childCode(4)
                .inpcSa(mainDataALHReportEntity.getChild4InpSar())
                .hbcSa(BigDecimal.valueOf(mainDataALHReportEntity.getChild4Hbc_()))
                .bonus(mainDataALHReportEntity.getChild4Bonus())
                .basicSumAssured(mainDataALHReportEntity.getBasicSumAssured())

                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild5Name())
                .dob(mainDataALHReportEntity.getChild5Dob())
                .age(mainDataALHReportEntity.getChild5Age())
                .gender(mainDataALHReportEntity.getChild5Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild5Hbc_().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild6Name())
                .dob(mainDataALHReportEntity.getChild6Dob())
                .age(mainDataALHReportEntity.getChild6Age())
                .gender(mainDataALHReportEntity.getChild6Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild6Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild7Name())
                .dob(mainDataALHReportEntity.getChild7Dob())
                .age(mainDataALHReportEntity.getChild7Age())
                .gender(mainDataALHReportEntity.getChild7Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild7Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild8Name())
                .dob(mainDataALHReportEntity.getChild8Dob())
                .age(mainDataALHReportEntity.getChild8Age())
                .gender(mainDataALHReportEntity.getChild8Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild8Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild9Name())
                .dob(mainDataALHReportEntity.getChild9Dob())
                .age(mainDataALHReportEntity.getChild9Age())
                .gender(mainDataALHReportEntity.getChild9Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild9Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild10Name())
                .dob(mainDataALHReportEntity.getChild10Dob())
                .age(mainDataALHReportEntity.getChild10Age())
                .gender(mainDataALHReportEntity.getChild10Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild10Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild11Name())
                .dob(mainDataALHReportEntity.getChild11Dob())
                .age(mainDataALHReportEntity.getChild11Age())
                .gender(mainDataALHReportEntity.getChild11Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild11Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild12Name())
                .dob(mainDataALHReportEntity.getChild12Dob())
                .age(mainDataALHReportEntity.getChild12Age())
                .gender(mainDataALHReportEntity.getChild12Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild12Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild13Name())
                .dob(mainDataALHReportEntity.getChild13Dob())
                .age(mainDataALHReportEntity.getChild13Age())
                .gender(mainDataALHReportEntity.getChild13Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild13Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild14Name())
                .dob(mainDataALHReportEntity.getChild14Dob())
                .age(mainDataALHReportEntity.getChild14Age())
                .gender(mainDataALHReportEntity.getChild14Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild14Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild15Name())
                .dob(mainDataALHReportEntity.getChild15Dob())
                .age(mainDataALHReportEntity.getChild15Age())
                .gender(mainDataALHReportEntity.getChild15Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild15Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild16Name())
                .dob(mainDataALHReportEntity.getChild16Dob())
                .age(mainDataALHReportEntity.getChild16Age())
                .gender(mainDataALHReportEntity.getChild16Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild16Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild17Name())
                .dob(mainDataALHReportEntity.getChild17Dob())
                .age(mainDataALHReportEntity.getChild17Age())
                .gender(mainDataALHReportEntity.getChild17Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild17Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild18Name())
                .dob(mainDataALHReportEntity.getChild18Dob())
                .age(mainDataALHReportEntity.getChild18Age())
                .gender(mainDataALHReportEntity.getChild18Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild18Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild19Name())
                .dob(mainDataALHReportEntity.getChild19Dob())
                .age(mainDataALHReportEntity.getChild19Age())
                .gender(mainDataALHReportEntity.getChild19Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild19Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild20Name())
                .dob(mainDataALHReportEntity.getChild20Dob())
                .age(mainDataALHReportEntity.getChild20Age())
                .gender(mainDataALHReportEntity.getChild20Gender())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild20Hbc().intValue()))
                .build());

        setDataToPolicyBeneficiariesList(childDtoList, policyNumber, beneficiariesEntityList);
        return beneficiariesEntityList;
    }

    private void setDataToPolicyBeneficiariesList(List<ChildDto> childDtoList, String policyNumber, List<PolicyBeneficiariesEntity> beneficiariesEntityList) {
        childDtoList.forEach(childDto -> {
            if (childDto.getName() != null && !childDto.getName().isEmpty() && !childDto.getName().isBlank()) {
                PolicyBeneficiariesEntity policyBeneficiariesEntity = new PolicyBeneficiariesEntity();
                policyBeneficiariesEntity.setBePolicyNo(policyNumber.trim());
                policyBeneficiariesEntity.setBeFullName(childDto.getName());
                policyBeneficiariesEntity.setBeAge(childDto.getAge());
                policyBeneficiariesEntity.setBeDob(childDto.getDob());
                policyBeneficiariesEntity.setBeType(CHILD_TYPE);
                policyBeneficiariesEntity.setBeIsHb(Double.parseDouble(childDto.getChildHbc()) != 0.0);
                policyBeneficiariesEntity.setBeSex(getSex(childDto.getGender()));

                policyBeneficiariesEntity.setChildCode(childDto.getChildCode());
                policyBeneficiariesEntity.setHbcSa(childDto.getHbcSa());
                policyBeneficiariesEntity.setInpcSa(childDto.getInpcSa());
                policyBeneficiariesEntity.setBonus(childDto.getBonus());
                policyBeneficiariesEntity.setBasicSumAssured(childDto.getBasicSumAssured());

                beneficiariesEntityList.add(policyBeneficiariesEntity);
            }
        });
    }


    private Character getSex(String gender) {
        if (gender != null && !gender.isBlank()) {
            if (gender.equalsIgnoreCase("MALE") || gender.equalsIgnoreCase("M")) {
                return 'M';
            } else if (gender.equalsIgnoreCase("FEMALE") || gender.equalsIgnoreCase("F")) {
                return 'F';
            } else {
                return null;
            }
        } else {
            return 'M';
        }
    }

    private PolicyNumberResponseDTO extractPolicyNumberHelper (String policyRef) {
        return sharedFunction.extractPolicyNumber(policyRef);
    }

    private <T> void saveInBatches(List<T> list, JpaRepository<T, ?> repository) {
        int batchSize = 1000;
        int totalSize = list.size();
        int totalBatches = (int) Math.ceil((double) totalSize / batchSize);

        log.info("Starting batch save: totalRecords={}, batchSize={}, totalBatches={}, repository={}",
                totalSize, batchSize, totalBatches, repository.getClass().getSimpleName());

        for (int i = 0; i < list.size(); i += 1000) {
            int batchNumber = (i / batchSize) + 1;
            int end = Math.min(i + 1000, list.size());

            log.info("Saving batch {}/{} (records {} - {})",
                    batchNumber, totalBatches, i + 1, end);

            List<T> batch = list.subList(i, end);
            repository.saveAll(batch);
            repository.flush();
        }
        log.info("Completed batch save: totalRecords={}, repository={}",
                totalSize, repository.getClass().getSimpleName());
    }

}
