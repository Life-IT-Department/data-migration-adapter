package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.service.CashFlowReportUploadService;
import lk.avengers.datamigrationadapter.service.ContactDetailReportService;
import lk.avengers.datamigrationadapter.service.MainDataReportUploadService;
import lk.avengers.datamigrationadapter.service.PosSignatureReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/cash-flow-report")
    public ResponseEntity<CommonResponseDTO> uploadCashFlow() {
        log.info("CASH_FLOW_REPORT_UPLOAD API METHOD ACCESSED.");
        return cashFlowReportUploadService.uploadCashFlowReport();
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

    @GetMapping("/main-data-report")
    public ResponseEntity<CommonResponseDTO> uploadMainDataExcel() {
        log.info("MAIN_DATA_REPORT_UPLOAD API METHOD ACCESSED.");
        return mainDataReportUploadService.uploadMainDataReports();
    }

    @GetMapping("/main-data-report-2")
    public ResponseEntity<CommonResponseDTO> uploadMainDataReport2Excel() {
        log.info("MAIN_DATA_REPORT_2_UPLOAD API METHOD ACCESSED.");
        return mainDataReportUploadService.uploadMainDataReport2();
    }
}
