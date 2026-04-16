package com.fileprocessor.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.util.Date;

/**
 * Alibaba Cloud OSS storage service
 */
@Component
@ConditionalOnProperty(name = "storage.aliyun-oss.enabled", havingValue = "true")
public class AliyunOssService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssService.class);

    private final OSS ossClient;
    private final String bucket;
    private final String endpoint;
    private final String urlPrefix;

    @Autowired
    public AliyunOssService(
            @Value("${storage.aliyun-oss.endpoint}") String endpoint,
            @Value("${storage.aliyun-oss.access-key-id}") String accessKeyId,
            @Value("${storage.aliyun-oss.access-key-secret}") String accessKeySecret,
            @Value("${storage.aliyun-oss.bucket}") String bucket,
            @Value("${storage.aliyun-oss.url-prefix:}") String urlPrefix) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.urlPrefix = urlPrefix;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Override
    public StorageType getType() {
        return StorageType.ALIYUN_OSS;
    }

    @Override
    public void initialize() {
        try {
            if (!ossClient.doesBucketExist(bucket)) {
                ossClient.createBucket(bucket);
                log.info("Created OSS bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize OSS storage", e);
        }
    }

    @Override
    public String save(InputStream inputStream, String path, String filename) {
        try {
            String key = path + "/" + filename;
            ossClient.putObject(bucket, key, inputStream);
            log.info("Saved file to OSS: {}", key);
            return key;
        } catch (Exception e) {
            log.error("Failed to save file to OSS", e);
            throw new RuntimeException("Failed to save file to OSS", e);
        }
    }

    @Override
    public InputStream read(String path) {
        try {
            OSSObject ossObject = ossClient.getObject(bucket, path);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to read file from OSS: {}", path, e);
            throw new RuntimeException("Failed to read file from OSS", e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            ossClient.deleteObject(bucket, path);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from OSS: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return ossClient.doesObjectExist(bucket, path);
        } catch (Exception e) {
            log.error("Failed to check file existence in OSS: {}", path, e);
            return false;
        }
    }

    @Override
    public String getUrl(String path) {
        if (urlPrefix != null && !urlPrefix.isEmpty()) {
            return urlPrefix + "/" + path;
        }
        // Generate signed URL valid for 1 hour
        long expiration = System.currentTimeMillis() + 3600 * 1000;
        return ossClient.generatePresignedUrl(bucket, path, new Date(expiration)).toString();
    }

    @Override
    public long getSize(String path) {
        try {
            return ossClient.getObjectMetadata(bucket, path).getContentLength();
        } catch (Exception e) {
            log.error("Failed to get file size from OSS: {}", path, e);
            return 0;
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
