package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.UserQuota;
import com.fileprocessor.mapper.UserQuotaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户配额服务
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    @Autowired
    private UserQuotaMapper userQuotaMapper;

    @Value("${quota.default-storage:10737418240}") // 10GB default
    private Long defaultStorageQuota;

    @Value("${quota.default-daily-upload:1073741824}") // 1GB default
    private Long defaultDailyUploadLimit;

    @Value("${quota.default-max-files:10000}")
    private Integer defaultMaxFileCount;

    /**
     * 获取或创建用户配额
     */
    public UserQuota getOrCreateQuota(Long userId) {
        UserQuota quota = userQuotaMapper.selectByUserId(userId);
        if (quota == null) {
            quota = createDefaultQuota(userId);
        }
        // 检查是否需要重置每日上传限制
        if (quota.needsDailyReset()) {
            resetDailyQuota(userId);
            quota = userQuotaMapper.selectByUserId(userId);
        }
        return quota;
    }

    /**
     * 检查上传配额
     */
    public QuotaCheckResult checkUploadQuota(Long userId, long fileSize) {
        UserQuota quota = getOrCreateQuota(userId);

        // 检查存储空间
        long remainingStorage = quota.getRemainingStorage();
        if (remainingStorage < fileSize) {
            return QuotaCheckResult.failed(
                String.format("存储空间不足。剩余: %s, 需要: %s",
                    formatSize(remainingStorage), formatSize(fileSize))
            );
        }

        // 检查每日上传限制
        long remainingDaily = quota.getRemainingDailyUpload();
        if (remainingDaily < fileSize) {
            return QuotaCheckResult.failed(
                String.format("今日上传额度不足。剩余: %s, 需要: %s",
                    formatSize(remainingDaily), formatSize(fileSize))
            );
        }

        // 检查文件数量限制
        int remainingFiles = quota.getRemainingFileCount();
        if (remainingFiles < 1) {
            return QuotaCheckResult.failed("文件数量已达上限: " + quota.getMaxFileCount());
        }

        return QuotaCheckResult.success(quota);
    }

    /**
     * 检查批量上传配额
     */
    public QuotaCheckResult checkBatchUploadQuota(Long userId, long totalSize, int fileCount) {
        UserQuota quota = getOrCreateQuota(userId);

        // 检查存储空间
        long remainingStorage = quota.getRemainingStorage();
        if (remainingStorage < totalSize) {
            return QuotaCheckResult.failed(
                String.format("存储空间不足。剩余: %s, 需要: %s",
                    formatSize(remainingStorage), formatSize(totalSize))
            );
        }

        // 检查每日上传限制
        long remainingDaily = quota.getRemainingDailyUpload();
        if (remainingDaily < totalSize) {
            return QuotaCheckResult.failed(
                String.format("今日上传额度不足。剩余: %s, 需要: %s",
                    formatSize(remainingDaily), formatSize(totalSize))
            );
        }

        // 检查文件数量限制
        int remainingFiles = quota.getRemainingFileCount();
        if (remainingFiles < fileCount) {
            return QuotaCheckResult.failed(
                String.format("文件数量不足。剩余配额: %d, 需要: %d", remainingFiles, fileCount)
            );
        }

        return QuotaCheckResult.success(quota);
    }

    /**
     * 记录上传完成（更新配额使用）
     */
    public void recordUploadComplete(Long userId, long fileSize) {
        try {
            userQuotaMapper.increaseUsedStorage(userId, fileSize);
            userQuotaMapper.increaseDailyUpload(userId, fileSize);
            userQuotaMapper.increaseFileCount(userId);
            log.debug("Updated quota for user {}: +{} bytes", userId, fileSize);
        } catch (Exception e) {
            log.error("Failed to update quota for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 记录文件删除（释放配额）
     */
    public void recordFileDeleted(Long userId, long fileSize) {
        try {
            userQuotaMapper.decreaseUsedStorage(userId, fileSize);
            userQuotaMapper.decreaseFileCount(userId);
            log.debug("Released quota for user {}: -{} bytes", userId, fileSize);
        } catch (Exception e) {
            log.error("Failed to release quota for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 获取配额信息响应
     */
    public FileResponse getQuotaInfo(Long userId) {
        UserQuota quota = getOrCreateQuota(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("totalStorage", quota.getTotalStorageQuota());
        data.put("usedStorage", quota.getUsedStorageQuota());
        data.put("remainingStorage", quota.getRemainingStorage());
        data.put("storageUsagePercent", calculatePercent(quota.getUsedStorageQuota(), quota.getTotalStorageQuota()));

        data.put("dailyUploadLimit", quota.getDailyUploadLimit());
        data.put("dailyUploadUsed", quota.getDailyUploadUsed());
        data.put("remainingDailyUpload", quota.getRemainingDailyUpload());
        data.put("dailyUploadPercent", calculatePercent(quota.getDailyUploadUsed(), quota.getDailyUploadLimit()));

        data.put("maxFileCount", quota.getMaxFileCount());
        data.put("currentFileCount", quota.getCurrentFileCount());
        data.put("remainingFileCount", quota.getRemainingFileCount());
        data.put("fileCountPercent", calculatePercent(quota.getCurrentFileCount(), quota.getMaxFileCount()));

        data.put("dailyResetAt", quota.getDailyResetAt());

        return FileResponse.builder()
                .success(true)
                .message("Quota information retrieved")
                .data(data)
                .build();
    }

    /**
     * 更新用户配额
     */
    public FileResponse updateQuota(Long userId, Long storageQuota, Long dailyUploadLimit, Integer maxFileCount) {
        UserQuota quota = userQuotaMapper.selectByUserId(userId);
        if (quota == null) {
            quota = new UserQuota();
            quota.setUserId(userId);
            quota.setUsedStorageQuota(0L);
            quota.setDailyUploadUsed(0L);
            quota.setCurrentFileCount(0);
            quota.setDailyResetAt(LocalDateTime.now());
        }

        if (storageQuota != null) {
            quota.setTotalStorageQuota(storageQuota);
        }
        if (dailyUploadLimit != null) {
            quota.setDailyUploadLimit(dailyUploadLimit);
        }
        if (maxFileCount != null) {
            quota.setMaxFileCount(maxFileCount);
        }
        quota.setUpdatedAt(LocalDateTime.now());

        if (quota.getId() == null) {
            userQuotaMapper.insert(quota);
        } else {
            userQuotaMapper.updateById(quota);
        }

        return FileResponse.builder()
                .success(true)
                .message("Quota updated successfully")
                .build();
    }

    /**
     * 重置每日配额
     */
    public void resetDailyQuota(Long userId) {
        userQuotaMapper.resetDailyUpload(userId, LocalDateTime.now());
        log.info("Reset daily quota for user {}", userId);
    }

    /**
     * 创建默认配额
     */
    private UserQuota createDefaultQuota(Long userId) {
        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setTotalStorageQuota(defaultStorageQuota);
        quota.setUsedStorageQuota(0L);
        quota.setDailyUploadLimit(defaultDailyUploadLimit);
        quota.setDailyUploadUsed(0L);
        quota.setDailyResetAt(LocalDateTime.now());
        quota.setMaxFileCount(defaultMaxFileCount);
        quota.setCurrentFileCount(0);
        quota.setCreatedAt(LocalDateTime.now());
        quota.setUpdatedAt(LocalDateTime.now());

        userQuotaMapper.insert(quota);
        log.info("Created default quota for user {}: storage={}, daily={}",
                userId, defaultStorageQuota, defaultDailyUploadLimit);

        return quota;
    }

    /**
     * 计算百分比
     */
    private double calculatePercent(Number used, Number total) {
        if (used == null || total == null || total.longValue() == 0) {
            return 0.0;
        }
        return Math.round((used.doubleValue() / total.doubleValue()) * 10000.0) / 100.0;
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 配额检查结果
     */
    public static class QuotaCheckResult {
        private final boolean allowed;
        private final String message;
        private final UserQuota quota;

        private QuotaCheckResult(boolean allowed, String message, UserQuota quota) {
            this.allowed = allowed;
            this.message = message;
            this.quota = quota;
        }

        public static QuotaCheckResult success(UserQuota quota) {
            return new QuotaCheckResult(true, null, quota);
        }

        public static QuotaCheckResult failed(String message) {
            return new QuotaCheckResult(false, message, null);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getMessage() {
            return message;
        }

        public UserQuota getQuota() {
            return quota;
        }
    }
}
