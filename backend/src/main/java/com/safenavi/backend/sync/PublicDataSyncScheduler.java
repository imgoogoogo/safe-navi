package com.safenavi.backend.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.safenavi.backend.common.exception.BusinessException;

@Component
public class PublicDataSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublicDataSyncScheduler.class);

    private final PublicDataSyncService syncService;

    public PublicDataSyncScheduler(PublicDataSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(cron = "${public-data.cron}", zone = "Asia/Seoul")
    public void run() {
        try {
            SyncResult result = syncService.syncAll();
            log.info("Scheduled public data sync finished: {}", result.sources());
        } catch (BusinessException e) {
            log.warn("Scheduled public data sync skipped: {}", e.getMessage());
        }
    }
}
