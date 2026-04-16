package com.fileprocessor.entity;

import java.time.LocalDateTime;

/**
 * 用户存储配额实体
 */
public class UserQuota {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 总存储配额（字节）
     */
    private Long totalStorageQuota;

    /**
     * 已使用存储空间（字节）
     */
    private Long usedStorageQuota;

    /**
     * 每日上传限制（字节）
     */
    private Long dailyUploadLimit;

    /**
     * 今日已上传（字节）
     */
    private Long dailyUploadUsed;

    /**
     * 每日上传限制最后重置时间
     */
    private LocalDateTime dailyResetAt;

    /**
     * 最大文件数限制
     */
    private Integer maxFileCount;

    /**
     * 当前文件数
     */
    private Integer currentFileCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTotalStorageQuota() {
        return totalStorageQuota;
    }

    public void setTotalStorageQuota(Long totalStorageQuota) {
        this.totalStorageQuota = totalStorageQuota;
    }

    public Long getUsedStorageQuota() {
        return usedStorageQuota;
    }

    public void setUsedStorageQuota(Long usedStorageQuota) {
        this.usedStorageQuota = usedStorageQuota;
    }

    public Long getDailyUploadLimit() {
        return dailyUploadLimit;
    }

    public void setDailyUploadLimit(Long dailyUploadLimit) {
        this.dailyUploadLimit = dailyUploadLimit;
    }

    public Long getDailyUploadUsed() {
        return dailyUploadUsed;
    }

    public void setDailyUploadUsed(Long dailyUploadUsed) {
        this.dailyUploadUsed = dailyUploadUsed;
    }

    public LocalDateTime getDailyResetAt() {
        return dailyResetAt;
    }

    public void setDailyResetAt(LocalDateTime dailyResetAt) {
        this.dailyResetAt = dailyResetAt;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public void setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    public Integer getCurrentFileCount() {
        return currentFileCount;
    }

    public void setCurrentFileCount(Integer currentFileCount) {
        this.currentFileCount = currentFileCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取剩余存储空间
     */
    public long getRemainingStorage() {
        if (totalStorageQuota == null || usedStorageQuota == null) {
            return 0;
        }
        return Math.max(0, totalStorageQuota - usedStorageQuota);
    }

    /**
     * 获取今日剩余上传额度
     */
    public long getRemainingDailyUpload() {
        if (dailyUploadLimit == null || dailyUploadUsed == null) {
            return 0;
        }
        return Math.max(0, dailyUploadLimit - dailyUploadUsed);
    }

    /**
     * 检查是否需要重置每日上传限制
     */
    public boolean needsDailyReset() {
        if (dailyResetAt == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return dailyResetAt.toLocalDate().isBefore(now.toLocalDate());
    }

    /**
     * 获取剩余可上传文件数
     */
    public int getRemainingFileCount() {
        if (maxFileCount == null || currentFileCount == null) {
            return 0;
        }
        return Math.max(0, maxFileCount - currentFileCount);
    }
}
