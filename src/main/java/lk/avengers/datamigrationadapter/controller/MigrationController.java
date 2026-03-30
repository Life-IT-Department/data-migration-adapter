package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("migrate")
public class MigrationController {

    private final ClaimsMappingService claimsMappingService;
    private final PolicyDataMigrationService policyDataMigrationService;
    private final PolicyBeneficiariesMappingService policyBeneficiariesMappingService;
    private final PolicyBenefitMappingService policyBenefitMappingService;
    private final PremiumsMappingService premiumsPaidMappingService;

    @GetMapping("/policy")
    public ResponseEntity<String> migrateMigrPolicyData() {
        log.info("MIGRATE_POLICY_DATA (STRING) METHOD ACCESSED.");

        policyDataMigrationService.migratePolicyData();
        return ResponseEntity.ok("Policy data migration successful") ;
    }

    @GetMapping("/beneficiaries")
    public CommonResponseDTO migrateBeneficiariesData() {
        log.info("MIGRATE BENEFICIARIES API METHOD ACCESSED.");
        return policyBeneficiariesMappingService.mapBeneficiaries();
    }

    @GetMapping("/benefits")
    public CommonResponseDTO migrateBenefitsData() {
        log.info("MIGRATE BENEFITS API METHOD ACCESSED.");
        return policyBenefitMappingService.processBenefitCodeMapping();
    }

    @GetMapping("/claims")
    public ResponseEntity<CommonResponseDTO> migrateClaimsData(){
        log.info("MIGRATE CLAIMS METHOD ACCESSED");
        return claimsMappingService.mapClaimsData();
    }

    @GetMapping("/premiums")
    public ResponseEntity<CommonResponseDTO> migratePremiumsPaid(){
        log.info("MIGRATE PREMIUMS PAID METHOD ACCESSED");
        return premiumsPaidMappingService.mapPremiumsPaid();
    }
}
