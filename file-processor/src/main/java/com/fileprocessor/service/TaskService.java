package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.TaskRecord;
import com.fileprocessor.mapper.TaskRecordMapper;
import com.fileprocessor.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task service for async task management
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    private final Map<String, TaskHandler<?>> handlers = new ConcurrentHashMap<>();
    private final Set<String> runningTasks = ConcurrentHashMap.newKeySet();

    /**
     * Register task handler
     */
    public void registerHandler(TaskHandler<?> handler) {
        handlers.put(handler.getTaskType().getCode(), handler);
        log.info("Registered task handler: {}", handler.getTaskType());
    }

    /**
     * Submit a new task
     */
    public TaskRecord submitTask(TaskSubmitRequest request) {
        String taskId = generateTaskId();
        log.info("Submitting task: {} of type {}", taskId, request.getTaskType());

        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setTaskType(request.getTaskType());
        record.setTaskName(request.getTaskName());
        record.setSourceFiles(request.getSourceFiles());
        record.setParameters(request.getParameters());
        record.setCallbackUrl(request.getCallbackUrl());
        record.setStatus(TaskStatus.PENDING.getCode());
        record.setProgress(0);
        record.setTotalItems(request.getTotalItems() != null ? request.getTotalItems() : 1);
        record.setProcessedItems(0);
        record.setMaxRetry(request.getMaxRetry() != null ? request.getMaxRetry() : 3);

        taskRecordMapper.insert(record);

        // Execute task asynchronously
        executeTaskAsync(taskId);

        return record;
    }

    /**
     * Execute task asynchronously
     */
    @Async("taskExecutor")
    public void executeTaskAsync(String taskId) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record == null) {
            log.error("Task not found: {}", taskId);
            return;
        }

        // Check if already processing
        if (!runningTasks.add(taskId)) {
            log.warn("Task {} is already running", taskId);
            return;
        }

        try {
            record.setStatus(TaskStatus.PROCESSING.getCode());
            record.setStartedAt(LocalDateTime.now());
            record.setCurrentStep("Starting...");
            taskRecordMapper.updateById(record);

            TaskHandler<?> handler = handlers.get(record.getTaskType());
            if (handler == null) {
                throw new IllegalStateException("No handler found for task type: " + record.getTaskType());
            }

            DefaultProgressListener listener = new DefaultProgressListener(taskId, taskRecordMapper);

            @SuppressWarnings("unchecked")
            TaskResult result = ((TaskHandler<Object>) handler).execute(record.getParameters(), listener);

            // Update completion status
            record.setStatus(result.isSuccess() ? TaskStatus.SUCCESS.getCode() : TaskStatus.FAILED.getCode());
            record.setProgress(100);
            record.setCurrentStep(result.isSuccess() ? "Completed" : "Failed: " + result.getMessage());
            record.setResult(result.getData());
            record.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Task execution failed: {}", taskId, e);
            record.setStatus(TaskStatus.FAILED.getCode());
            record.setErrorMessage(e.getMessage());
            record.setErrorStack(getStackTraceString(e));
            record.setCompletedAt(LocalDateTime.now());

            // Retry logic
            if (record.getRetryCount() < record.getMaxRetry()) {
                record.setRetryCount(record.getRetryCount() + 1);
                record.setStatus(TaskStatus.PENDING.getCode());
                record.setErrorMessage("Retry attempt " + record.getRetryCount() + ": " + e.getMessage());
                log.info("Retrying task {} (attempt {}/{})", taskId, record.getRetryCount(), record.getMaxRetry());
            }
        } finally {
            taskRecordMapper.updateById(record);
            runningTasks.remove(taskId);

            // Trigger callback if configured
            if (record.getCallbackUrl() != null) {
                triggerCallback(record);
            }
        }
    }

    /**
     * Get task status
     */
    public FileResponse getTaskStatus(String taskId) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Task not found: " + taskId)
                    .build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", record.getTaskId());
        data.put("taskType", record.getTaskType());
        data.put("taskName", record.getTaskName());
        data.put("status", record.getStatus());
        data.put("progress", record.getProgress());
        data.put("currentStep", record.getCurrentStep());
        data.put("processedItems", record.getProcessedItems());
        data.put("totalItems", record.getTotalItems());
        data.put("createdAt", record.getCreatedAt());
        data.put("startedAt", record.getStartedAt());
        data.put("completedAt", record.getCompletedAt());
        data.put("retryCount", record.getRetryCount());

        if (record.getErrorMessage() != null) {
            data.put("error", record.getErrorMessage());
        }

        if (record.getResult() != null) {
            data.put("result", record.getResult());
        }

        return FileResponse.builder()
                .success(true)
                .message("Task status retrieved")
                .data(data)
                .build();
    }

    /**
     * Cancel task
     */
    public FileResponse cancelTask(String taskId) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Task not found: " + taskId)
                    .build();
        }

        if (TaskStatus.SUCCESS.getCode().equals(record.getStatus()) ||
            TaskStatus.FAILED.getCode().equals(record.getStatus()) ||
            TaskStatus.CANCELLED.getCode().equals(record.getStatus())) {
            return FileResponse.builder()
                    .success(false)
                    .message("Task is already in terminal state: " + record.getStatus())
                    .build();
        }

        record.setStatus(TaskStatus.CANCELLED.getCode());
        record.setCurrentStep("Cancelled by user");
        record.setCompletedAt(LocalDateTime.now());
        taskRecordMapper.updateById(record);

        runningTasks.remove(taskId);

        return FileResponse.builder()
                .success(true)
                .message("Task cancelled successfully")
                .build();
    }

    /**
     * List tasks
     */
    public List<TaskRecord> listTasks(String status, String taskType, int page, int size) {
        // Simple implementation - can be enhanced with pagination
        if (status != null) {
            return taskRecordMapper.findByStatus(status);
        }
        return taskRecordMapper.selectList(null);
    }

    /**
     * Generate unique task ID
     */
    private String generateTaskId() {
        return "t-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Get stack trace as string
     */
    private String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        return sb.toString();
    }

    /**
     * Trigger callback
     */
    private void triggerCallback(TaskRecord record) {
        // TODO: Implement HTTP callback
        log.info("Triggering callback for task {} to {}", record.getTaskId(), record.getCallbackUrl());
        record.setCallbackStatus("PENDING");
        taskRecordMapper.updateById(record);
    }

    /**
     * Submit simple task (simplified API)
     */
    public String submitTask(String taskType, String taskName, int totalItems, Map<String, Object> parameters) {
        TaskSubmitRequest request = new TaskSubmitRequest();
        request.setTaskType(taskType);
        request.setTaskName(taskName);
        request.setTotalItems(totalItems);
        request.setParameters(parameters);

        TaskRecord record = submitTask(request);
        return record.getTaskId();
    }

    /**
     * Update task progress
     */
    public void updateTaskProgress(String taskId, int progress, String currentStep) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record != null) {
            record.setProgress(progress);
            record.setCurrentStep(currentStep);
            record.setProcessedItems((int) (progress * record.getTotalItems() / 100.0));
            taskRecordMapper.updateById(record);
            log.debug("Task {} progress: {}% - {}", taskId, progress, currentStep);
        }
    }

    /**
     * Complete task with result
     */
    public void completeTask(String taskId, Map<String, Object> result) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record != null) {
            record.setStatus(TaskStatus.SUCCESS.getCode());
            record.setProgress(100);
            record.setCurrentStep("Completed");
            record.setProcessedItems(record.getTotalItems());
            record.setCompletedAt(LocalDateTime.now());
            record.setResult(result);
            taskRecordMapper.updateById(record);
            log.info("Task {} completed successfully", taskId);
        }
        runningTasks.remove(taskId);
    }

    /**
     * Fail task
     */
    public void failTask(String taskId, String errorMessage) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record != null) {
            record.setStatus(TaskStatus.FAILED.getCode());
            record.setErrorMessage(errorMessage);
            record.setCompletedAt(LocalDateTime.now());
            taskRecordMapper.updateById(record);
            log.error("Task {} failed: {}", taskId, errorMessage);
        }
        runningTasks.remove(taskId);
    }

    /**
     * Get task result
     */
    public Map<String, Object> getTaskResult(String taskId) {
        TaskRecord record = taskRecordMapper.findByTaskId(taskId);
        if (record != null && record.getResult() != null) {
            return record.getResult();
        }
        return null;
    }

    // ==================== Request DTO ====================

    public static class TaskSubmitRequest {
        private String taskType;
        private String taskName;
        private List<Map<String, String>> sourceFiles;
        private Map<String, Object> parameters;
        private String callbackUrl;
        private Integer totalItems;
        private Integer maxRetry;

        // Getters and Setters
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public List<Map<String, String>> getSourceFiles() { return sourceFiles; }
        public void setSourceFiles(List<Map<String, String>> sourceFiles) { this.sourceFiles = sourceFiles; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        public Integer getMaxRetry() { return maxRetry; }
        public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    }
}
