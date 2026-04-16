package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 文件分割记录实体
 */
@TableName("file_split_record")
public class FileSplitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String splitId;
    private String originalFileId;
    private String originalFileName;
    private Long originalSize;
    private String originalHash;
    private Long chunkSize;
    private Integer totalChunks;
    private String outputDir;
    private String manifestPath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSplitId() { return splitId; }
    public void setSplitId(String splitId) { this.splitId = splitId; }
    public String getOriginalFileId() { return originalFileId; }
    public void setOriginalFileId(String originalFileId) { this.originalFileId = originalFileId; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public Long getOriginalSize() { return originalSize; }
    public void setOriginalSize(Long originalSize) { this.originalSize = originalSize; }
    public String getOriginalHash() { return originalHash; }
    public void setOriginalHash(String originalHash) { this.originalHash = originalHash; }
    public Long getChunkSize() { return chunkSize; }
    public void setChunkSize(Long chunkSize) { this.chunkSize = chunkSize; }
    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    public String getManifestPath() { return manifestPath; }
    public void setManifestPath(String manifestPath) { this.manifestPath = manifestPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
