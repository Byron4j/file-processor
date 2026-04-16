package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.TaskRecord;
import com.fileprocessor.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Task management REST API controller
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    /**
     * Submit async task
     */
    @PostMapping("/submit")
    public ResponseEntity<FileResponse> submitTask(
            @RequestBody @Valid SubmitTaskRequest request) {
        log.info("REST request to submit task: {}", request.getTaskType());

        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType(request.getTaskType());
        submitRequest.setTaskName(request.getTaskName());
        submitRequest.setSourceFiles(request.getSourceFiles());
        submitRequest.setParameters(request.getParameters());
        submitRequest.setCallbackUrl(request.getCallbackUrl());
        submitRequest.setTotalItems(request.getTotalItems());
        submitRequest.setMaxRetry(request.getMaxRetry());

        TaskRecord record = taskService.submitTask(submitRequest);

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Task submitted successfully")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus(),
                        "queryUrl", "/api/tasks/" + record.getTaskId() + "/status"
                ))
                .build());
    }

    /**
     * Get task status
     */
    @GetMapping("/{taskId}/status")
    public ResponseEntity<FileResponse> getTaskStatus(
            @PathVariable String taskId) {
        log.info("REST request to get task status: {}", taskId);

        FileResponse response = taskService.getTaskStatus(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel task
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<FileResponse> cancelTask(
            @PathVariable String taskId) {
        log.info("REST request to cancel task: {}", taskId);

        FileResponse response = taskService.cancelTask(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * List tasks
     */
    @GetMapping
    public ResponseEntity<FileResponse> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST request to list tasks: status={}, type={}", status, taskType);

        List<TaskRecord> tasks = taskService.listTasks(status, taskType, page, size);

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Tasks retrieved")
                .data(Map.of(
                        "items", tasks,
                        "total", tasks.size(),
                        "page", page,
                        "size", size
                ))
                .build());
    }

    // ==================== Request DTO ====================

    public static class SubmitTaskRequest {
        private String taskType;
        private String taskName;
        private List<Map<String, String>> sourceFiles;
        private Map<String, Object> parameters;
        private String callbackUrl;
        private Integer totalItems;
        private Integer maxRetry;

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
