package com.fileprocessor.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Task record entity for async task management
 */
@TableName(value = "task_record", autoResultMap = true)
public class TaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("task_type")
    private String taskType;

    @TableField("task_name")
    private String taskName;

    @TableField(value = "source_files", typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> sourceFiles;

    @TableField(value = "target_files", typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> targetFiles;

    @TableField(value = "parameters", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> parameters;

    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress;

    @TableField("current_step")
    private String currentStep;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("processed_items")
    private Integer processedItems;

    @TableField(value = "result", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> result;

    @TableField("error_message")
    private String errorMessage;

    @TableField("error_stack")
    private String errorStack;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry")
    private Integer maxRetry;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField("callback_status")
    private String callbackStatus;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("created_by")
    private String createdBy;

    // Default constructor
    public TaskRecord() {
        this.status = "PENDING";
        this.progress = 0;
        this.totalItems = 1;
        this.processedItems = 0;
        this.retryCount = 0;
        this.maxRetry = 3;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public List<Map<String, String>> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<Map<String, String>> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public List<Map<String, String>> getTargetFiles() {
        return targetFiles;
    }

    public void setTargetFiles(List<Map<String, String>> targetFiles) {
        this.targetFiles = targetFiles;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(Integer processedItems) {
        this.processedItems = processedItems;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackStatus() {
        return callbackStatus;
    }

    public void setCallbackStatus(String callbackStatus) {
        this.callbackStatus = callbackStatus;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
