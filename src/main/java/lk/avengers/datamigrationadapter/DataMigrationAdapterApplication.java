package lk.avengers.datamigrationadapter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@EnableJpaAuditing
@SpringBootApplication
public class DataMigrationAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataMigrationAdapterApplication.class, args);
    }

}
