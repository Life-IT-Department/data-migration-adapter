package lk.avengers.datamigrationadapter.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PoiConfiguration {

    @EventListener(ApplicationReadyEvent.class)
    public void configurePoiLimits() {
        // Increase the byte array limit to 500MB for large files
        // This prevents POI from throwing exceptions when processing large Excel files
        int limitInBytes = 500_000_000; // 500MB
        IOUtils.setByteArrayMaxOverride(limitInBytes);

        log.info("Apache POI configured with byte array max override: {} MB", limitInBytes / (1024 * 1024));

        // Disable excessive POI logging to reduce overhead
        System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger");

        // Log current JVM memory settings
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);

        log.info("JVM Memory - Max: {} MB, Initial: {} MB", maxMemory, totalMemory);

        if (maxMemory < 4096) {
            log.warn("⚠️ WARNING: Max heap size is {} MB. Recommended minimum is 4096 MB for large Excel processing.", maxMemory);
            log.warn("⚠️ Please increase heap size with JVM option: -Xmx6g");
        }
    }
}