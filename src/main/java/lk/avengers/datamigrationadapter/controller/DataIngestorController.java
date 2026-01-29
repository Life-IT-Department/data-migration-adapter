package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.service.DataIngestorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("data-ingestor")
public class DataIngestorController {

    private final DataIngestorService dataIngestorService;

    @PostMapping("/extract-acp")
    public ResponseEntity<String> retrieveACPData(@RequestHeader String uuid, @RequestParam MultipartFile file) {
        log.info("UUID: {} RETRIEVE_ACP_DATA (STRING, MULTIPART_FILE) METHOD ACCESSED.", uuid);

        dataIngestorService.ProcessACPData(uuid, file);

        return ResponseEntity.ok("ACP Data extracted successfully.") ;
    }

    @PostMapping("/extract-policy-list")
    public ResponseEntity<String> retrievePolicyListData(@RequestHeader String uuid, @RequestParam MultipartFile file) {
      log.info("UUID: {} RETRIEVE_POLICY_LIST_DATA (STRING, MULTIPART_FILE) METHOD ACCESSED.", uuid);

        dataIngestorService.ProcessPolicyListData(uuid, file);

        return ResponseEntity.ok("Policy Data extracted successfully.") ;
    }

    @PostMapping("/extract-contact-detail")
    public ResponseEntity<String> retrieveContactDetailData(@RequestHeader String uuid) {
        log.info("UUID: {} RETRIEVE_CONTACT_DETAIL_DATA (STRING) METHOD ACCESSED.", uuid);

       dataIngestorService.ProcessContactDetailData(uuid);

        return ResponseEntity.ok("Contact Detail Data extracted successfully.") ;
    }

}
