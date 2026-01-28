package lk.avengers.datamigrationadapter.service;

import org.springframework.web.multipart.MultipartFile;

public interface DataIngestorService {

    void ProcessPolicyListData(String uuid, MultipartFile excelFile);

    void ProcessACPData(String uuid, MultipartFile excelFile);
}
