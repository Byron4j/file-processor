package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileCategory;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.mapper.FileCategoryMapper;
import com.fileprocessor.mapper.FileMetadataMapper;
import com.fileprocessor.service.storage.StorageManager;
import com.fileprocessor.service.storage.StorageType;
import com.fileprocessor.util.FileHashCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * File metadata management service
 */
@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private FileCategoryMapper fileCategoryMapper;

    @Autowired
    private StorageManager storageManager;

    /**
     * Register file metadata
     */
    public FileResponse registerFile(RegisterRequest request) {
        log.info("Registering file: {}", request.getOriginalName());

        String fileId = generateFileId();

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setOriginalName(request.getOriginalName());
        metadata.setStorageType(request.getStorageType() != null ? request.getStorageType() : StorageType.LOCAL.getCode());
        metadata.setStoragePath(request.getStoragePath());
        metadata.setFileSize(request.getFileSize());
        metadata.setMimeType(request.getMimeType());
        metadata.setExtension(getExtension(request.getOriginalName()));
        metadata.setCategoryId(request.getCategoryId());
        metadata.setTags(request.getTags());
        metadata.setDescription(request.getDescription());
        metadata.setSourceType(request.getSourceType());

        // Calculate hashes if file exists locally
        try {
            File file = new File(request.getStoragePath());
            if (file.exists()) {
                metadata.setMd5Hash(FileHashCalculator.calculateHash(request.getStoragePath(), FileHashCalculator.HashAlgorithm.MD5));
                metadata.setSha256Hash(FileHashCalculator.calculateHash(request.getStoragePath(), FileHashCalculator.HashAlgorithm.SHA_256));
            }
        } catch (Exception e) {
            log.warn("Failed to calculate hash for file: {}", request.getStoragePath());
        }

        fileMetadataMapper.insert(metadata);

        return FileResponse.builder()
                .success(true)
                .message("File registered successfully")
                .data(Map.of(
                        "fileId", fileId,
                        "createdAt", metadata.getCreatedAt()
                ))
                .build();
    }

    /**
     * Get file metadata by fileId
     */
    public FileResponse getFile(String fileId) {
        FileMetadata metadata = fileMetadataMapper.findByFileId(fileId);
        if (metadata == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("File not found: " + fileId)
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("File metadata retrieved")
                .data(convertToMap(metadata))
                .build();
    }

    /**
     * List files with filtering
     */
    public FileResponse listFiles(ListRequest request) {
        // Query all and filter in memory for simplicity
        // In production, use MyBatis Plus QueryWrapper for database-level filtering
        List<FileMetadata> allFiles = fileMetadataMapper.selectList(null);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (FileMetadata metadata : allFiles) {
            if (request.getCategoryId() != null && !request.getCategoryId().equals(metadata.getCategoryId())) {
                continue;
            }
            if (request.getExtension() != null && !request.getExtension().equalsIgnoreCase(metadata.getExtension())) {
                continue;
            }
            if (request.getStatus() != null && !request.getStatus().equals(metadata.getStatus())) {
                continue;
            }
            if (request.getStorageType() != null && !request.getStorageType().equals(metadata.getStorageType())) {
                continue;
            }

            filtered.add(convertToMap(metadata));
        }

        // Pagination
        int total = filtered.size();
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<Map<String, Object>> items = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        return FileResponse.builder()
                .success(true)
                .message("File list retrieved")
                .data(Map.of(
                        "total", total,
                        "page", page,
                        "size", size,
                        "items", items
                ))
                .build();
    }

    /**
     * Update file tags
     */
    public FileResponse updateTags(String fileId, List<String> tags) {
        FileMetadata metadata = fileMetadataMapper.findByFileId(fileId);
        if (metadata == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("File not found: " + fileId)
                    .build();
        }

        metadata.setTags(tags);
        fileMetadataMapper.updateById(metadata);

        return FileResponse.builder()
                .success(true)
                .message("Tags updated successfully")
                .build();
    }

    /**
     * Delete file (soft delete)
     */
    public FileResponse deleteFile(String fileId) {
        FileMetadata metadata = fileMetadataMapper.findByFileId(fileId);
        if (metadata == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("File not found: " + fileId)
                    .build();
        }

        fileMetadataMapper.softDeleteByFileId(fileId);
        return FileResponse.builder()
                .success(true)
                .message("File deleted successfully")
                .build();
    }

    /**
     * Get all categories
     */
    public FileResponse getCategories() {
        List<FileCategory> categories = fileCategoryMapper.findAllActive();
        List<Map<String, Object>> result = new ArrayList<>();

        for (FileCategory category : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("name", category.getName());
            map.put("code", category.getCode());
            map.put("parentId", category.getParentId());
            map.put("description", category.getDescription());
            map.put("icon", category.getIcon());
            result.add(map);
        }

        return FileResponse.builder()
                .success(true)
                .message("Categories retrieved")
                .data(Map.of("items", result))
                .build();
    }

    /**
     * Get file statistics
     */
    public FileResponse getStatistics() {
        Long totalCount = fileMetadataMapper.countByStatus(1);
        Long deletedCount = fileMetadataMapper.countByStatus(0);

        // Count by category
        List<FileCategory> categories = fileCategoryMapper.findAllActive();
        Map<String, Object> categoryStats = new HashMap<>();
        for (FileCategory category : categories) {
            Long count = fileMetadataMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileMetadata>()
                            .eq(FileMetadata::getCategoryId, category.getId())
                            .eq(FileMetadata::getStatus, 1)
            );
            Long size = fileMetadataMapper.sumFileSizeByCategory(category.getId());
            categoryStats.put(category.getCode(), Map.of(
                    "count", count != null ? count : 0,
                    "size", size != null ? size : 0
            ));
        }

        return FileResponse.builder()
                .success(true)
                .message("Statistics retrieved")
                .data(Map.of(
                        "totalCount", totalCount != null ? totalCount : 0,
                        "deletedCount", deletedCount != null ? deletedCount : 0,
                        "categoryStats", categoryStats
                ))
                .build();
    }

    /**
     * Convert metadata to map
     */
    private Map<String, Object> convertToMap(FileMetadata metadata) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", metadata.getFileId());
        map.put("originalName", metadata.getOriginalName());
        map.put("storageType", metadata.getStorageType());
        map.put("storagePath", metadata.getStoragePath());
        map.put("fileSize", metadata.getFileSize());
        map.put("mimeType", metadata.getMimeType());
        map.put("extension", metadata.getExtension());
        map.put("md5Hash", metadata.getMd5Hash());
        map.put("sha256Hash", metadata.getSha256Hash());
        map.put("categoryId", metadata.getCategoryId());
        map.put("tags", metadata.getTags());
        map.put("description", metadata.getDescription());
        map.put("metadata", metadata.getMetadata());
        map.put("sourceType", metadata.getSourceType());
        map.put("referenceCount", metadata.getReferenceCount());
        map.put("expireAt", metadata.getExpireAt());
        map.put("status", metadata.getStatus());
        map.put("createdAt", metadata.getCreatedAt());
        map.put("updatedAt", metadata.getUpdatedAt());
        map.put("createdBy", metadata.getCreatedBy());
        return map;
    }

    /**
     * Generate unique file ID
     */
    private String generateFileId() {
        return "f-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Get file extension
     */
    private String getExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    // ==================== Request DTOs ====================

    public static class RegisterRequest {
        private String originalName;
        private String storageType;
        private String storagePath;
        private Long fileSize;
        private String mimeType;
        private Long categoryId;
        private List<String> tags;
        private String description;
        private String sourceType;

        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getStorageType() { return storageType; }
        public void setStorageType(String storageType) { this.storageType = storageType; }
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    }

    public static class ListRequest {
        private Long categoryId;
        private String extension;
        private Integer status = 1;
        private String storageType;
        private String tag;
        private Integer page = 1;
        private Integer size = 20;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStorageType() { return storageType; }
        public void setStorageType(String storageType) { this.storageType = storageType; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }
}
