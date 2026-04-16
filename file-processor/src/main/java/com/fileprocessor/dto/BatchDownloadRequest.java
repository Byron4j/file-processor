package com.fileprocessor.dto;

import java.util.List;

/**
 * 批量下载请求
 */
public class BatchDownloadRequest {

    /**
     * 文件ID列表
     */
    private List<String> fileIds;

    /**
     * 压缩包名称
     */
    private String archiveName;

    /**
     * 压缩格式: ZIP, 7z
     */
    private String archiveFormat;

    /**
     * 压缩级别 0-9
     */
    private Integer compressionLevel;

    /**
     * 密码保护
     */
    private String password;

    // Getters and Setters

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getArchiveFormat() {
        return archiveFormat;
    }

    public void setArchiveFormat(String archiveFormat) {
        this.archiveFormat = archiveFormat;
    }

    public Integer getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(Integer compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
