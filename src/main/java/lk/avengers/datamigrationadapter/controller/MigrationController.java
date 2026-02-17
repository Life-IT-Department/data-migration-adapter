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
@RequestMapping("migrate")
public class MigrationController {

    public final MigrationService  migrationService;
    private final ClaimsMappingService claimsMappingService;
    private final MigrationMigrService migrationMigrService;
    private final PolicyBeneficiariesMappingService policyBeneficiariesMappingService;
    private final PolicyBenefitMappingService policyBenefitMappingService;

    @GetMapping("/policy")
    public ResponseEntity<String> migratePolicyData(@RequestHeader String uuid) {
        log.info("UUID: {} MIGRATE_POLICY_DATA (STRING) METHOD ACCESSED.", uuid);

       migrationService.migratePolicyData(uuid);

        return ResponseEntity.ok("Policy data migration successful") ;
    }

    @GetMapping("/beneficiaries")
    public CommonResponseDTO migrateBeneficiariesData() {
        log.info("MIGRATE BENEFICIARIES API METHOD ACCESSED.");
        return policyBeneficiariesMappingService.mapBeneficiaries();
    }

    @GetMapping("/benefits")
    public CommonResponseDTO test2() {
        log.info("MIGRATE BENEFITS API METHOD ACCESSED.");
        return policyBenefitMappingService.processBenefitCodeMapping();
    }

    @GetMapping("/claims")
    public ResponseEntity<CommonResponseDTO> migrateClaimsData(){
        log.info("MIGRATE CLAIMS METHOD ACCESSED");
        return claimsMappingService.mapClaimsData();
    }

    @GetMapping("/policy-migr")
    public ResponseEntity<String> migrateMigrPolicyData() {
        log.info("MIGRATE_POLICY_DATA (STRING) METHOD ACCESSED.");

        migrationMigrService.migratePolicyData();
        return ResponseEntity.ok("Policy data migration successful") ;
    }
}
