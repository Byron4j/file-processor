package com.fileprocessor.task;

/**
 * Task status enumeration
 */
public enum TaskStatus {
    PENDING("PENDING", "等待中"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + code);
    }
}
