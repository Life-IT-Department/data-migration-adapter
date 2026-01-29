package lk.avengers.datamigrationadapter.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PoiConfiguration {

//    @EventListener(ApplicationReadyEvent.class)
//    public void configurePoiLimits() {
//        // Configure ZIP security settings for large Excel files
//        ZipSecureFile.setMinInflateRatio(0.001); // Allow higher compression ratios
//        ZipSecureFile.setMaxEntrySize(200_000_000L); // 200MB max entry size (increased from 100MB)
//        ZipSecureFile.setMaxTextSize(100_000_000L); // 100MB max text size (increased from 50MB)
//
//        // With streaming, we don't need extreme limits but keep it reasonable
//        IOUtils.setByteArrayMaxOverride(200_000_000); // 200MB (increased from 100MB)
//
//
//        // With streaming, we don't need extreme limits but keep it reasonable
//        IOUtils.setByteArrayMaxOverride(300_000_000); // 300MB
//
//        Runtime runtime = Runtime.getRuntime();
//        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
//        long freeMemoryMB = runtime.freeMemory() / (1024 * 1024);
//
//        log.info("==========================================================");
//        log.info("POI Configuration:");
//        log.info("  - ByteArrayMaxOverride: 100 MB");
//        log.info("  - MinInflateRatio: 0.001 (allow high compression)");
//        log.info("  - MaxEntrySize: 100 MB");
//        log.info("  - MaxTextSize: 50 MB");
//        log.info("  - JVM Max Memory: {} MB", maxMemoryMB);
//        log.info("  - JVM Free Memory: {} MB", freeMemoryMB);
//        log.info("  - Streaming mode: ENABLED");
//        log.info("==========================================================");
//
//        if (maxMemoryMB < 2048) {
//            log.warn("⚠️ WARNING: JVM max memory is only {} MB. Recommend at least 2048 MB (-Xmx2g)", maxMemoryMB);
//            log.warn("⚠️ Set JVM options: -Xmx3g -Xms1g");
//        }
//
//    }

    @PostConstruct
    public void configurePoiLimits() {
        // Configure ZIP security settings for large Excel files
        // Your file needs ~200MB, so set to 300MB for safety
        ZipSecureFile.setMinInflateRatio(0.0001); // Even more relaxed ratio
        ZipSecureFile.setMaxEntrySize(300_000_000L); // 300MB max entry size
        ZipSecureFile.setMaxTextSize(200_000_000L); // 200MB max text size

        // With streaming, we don't need extreme limits but keep it reasonable
        IOUtils.setByteArrayMaxOverride(300_000_000); // 300MB

        Runtime runtime = Runtime.getRuntime();
        long maxMemoryMB = runtime.maxMemory() / (1024 * 1024);
        long freeMemoryMB = runtime.freeMemory() / (1024 * 1024);

        log.info("==========================================================");
        log.info("POI Configuration Applied:");
        log.info("  - ByteArrayMaxOverride: 300 MB");
        log.info("  - MinInflateRatio: 0.0001 (very relaxed for large files)");
        log.info("  - MaxEntrySize: 300 MB");
        log.info("  - MaxTextSize: 200 MB");
        log.info("  - JVM Max Memory: {} MB", maxMemoryMB);
        log.info("  - JVM Free Memory: {} MB", freeMemoryMB);
        log.info("  - Streaming mode: ENABLED");
        log.info("==========================================================");

        if (maxMemoryMB < 2048) {
            log.warn("⚠️ WARNING: JVM max memory is only {} MB. Recommend at least 2048 MB (-Xmx2g)", maxMemoryMB);
            log.warn("⚠️ Set JVM options: -Xmx3g -Xms1g");
        }
    }

}