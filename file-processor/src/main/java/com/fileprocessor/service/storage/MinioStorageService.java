package com.fileprocessor.service.storage;

import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO storage service
 */
@Component
@ConditionalOnProperty(name = "storage.minio.enabled", havingValue = "true")
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${storage.minio.bucket:file-processor}")
    private String bucket;

    @Value("${storage.minio.url-prefix:}")
    private String urlPrefix;

    private final MinioClient minioClient;

    @Autowired
    public MinioStorageService(
            @Value("${storage.minio.endpoint}") String endpoint,
            @Value("${storage.minio.access-key}") String accessKey,
            @Value("${storage.minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Override
    public StorageType getType() {
        return StorageType.MINIO;
    }

    @Override
    public void initialize() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO storage", e);
        }
    }

    @Override
    public String save(InputStream inputStream, String path, String filename) {
        try {
            String objectName = path + "/" + filename;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, -1, 10485760) // 10MB part size
                            .build()
            );

            log.info("Saved file to MinIO: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Failed to save file to MinIO", e);
            throw new RuntimeException("Failed to save file to MinIO", e);
        }
    }

    @Override
    public InputStream read(String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to read file from MinIO: {}", path, e);
            throw new RuntimeException("Failed to read file from MinIO", e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUrl(String path) {
        if (urlPrefix != null && !urlPrefix.isEmpty()) {
            return urlPrefix + "/" + path;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(bucket)
                            .object(path)
                            .expiry(3600) // 1 hour
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return "minio://" + bucket + "/" + path;
        }
    }

    @Override
    public long getSize(String path) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            log.error("Failed to get file size from MinIO: {}", path, e);
            return 0;
        }
    }
}
