package com.fileprocessor.service.storage;

/**
 * Storage type enumeration
 */
public enum StorageType {
    LOCAL("LOCAL", "本地存储"),
    MINIO("MINIO", "MinIO对象存储"),
    ALIYUN_OSS("ALIYUN_OSS", "阿里云OSS"),
    AWS_S3("AWS_S3", "AWS S3");

    private final String code;
    private final String description;

    StorageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StorageType fromCode(String code) {
        for (StorageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage type: " + code);
    }
}
