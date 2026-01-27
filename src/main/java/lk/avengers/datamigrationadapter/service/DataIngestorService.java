package lk.avengers.datamigrationadapter.service;

import org.springframework.web.multipart.MultipartFile;

public interface DataIngestorService {

    void ProcessACPData(String uuid, MultipartFile excelFile);
}
