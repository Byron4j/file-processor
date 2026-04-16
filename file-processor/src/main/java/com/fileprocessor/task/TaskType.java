package com.fileprocessor.task;

/**
 * Task type enumeration
 */
public enum TaskType {
    CONVERT("CONVERT", "格式转换"),
    EXTRACT("EXTRACT", "文本提取"),
    MERGE("MERGE", "合并文件"),
    SPLIT("SPLIT", "拆分文件"),
    WATERMARK("WATERMARK", "添加水印"),
    ENCRYPT("ENCRYPT", "加密文件"),
    DECRYPT("DECRYPT", "解密文件"),
    COMPRESS("COMPRESS", "压缩文件"),
    HASH("HASH", "计算哈希"),
    PREVIEW("PREVIEW", "生成预览"),
    BATCH("BATCH", "批量处理"),
    OCR("OCR", "文字识别");

    private final String code;
    private final String description;

    TaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TaskType fromCode(String code) {
        for (TaskType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type: " + code);
    }
}
