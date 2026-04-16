package com.fileprocessor.controller;

import com.fileprocessor.annotation.AuditLog;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.security.UserPrincipal;
import com.fileprocessor.service.VirusScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security/virus-scan")
@PreAuthorize("hasRole('ADMIN')")
public class VirusScanController {

    private static final Logger log = LoggerFactory.getLogger(VirusScanController.class);

    @Autowired
    private VirusScanService virusScanService;

    @PostMapping("/file")
    @AuditLog(action = "VIRUS_SCAN", resourceType = "FILE")
    public ResponseEntity<FileResponse> scanFile(@RequestBody ScanRequest request) {
        log.info("Virus scan request for file: {}", request.getFilePath());

        FileResponse response = virusScanService.scanFile(request.getFilePath());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    @AuditLog(action = "VIRUS_SCAN_BATCH", resourceType = "FILE")
    public ResponseEntity<FileResponse> scanFiles(@RequestBody BatchScanRequest request) {
        log.info("Batch virus scan request for {} files", request.getFilePaths().size());

        FileResponse response = virusScanService.scanFiles(request.getFilePaths());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<FileResponse> getStatus() {
        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Virus scan service status")
                .data(java.util.Map.of("enabled", virusScanService.isEnabled()))
                .build());
    }

    // Request DTOs
    public static class ScanRequest {
        private String filePath;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }

    public static class BatchScanRequest {
        private List<String> filePaths;

        public List<String> getFilePaths() { return filePaths; }
        public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }
    }
}
