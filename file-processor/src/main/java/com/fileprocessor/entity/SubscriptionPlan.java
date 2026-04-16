package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("subscription_plan")
public class SubscriptionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private Long storageQuota;
    private Integer maxFileSize;
    private Integer maxTasksPerDay;
    private Integer maxTeamMembers;
    private Boolean aiFeaturesEnabled;
    private Boolean advancedPdfEnabled;
    private Boolean prioritySupport;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public BigDecimal getYearlyPrice() { return yearlyPrice; }
    public void setYearlyPrice(BigDecimal yearlyPrice) { this.yearlyPrice = yearlyPrice; }
    public Long getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    public Integer getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(Integer maxFileSize) { this.maxFileSize = maxFileSize; }
    public Integer getMaxTasksPerDay() { return maxTasksPerDay; }
    public void setMaxTasksPerDay(Integer maxTasksPerDay) { this.maxTasksPerDay = maxTasksPerDay; }
    public Integer getMaxTeamMembers() { return maxTeamMembers; }
    public void setMaxTeamMembers(Integer maxTeamMembers) { this.maxTeamMembers = maxTeamMembers; }
    public Boolean getAiFeaturesEnabled() { return aiFeaturesEnabled; }
    public void setAiFeaturesEnabled(Boolean aiFeaturesEnabled) { this.aiFeaturesEnabled = aiFeaturesEnabled; }
    public Boolean getAdvancedPdfEnabled() { return advancedPdfEnabled; }
    public void setAdvancedPdfEnabled(Boolean advancedPdfEnabled) { this.advancedPdfEnabled = advancedPdfEnabled; }
    public Boolean getPrioritySupport() { return prioritySupport; }
    public void setPrioritySupport(Boolean prioritySupport) { this.prioritySupport = prioritySupport; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
