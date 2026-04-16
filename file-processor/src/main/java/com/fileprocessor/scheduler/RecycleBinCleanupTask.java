package com.fileprocessor.scheduler;

import com.fileprocessor.service.RecycleBinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecycleBinCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinCleanupTask.class);

    @Autowired
    private RecycleBinService recycleBinService;

    /**
     * 每天凌晨2点清理回收站
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredFiles() {
        log.info("Starting recycle bin cleanup task");
        recycleBinService.cleanupExpiredFiles();
        log.info("Recycle bin cleanup task completed");
    }
}
