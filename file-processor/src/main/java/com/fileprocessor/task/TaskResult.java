package com.fileprocessor.task;

import java.util.HashMap;
import java.util.Map;

/**
 * Task execution result
 */
public class TaskResult {

    private boolean success;
    private String message;
    private Map<String, Object> data;

    public TaskResult() {
        this.data = new HashMap<>();
    }

    public TaskResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public static TaskResult success(String message) {
        return new TaskResult(true, message);
    }

    public static TaskResult success() {
        return new TaskResult(true, "Success");
    }

    public static TaskResult failure(String message) {
        return new TaskResult(false, message);
    }

    public TaskResult withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public TaskResult withData(Map<String, Object> data) {
        this.data.putAll(data);
        return this;
    }

    // Getters and Setters
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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
