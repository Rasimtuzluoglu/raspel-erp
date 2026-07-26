package com.raspel.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncService {

    private final BackupService backupService;

    @Async
    public CompletableFuture<String> generateReport(String reportName) {
        log.info("Async report generation started: {}", reportName);
        try {
            Thread.sleep(2000);
            log.info("Async report generation completed: {}", reportName);
            return CompletableFuture.completedFuture("Report '" + reportName + "' generated successfully");
        } catch (Exception e) {
            log.error("Async report generation failed: {}", reportName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<String> backupAndNotify() {
        log.info("Async backup started");
        try {
            String filename = backupService.manualBackup("DAILY");
            backupService.cleanAllOldBackups();
            log.info("Async backup completed: {}", filename);
            return CompletableFuture.completedFuture("Backup '" + filename + "' completed");
        } catch (Exception e) {
            log.error("Async backup failed", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
