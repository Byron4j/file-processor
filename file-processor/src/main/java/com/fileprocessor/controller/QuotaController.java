package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户配额管理控制器
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private static final Logger log = LoggerFactory.getLogger(QuotaController.class);

    @Autowired
    private QuotaService quotaService;

    /**
     * 获取当前用户配额信息
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getQuotaInfo(
            @RequestParam(value = "userId", required = false) Long userId) {
        // TODO: 从SecurityContext获取实际用户ID
        Long currentUserId = userId != null ? userId : 1L;
        log.info("Getting quota info for user: {}", currentUserId);

        FileResponse response = quotaService.getQuotaInfo(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新用户配额（管理员接口）
     */
    @PostMapping("/update/{userId}")
    public ResponseEntity<FileResponse> updateQuota(
            @PathVariable Long userId,
            @RequestBody QuotaUpdateRequest request) {
        log.info("Updating quota for user: {}", userId);

        FileResponse response = quotaService.updateQuota(
                userId,
                request.getTotalStorageQuota(),
                request.getDailyUploadLimit(),
                request.getMaxFileCount()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 检查上传配额
     */
    @PostMapping("/check")
    public ResponseEntity<FileResponse> checkQuota(
            @RequestBody QuotaCheckRequest request) {
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        log.info("Checking quota for user: {}, size: {}", userId, request.getFileSize());

        QuotaService.QuotaCheckResult result = quotaService.checkUploadQuota(userId, request.getFileSize());

        if (result.isAllowed()) {
            return ResponseEntity.ok(FileResponse.builder()
                    .success(true)
                    .message("Quota sufficient")
                    .data(Map.of(
                            "allowed", true,
                            "remainingStorage", result.getQuota().getRemainingStorage(),
                            "remainingDailyUpload", result.getQuota().getRemainingDailyUpload(),
                            "remainingFileCount", result.getQuota().getRemainingFileCount()
                    ))
                    .build());
        } else {
            return ResponseEntity.ok(FileResponse.builder()
                    .success(false)
                    .message(result.getMessage())
                    .data(Map.of("allowed", false))
                    .build());
        }
    }

    /**
     * 初始化默认配额（用于新用户注册）
     */
    @PostMapping("/init/{userId}")
    public ResponseEntity<FileResponse> initQuota(@PathVariable Long userId) {
        log.info("Initializing quota for user: {}", userId);

        // 通过getOrCreateQuota创建默认配额
        quotaService.getOrCreateQuota(userId);

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Quota initialized successfully")
                .build());
    }

    // ==================== Request DTOs ====================

    public static class QuotaUpdateRequest {
        private Long totalStorageQuota;
        private Long dailyUploadLimit;
        private Integer maxFileCount;

        public Long getTotalStorageQuota() {
            return totalStorageQuota;
        }

        public void setTotalStorageQuota(Long totalStorageQuota) {
            this.totalStorageQuota = totalStorageQuota;
        }

        public Long getDailyUploadLimit() {
            return dailyUploadLimit;
        }

        public void setDailyUploadLimit(Long dailyUploadLimit) {
            this.dailyUploadLimit = dailyUploadLimit;
        }

        public Integer getMaxFileCount() {
            return maxFileCount;
        }

        public void setMaxFileCount(Integer maxFileCount) {
            this.maxFileCount = maxFileCount;
        }
    }

    public static class QuotaCheckRequest {
        private Long userId;
        private Long fileSize;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
}
