package com.fileprocessor.dto;

public class FileResponse {

    private boolean success;
    private String message;
    private String fileId;
    private String filePath;
    private Long fileSize;
    private Object data;

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

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "FileResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", fileId='" + fileId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }

    public static class Builder {
        private boolean success;
        private String message;
        private String fileId;
        private String filePath;
        private Long fileSize;
        private Object data;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder fileId(String fileId) {
            this.fileId = fileId;
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

        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        public FileResponse build() {
            FileResponse response = new FileResponse(success, message, filePath, fileSize);
            response.setFileId(fileId);
            response.setData(data);
            return response;
        }
    }
}
