package lk.avengers.datamigrationadapter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@Slf4j
public class SystemInfoController {

    @GetMapping("/jvm-info")
    public Map<String, Object> getJvmInfo() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> info = new HashMap<>();
        info.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        info.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        info.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        info.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        info.put("availableProcessors", runtime.availableProcessors());

        // JVM properties
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("jvmName", System.getProperty("java.vm.name"));

        log.info("JVM Info - Max Memory: {} MB, Used: {} MB",
                runtime.maxMemory() / (1024 * 1024),
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));

        return info;
    }
}