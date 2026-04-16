package com.fileprocessor.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分片上传初始化响应
 */
public class ChunkUploadResponse {

    /**
     * 上传会话ID
     */
    private String uploadId;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 已上传的分片索引列表
     */
    private List<Integer> uploadedChunks;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 是否已完成秒传
     */
    private Boolean fastUpload;

    /**
     * 秒传成功的文件ID
     */
    private String fileId;

    /**
     * 是否成功
     */
    private Boolean success = true;

    /**
     * 消息
     */
    private String message;

    // 静态 Builder 类
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uploadId;
        private Integer totalChunks;
        private Integer chunkSize;
        private List<Integer> uploadedChunks;
        private LocalDateTime expireAt;
        private Boolean fastUpload;
        private String fileId;
        private Boolean success = true;
        private String message;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder totalChunks(Integer totalChunks) {
            this.totalChunks = totalChunks;
            return this;
        }

        public Builder chunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder uploadedChunks(List<Integer> uploadedChunks) {
            this.uploadedChunks = uploadedChunks;
            return this;
        }

        public Builder expireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Builder fastUpload(Boolean fastUpload) {
            this.fastUpload = fastUpload;
            return this;
        }

        public Builder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public ChunkUploadResponse build() {
            ChunkUploadResponse response = new ChunkUploadResponse();
            response.uploadId = this.uploadId;
            response.totalChunks = this.totalChunks;
            response.chunkSize = this.chunkSize;
            response.uploadedChunks = this.uploadedChunks;
            response.expireAt = this.expireAt;
            response.fastUpload = this.fastUpload;
            response.fileId = this.fileId;
            response.success = this.success;
            response.message = this.message;
            return response;
        }
    }

    // Getters and Setters

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public Boolean getFastUpload() {
        return fastUpload;
    }

    public void setFastUpload(Boolean fastUpload) {
        this.fastUpload = fastUpload;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
