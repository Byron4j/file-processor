package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 文件分享实体
 */
@TableName("file_share")
public class FileShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分享唯一标识
     */
    private String shareId;

    /**
     * 分享的文件ID
     */
    private String fileId;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 分享令牌
     */
    private String shareToken;

    /**
     * 访问密码哈希
     */
    private String passwordHash;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 最大下载次数(0=无限制)
     */
    private Integer maxDownloads;

    /**
     * 已下载次数
     */
    private Integer downloadCount;

    /**
     * 是否允许预览
     */
    private Boolean allowPreview;

    /**
     * 状态: 0-失效, 1-有效
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 业务方法

    /**
     * 检查分享是否有效
     */
    public boolean isValid() {
        // 检查状态
        if (status == null || status != 1) {
            return false;
        }

        // 检查是否过期
        if (expireAt != null && expireAt.isBefore(LocalDateTime.now())) {
            return false;
        }

        // 检查下载次数
        if (maxDownloads != null && maxDownloads > 0) {
            if (downloadCount != null && downloadCount >= maxDownloads) {
                return false;
            }
        }

        return true;
    }

    /**
     * 是否需要密码
     */
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    /**
     * 增加下载计数
     */
    public void incrementDownloadCount() {
        if (downloadCount == null) {
            downloadCount = 0;
        }
        downloadCount++;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Boolean getAllowPreview() {
        return allowPreview;
    }

    public void setAllowPreview(Boolean allowPreview) {
        this.allowPreview = allowPreview;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
}
