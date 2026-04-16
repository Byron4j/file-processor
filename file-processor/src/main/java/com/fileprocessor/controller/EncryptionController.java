package com.fileprocessor.controller;

import com.fileprocessor.annotation.AuditLog;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.security.UserPrincipal;
import com.fileprocessor.service.FileEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security")
public class EncryptionController {

    private static final Logger log = LoggerFactory.getLogger(EncryptionController.class);

    @Autowired
    private FileEncryptionService encryptionService;

    @PostMapping("/encrypt")
    @PreAuthorize("hasRole('USER')")
    @AuditLog(action = "FILE_ENCRYPT", resourceType = "FILE")
    public ResponseEntity<FileResponse> encryptFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EncryptRequest request) {
        log.info("Encrypt file request: {} by user: {}", request.getSourcePath(), principal.getId());

        FileResponse response = encryptionService.encryptFile(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/decrypt")
    @PreAuthorize("hasRole('USER')")
    @AuditLog(action = "FILE_DECRYPT", resourceType = "FILE")
    public ResponseEntity<FileResponse> decryptFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DecryptRequest request) {
        log.info("Decrypt file request: {} by user: {}", request.getSourcePath(), principal.getId());

        FileResponse response = encryptionService.decryptFile(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/generate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileResponse> generateDataKey() {
        log.info("Generate data key request");

        FileResponse response = encryptionService.generateDataKey();
        return ResponseEntity.ok(response);
    }

    // Request DTOs
    public static class EncryptRequest {
        private String sourcePath;
        private String targetPath;
        private String password;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class DecryptRequest {
        private String sourcePath;
        private String targetPath;
        private String password;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
