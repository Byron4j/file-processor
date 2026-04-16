package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件版本实体
 */
@TableName("file_version")
public class FileVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版本唯一标识
     */
    private String versionId;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 版本存储路径
     */
    private String storagePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件哈希
     */
    private String fileHash;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 标签
     */
    private String tags;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // 辅助方法

    public List<String> getTagList() {
        if (tags == null || tags.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return java.util.Arrays.asList(tags.split(","));
    }

    public void setTagList(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            this.tags = "";
        } else {
            this.tags = String.join(",", tagList);
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
