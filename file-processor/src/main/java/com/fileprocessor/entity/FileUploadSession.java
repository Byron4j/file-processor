package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件上传会话实体（用于分片上传）
 */
@TableName("file_upload_session")
public class FileUploadSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 上传会话唯一标识
     */
    private String uploadId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件总大小（字节）
     */
    private Long fileSize;

    /**
     * 文件SHA-256哈希（用于秒传）
     */
    private String fileHash;

    /**
     * 分片大小（字节），默认5MB
     */
    private Integer chunkSize = 5 * 1024 * 1024;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传的分片索引列表
     */
    private String uploadedChunks;

    /**
     * 状态: PENDING/UPLOADING/COMPLETED/EXPIRED
     */
    private String status;

    /**
     * 最终存储路径
     */
    private String storagePath;

    /**
     * 关联的文件ID（完成后填充）
     */
    private String targetFileId;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 业务方法

    /**
     * 检查是否所有分片都已上传
     */
    public boolean isAllChunksUploaded() {
        List<Integer> chunks = getUploadedChunkList();
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        return chunks.size() >= totalChunks;
    }

    /**
     * 添加已上传分片
     */
    public void addUploadedChunk(int chunkNumber) {
        List<Integer> chunks = getUploadedChunkList();
        if (!chunks.contains(chunkNumber)) {
            chunks.add(chunkNumber);
            setUploadedChunkList(chunks);
        }
    }

    /**
     * 检查指定分片是否已上传
     */
    public boolean isChunkUploaded(int chunkNumber) {
        List<Integer> chunks = getUploadedChunkList();
        return chunks.contains(chunkNumber);
    }

    /**
     * 获取上传进度百分比
     */
    public int getProgress() {
        if (totalChunks == null || totalChunks == 0) {
            return 0;
        }
        List<Integer> chunks = getUploadedChunkList();
        int uploaded = chunks.size();
        return (int) ((uploaded * 100.0) / totalChunks);
    }

    /**
     * 解析已上传分片列表
     */
    public List<Integer> getUploadedChunkList() {
        if (uploadedChunks == null || uploadedChunks.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(uploadedChunks,
                new com.fasterxml.jackson.core.type.TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 设置已上传分片列表
     */
    public void setUploadedChunkList(List<Integer> chunks) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.uploadedChunks = mapper.writeValueAsString(chunks);
        } catch (Exception e) {
            this.uploadedChunks = "[]";
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(String uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getTargetFileId() {
        return targetFileId;
    }

    public void setTargetFileId(String targetFileId) {
        this.targetFileId = targetFileId;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
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
