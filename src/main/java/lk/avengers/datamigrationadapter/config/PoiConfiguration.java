package lk.avengers.datamigrationadapter.config;

import org.apache.poi.util.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PoiConfiguration {

    @EventListener(ApplicationReadyEvent.class)
    public void configurePoiLimits() {
        IOUtils.setByteArrayMaxOverride(200_000_000); // 200MB
    }
}