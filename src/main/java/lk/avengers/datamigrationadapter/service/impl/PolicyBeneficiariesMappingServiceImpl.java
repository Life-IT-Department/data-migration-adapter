package lk.avengers.datamigrationadapter.service.impl;

import lk.avengers.datamigrationadapter.dto.ChildDto;
import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataALHReportEntity;
import lk.avengers.datamigrationadapter.entity.postgresql.reportdb.MainDataReportEntity;
import lk.avengers.datamigrationadapter.entity.softlogicdb.PolicyBeneficiariesEntity;
import lk.avengers.datamigrationadapter.repository.softlogicdb.PolicyBeneficiariesRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataALHReportRepository;
import lk.avengers.datamigrationadapter.repository.postgresql.reportdb.MainDataReportRepository;
import lk.avengers.datamigrationadapter.service.PolicyBeneficiariesMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyBeneficiariesMappingServiceImpl implements PolicyBeneficiariesMappingService {

    private final MainDataReportRepository mainDataReportRepository;
    private final MainDataALHReportRepository mainDataALHReportRepository;
    private final PolicyBeneficiariesRepository policyBeneficiariesRepository;
    private final static String SPOUSE_TYPE = "Spouse";
    private final static String CHILD_TYPE = "Child";



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
            "SCL/834408", "SCL/873695", "SCL/846311", "SCL/893719", "SCL/1002906", "SCL/1082510", "SCL/859140", "SCL/905034", "SCL/974022",
            "SCL/974303", "SCL/974386", "SCL/992735", "SCL/992792", "SCL/992891", "SCL/994475", "SCL/1005792", "SCL/1005990", "SCL/1006303",
            "SCL/1048123", "SCL/1055102", "SCL/1055169", "SCL/1070424", "SCL/1071018", "SCL/1089275", "SCL/1089309", "SCL/1081926", "SCL/1005768",
            "SCL/935254", "SCL/935395", "SCL/935486", "SCL/935510","ULF/527879", "UPR/215038", "ULP/321661", "SCL/1038926", "UPR/399550","SCL/900753", "ULV/451021", "ULV/571323", "UPR/126334", "SCL/988394");


    @Transactional(transactionManager = "softlogicPlatformTransactionManager")
    @Override
    public CommonResponseDTO mapBeneficiaries() {
        log.info("mapBeneficiaries called.");
        CommonResponseDTO commonResponse = new CommonResponseDTO();
        policyList.forEach(policyNo -> {
            String[] policyNoSplit = policyNo.trim().split("/");

            mainDataReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                    .ifPresentOrElse(mainDataReportEntity -> {
                        log.info("Main Data Report data found for Policy No: {}", policyNo);
                        PolicyBeneficiariesEntity spouseDetailsFromMainData = getSpouseDetailsFromMainData(mainDataReportEntity, policyNo);
                        List<PolicyBeneficiariesEntity> childrenFromMainData = getChildrenFromMainData(mainDataReportEntity, policyNo);
                        if (spouseDetailsFromMainData != null) {
                            childrenFromMainData.add(spouseDetailsFromMainData);
                        }
                        policyBeneficiariesRepository.saveAll(childrenFromMainData);
                    }, () -> getDetailsFromALHMainData(policyNo, policyNoSplit));
        });
        return commonResponse;
    }

    private void getDetailsFromALHMainData(String policyNo, String[] policyNoSplit) {
        mainDataALHReportRepository.findFirstByProductCodeAndPolicyNo(policyNoSplit[0], Integer.parseInt(policyNoSplit[1]))
                .ifPresentOrElse(mainDataALHReportEntity -> {
                    log.info("Main ALH Data Report data found for Policy No: {}", policyNo);
                    PolicyBeneficiariesEntity spouseDetailsFromALHMainData = getSpouseDetailsFromALHMainData(mainDataALHReportEntity, policyNo);
                    List<PolicyBeneficiariesEntity> childrenFromALHMainData = getChildrenFromALHMainData(mainDataALHReportEntity, policyNo);

                    if (spouseDetailsFromALHMainData != null) {
                        childrenFromALHMainData.add(spouseDetailsFromALHMainData);
                    }
                    policyBeneficiariesRepository.saveAll(childrenFromALHMainData);
                }, () -> {
                    log.warn("No Main Data ALH Report data found for Policy No: {}", policyNo);
                });
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
            java.math.BigDecimal spouseHbSa = mainDataALHReportEntity.getSpouseHbSa();
            policyBeneficiariesEntity.setBeIsHb(spouseHbSa != null && spouseHbSa.compareTo(java.math.BigDecimal.ZERO) != 0);
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
            java.math.BigDecimal spouseHbSa = mainDataReportEntity.getSpouseChildHb_Sa();
            policyBeneficiariesEntity.setBeIsHb(spouseHbSa != null && spouseHbSa.compareTo(java.math.BigDecimal.ZERO) != 0);
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
                .childHbc(String.valueOf(mainDataReportEntity.getChild2Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild2Name())
                .dob(mainDataReportEntity.getChild2Dob())
                .age(mainDataReportEntity.getChild2Age())
                .childHbc(String.valueOf(mainDataReportEntity.getChild2Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild3Name())
                .dob(mainDataReportEntity.getChild3Dob())
                .age(mainDataReportEntity.getChild3Age())
                .childHbc(String.valueOf(mainDataReportEntity.getChild3Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild4Name())
                .dob(mainDataReportEntity.getChild4Dob())
                .age(mainDataReportEntity.getChild4Age())
                .childHbc(String.valueOf(mainDataReportEntity.getChild4Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataReportEntity.getChild5Name())
                .dob(mainDataReportEntity.getChild5Dob())
                .age(mainDataReportEntity.getChild5Age())
                .childHbc(String.valueOf(mainDataReportEntity.getChild5Hbc_()))
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
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild1Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild2Name())
                .dob(mainDataALHReportEntity.getChild2Dob())
                .age(mainDataALHReportEntity.getChild2Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild2Hbc_()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild3Name())
                .dob(mainDataALHReportEntity.getChild3Dob())
                .age(mainDataALHReportEntity.getChild3Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild3Hbc_().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild4Name())
                .dob(mainDataALHReportEntity.getChild4Dob())
                .age(mainDataALHReportEntity.getChild4Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild4Hbc_().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild5Name())
                .dob(mainDataALHReportEntity.getChild5Dob())
                .age(mainDataALHReportEntity.getChild5Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild5Hbc_().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild6Name())
                .dob(mainDataALHReportEntity.getChild6Dob())
                .age(mainDataALHReportEntity.getChild6Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild6Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild7Name())
                .dob(mainDataALHReportEntity.getChild7Dob())
                .age(mainDataALHReportEntity.getChild7Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild7Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild8Name())
                .dob(mainDataALHReportEntity.getChild8Dob())
                .age(mainDataALHReportEntity.getChild8Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild8Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild9Name())
                .dob(mainDataALHReportEntity.getChild9Dob())
                .age(mainDataALHReportEntity.getChild9Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild9Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild10Name())
                .dob(mainDataALHReportEntity.getChild10Dob())
                .age(mainDataALHReportEntity.getChild10Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild10Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild11Name())
                .dob(mainDataALHReportEntity.getChild11Dob())
                .age(mainDataALHReportEntity.getChild11Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild11Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild12Name())
                .dob(mainDataALHReportEntity.getChild12Dob())
                .age(mainDataALHReportEntity.getChild12Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild12Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild13Name())
                .dob(mainDataALHReportEntity.getChild13Dob())
                .age(mainDataALHReportEntity.getChild13Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild13Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild14Name())
                .dob(mainDataALHReportEntity.getChild14Dob())
                .age(mainDataALHReportEntity.getChild14Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild14Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild15Name())
                .dob(mainDataALHReportEntity.getChild15Dob())
                .age(mainDataALHReportEntity.getChild15Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild15Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild16Name())
                .dob(mainDataALHReportEntity.getChild16Dob())
                .age(mainDataALHReportEntity.getChild16Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild16Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild17Name())
                .dob(mainDataALHReportEntity.getChild17Dob())
                .age(mainDataALHReportEntity.getChild17Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild17Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild18Name())
                .dob(mainDataALHReportEntity.getChild18Dob())
                .age(mainDataALHReportEntity.getChild18Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild18Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild19Name())
                .dob(mainDataALHReportEntity.getChild19Dob())
                .age(mainDataALHReportEntity.getChild19Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild19Hbc().intValue()))
                .build());
        childDtoList.add(ChildDto.builder()
                .name(mainDataALHReportEntity.getChild20Name())
                .dob(mainDataALHReportEntity.getChild20Dob())
                .age(mainDataALHReportEntity.getChild20Age())
                .childHbc(String.valueOf(mainDataALHReportEntity.getChild20Hbc().intValue()))
                .build());

        setDataToPolicyBeneficiariesList(childDtoList, policyNumber, beneficiariesEntityList);
        return beneficiariesEntityList;
    }

    private static void setDataToPolicyBeneficiariesList(List<ChildDto> childDtoList, String policyNumber, List<PolicyBeneficiariesEntity> beneficiariesEntityList) {
        childDtoList.forEach(childDto -> {
            if (childDto.getName() != null && !childDto.getName().isEmpty() && !childDto.getName().isBlank()) {
                PolicyBeneficiariesEntity policyBeneficiariesEntity = new PolicyBeneficiariesEntity();
                policyBeneficiariesEntity.setBePolicyNo(policyNumber.trim());
                policyBeneficiariesEntity.setBeFullName(childDto.getName());
                policyBeneficiariesEntity.setBeAge(childDto.getAge());
                policyBeneficiariesEntity.setBeDob(childDto.getDob());
                policyBeneficiariesEntity.setBeType(CHILD_TYPE);
                policyBeneficiariesEntity.setBeIsHb(Double.parseDouble(childDto.getChildHbc()) != 0.0);
                beneficiariesEntityList.add(policyBeneficiariesEntity);
            }
        });
    }


    private Character getSex(String gender) {
        if (gender.equalsIgnoreCase("MALE")) {
            return 'M';
        } else if (gender.equalsIgnoreCase("FEMALE")) {
            return 'F';
        } else {
            return null;
        }
    }

}
