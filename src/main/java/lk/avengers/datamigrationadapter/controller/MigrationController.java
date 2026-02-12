package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.service.ClaimsMappingService;
import lk.avengers.datamigrationadapter.service.MigrationService;
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

    @PostMapping("/policy")
    public ResponseEntity<String> migratePolicyData(@RequestHeader String uuid) {
        log.info("UUID: {} MIGRATE_POLICY_DATA (STRING) METHOD ACCESSED.", uuid);

       migrationService.migratePolicyData(uuid);

        return ResponseEntity.ok("Policy data migration successfully.") ;
    }

    @GetMapping("/claims")
    public ResponseEntity<CommonResponseDTO> migrateClaimsData(){
        log.info("MIGRATE CLAIMS METHOD ACCESSED");
        return claimsMappingService.mapClaimsData();
    }

}
