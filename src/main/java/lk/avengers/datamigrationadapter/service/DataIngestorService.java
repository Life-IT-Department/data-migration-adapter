package lk.avengers.datamigrationadapter.service;

import org.springframework.web.multipart.MultipartFile;

public interface DataIngestorService {

    void ProcessPolicyListData(String uuid);

    void ProcessACPData(String uuid);

    void ProcessContactDetailData(String uuid);
}
