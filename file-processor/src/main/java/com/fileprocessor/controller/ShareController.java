package com.fileprocessor.controller;

import com.fileprocessor.annotation.RateLimit;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.security.UserPrincipal;
import com.fileprocessor.service.FileShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private FileShareService fileShareService;

    @PostMapping("/create")
    @RateLimit(limit = 10, window = 60)
    public ResponseEntity<FileResponse> createShare(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ShareCreateRequest request) {
        log.info("Create share request for file: {} by user: {}", request.getFileId(), principal.getId());

        FileResponse response = fileShareService.createShare(
                request.getFileId(),
                principal.getId(),
                request.getPassword(),
                request.getExpireHours(),
                request.getMaxDownloads(),
                request.getAllowPreview()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shareId}/validate")
    public ResponseEntity<FileResponse> validateShare(
            @PathVariable String shareId,
            @RequestBody ValidateRequest request) {
        log.info("Validate share: {}", shareId);

        FileResponse response = fileShareService.validateShare(
                shareId,
                request.getToken(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}")
    public ResponseEntity<FileResponse> getShareInfo(
            @PathVariable String shareId,
            @RequestParam String token) {
        log.info("Get share info: {}", shareId);

        FileResponse response = fileShareService.validateShare(shareId, token, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shareId}/revoke")
    public ResponseEntity<FileResponse> revokeShare(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String shareId) {
        log.info("Revoke share: {} by user: {}", shareId, principal.getId());

        FileResponse response = fileShareService.revokeShare(shareId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shareId}/download")
    @RateLimit(limit = 5, window = 60)
    public ResponseEntity<FileResponse> downloadSharedFile(
            @PathVariable String shareId,
            @RequestParam String token,
            @RequestParam(required = false) String password) {
        log.info("Download shared file: {}", shareId);

        // Validate share
        FileResponse validation = fileShareService.validateShare(shareId, token, password);
        if (!validation.isSuccess()) {
            return ResponseEntity.badRequest().body(validation);
        }

        // Record download
        fileShareService.recordDownload(shareId);

        // Return file info for client to download
        Map<String, Object> data = (Map<String, Object>) validation.getData();
        String fileId = (String) data.get("fileId");

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Share validated, ready for download")
                .data(Map.of("fileId", fileId))
                .build());
    }

    // Request DTOs
    public static class ShareCreateRequest {
        private String fileId;
        private String password;
        private Integer expireHours;
        private Integer maxDownloads;
        private Boolean allowPreview;

        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Integer getExpireHours() { return expireHours; }
        public void setExpireHours(Integer expireHours) { this.expireHours = expireHours; }
        public Integer getMaxDownloads() { return maxDownloads; }
        public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }
        public Boolean getAllowPreview() { return allowPreview; }
        public void setAllowPreview(Boolean allowPreview) { this.allowPreview = allowPreview; }
    }

    public static class ValidateRequest {
        private String token;
        private String password;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
