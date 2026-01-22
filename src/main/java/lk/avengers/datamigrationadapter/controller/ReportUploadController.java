package lk.avengers.datamigrationadapter.controller;

import lk.avengers.datamigrationadapter.dto.CommonResponseDTO;
import lk.avengers.datamigrationadapter.service.CashFlowReportUploadService;
import lk.avengers.datamigrationadapter.service.MainDataReportUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("report-upload")
@RequiredArgsConstructor
public class ReportUploadController {

    private final MainDataReportUploadService mainDataReportUploadService;
    private final CashFlowReportUploadService cashFlowReportUploadService;

    @GetMapping("/cash-flow")
    public ResponseEntity<CommonResponseDTO> uploadCashFlow() {
        log.info("CASH_FLOW_REPORT_UPLOAD API METHOD ACCESSED.");
        return cashFlowReportUploadService.uploadCashFlowReport();
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
