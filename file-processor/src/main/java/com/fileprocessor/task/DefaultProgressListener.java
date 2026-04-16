package com.fileprocessor.task;

import com.fileprocessor.entity.TaskRecord;
import com.fileprocessor.mapper.TaskRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default task progress listener with database persistence
 */
public class DefaultProgressListener implements TaskProgressListener {

    private static final Logger log = LoggerFactory.getLogger(DefaultProgressListener.class);

    private final String taskId;
    private final TaskRecordMapper taskRecordMapper;
    private final AtomicBoolean cancelled;

    public DefaultProgressListener(String taskId, TaskRecordMapper taskRecordMapper) {
        this.taskId = taskId;
        this.taskRecordMapper = taskRecordMapper;
        this.cancelled = new AtomicBoolean(false);
    }

    @Override
    public void onProgress(int progress, String message) {
        try {
            TaskRecord record = taskRecordMapper.findByTaskId(taskId);
            if (record != null) {
                record.setProgress(progress);
                record.setCurrentStep(message);
                record.setProcessedItems((int) (progress / 100.0 * record.getTotalItems()));
                taskRecordMapper.updateById(record);
                log.debug("Task {} progress: {}% - {}", taskId, progress, message);
            }
        } catch (Exception e) {
            log.error("Failed to update task progress", e);
        }
    }

    @Override
    public void onComplete(TaskResult result) {
        try {
            TaskRecord record = taskRecordMapper.findByTaskId(taskId);
            if (record != null) {
                record.setStatus(result.isSuccess() ? TaskStatus.SUCCESS.getCode() : TaskStatus.FAILED.getCode());
                record.setProgress(100);
                record.setCurrentStep(result.isSuccess() ? "Completed" : "Failed");
                record.setResult(result.getData());
                record.setCompletedAt(LocalDateTime.now());
                taskRecordMapper.updateById(record);
                log.info("Task {} completed: {}", taskId, result.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to update task completion", e);
        }
    }

    @Override
    public void onError(String error, String stackTrace) {
        try {
            TaskRecord record = taskRecordMapper.findByTaskId(taskId);
            if (record != null) {
                record.setStatus(TaskStatus.FAILED.getCode());
                record.setErrorMessage(error);
                record.setErrorStack(stackTrace);
                record.setCompletedAt(LocalDateTime.now());
                taskRecordMapper.updateById(record);
                log.error("Task {} failed: {}", taskId, error);
            }
        } catch (Exception e) {
            log.error("Failed to update task error", e);
        }
    }

    @Override
    public boolean isCancelled() {
        if (cancelled.get()) {
            return true;
        }
        // Check database for cancellation
        try {
            TaskRecord record = taskRecordMapper.findByTaskId(taskId);
            if (record != null && TaskStatus.CANCELLED.getCode().equals(record.getStatus())) {
                cancelled.set(true);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to check task cancellation", e);
        }
        return false;
    }

    public void cancel() {
        cancelled.set(true);
    }
}
