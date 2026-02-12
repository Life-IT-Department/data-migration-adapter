package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("migrate")
public class MigrationController {

    public final MigrationService  migrationService;

    @PostMapping("/policy")
    public ResponseEntity<String> migratePolicyData(@RequestHeader String uuid) {
        log.info("UUID: {} MIGRATE_POLICY_DATA (STRING) METHOD ACCESSED.", uuid);

       migrationService.migratePolicyData(uuid);

        return ResponseEntity.ok("Policy data migration successfully.") ;
    }

}
