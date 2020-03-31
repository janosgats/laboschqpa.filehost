package com.laboschqpa.filehost.config;

import com.laboschqpa.filehost.service.GlobalStreamTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ContextStaredEventListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ContextStaredEventListener.class);

    private static volatile boolean wasGlobalStreamTrackerServiceStarted = false;
    private static final Object globalStreamTrackerServiceStartingLock = new Object();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextStartedEvent) {
        synchronized (globalStreamTrackerServiceStartingLock) {
            if (!wasGlobalStreamTrackerServiceStarted) {
                logger.info("Starting GlobalStreamTrackerService.");
                GlobalStreamTrackerService globalStreamTrackerService = contextStartedEvent.getApplicationContext().getBean(GlobalStreamTrackerService.class);
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(globalStreamTrackerService, 0, 10, TimeUnit.SECONDS);
                wasGlobalStreamTrackerServiceStarted = true;
            }
        }
    }
}