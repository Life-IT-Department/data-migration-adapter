package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.service.CashFlowReportUploadService;
import lk.avengers.datamigrationadapter.service.ContactDetailReportService;
import lk.avengers.datamigrationadapter.service.MainDataReportUploadService;
import lk.avengers.datamigrationadapter.service.PosSignatureReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("report-upload")
@RequiredArgsConstructor
public class ReportUploadController {

    private final MainDataReportUploadService mainDataReportUploadService;
    private final CashFlowReportUploadService cashFlowReportUploadService;
    private final ContactDetailReportService contactDetailReportService;
    private final PosSignatureReportService posSignatureReportService;

    @PostMapping("/cash-flow-report")
    public ResponseEntity<CommonResponseDTO> uploadCashFlow(@RequestParam int year) {
        log.info("CASH_FLOW_REPORT_UPLOAD API METHOD ACCESSED.");
        return cashFlowReportUploadService.uploadCashFlowReport(year);
    }

    @GetMapping("/contact-detail-report")
    public ResponseEntity<CommonResponseDTO> uploadContactDetail() {
        log.info("CONTACT_DETAIL_REPORT_UPLOAD API METHOD ACCESSED.");
        return contactDetailReportService.processExcel();
    }

    @GetMapping("/paid-claim-report")
    public ResponseEntity<CommonResponseDTO> updatePaidClaimReport() {
        log.info("PAID_CLAIM_REPORT_UPLOAD API METHOD ACCESSED.");
        return contactDetailReportService.processExcel();
    }


    @GetMapping("/pos-signature-report")
    public ResponseEntity<CommonResponseDTO> uploadPosSignatureReport(@RequestParam(name = "year") int year) {
        log.info("POS_SIGNATURE_UPLOAD API METHOD ACCESSED.");
        return posSignatureReportService.uploadPosSignatureReport(year);
    }

    @PostMapping("/main-data-report")
    public ResponseEntity<CommonResponseDTO> uploadMainDataExcel() {
        log.info("MAIN_DATA_REPORT_UPLOAD API METHOD ACCESSED.");
        return mainDataReportUploadService.uploadMainDataReports();
    }

    @PostMapping("/main-data-alh-report")
    public ResponseEntity<CommonResponseDTO> uploadMainDataALHReportExcel() {
        log.info("UploadMainDataALLReportExcel API METHOD ACCESSED.");
        return mainDataReportUploadService.uploadMainDataALHReportExcel();
    }
}
