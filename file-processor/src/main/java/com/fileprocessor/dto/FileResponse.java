package com.fileprocessor.dto;

public class FileResponse {

    private boolean success;
    private String message;
    private String filePath;
    private Long fileSize;

    public FileResponse() {
    }

    public FileResponse(boolean success, String message, String filePath, Long fileSize) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "FileResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }

    public static class Builder {
        private boolean success;
        private String message;
        private String filePath;
        private Long fileSize;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public FileResponse build() {
            return new FileResponse(success, message, filePath, fileSize);
        }
    }
}
