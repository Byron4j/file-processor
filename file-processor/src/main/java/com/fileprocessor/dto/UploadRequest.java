package com.fileprocessor.dto;

import java.util.List;

/**
 * 文件上传请求
 */
public class UploadRequest {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 过期时间（天），null表示永不过期
     */
    private Integer expireDays;

    /**
     * 用户ID（用于配额检查）
     */
    private Long userId;

    // Getters and Setters

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getExpireDays() {
        return expireDays;
    }

    public void setExpireDays(Integer expireDays) {
        this.expireDays = expireDays;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
