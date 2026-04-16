package com.fileprocessor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.mapper.FileMetadataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecycleBinService {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinService.class);

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Value("${custom.file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${recycle-bin.retention-days:30}")
    private int retentionDays;

    /**
     * 移入回收站
     */
    public FileResponse moveToRecycleBin(String fileId, Long userId) {
        try {
            FileMetadata file = fileMetadataMapper.selectById(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found")
                        .build();
            }

            // Check ownership
            if (!String.valueOf(userId).equals(file.getCreatedBy())) {
                return FileResponse.builder()
                        .success(false)
                        .message("No permission to delete this file")
                        .build();
            }

            // Soft delete
            file.setDeleted(true);
            file.setDeletedAt(LocalDateTime.now());
            file.setDeletedBy(userId.longValue());
            fileMetadataMapper.updateById(file);

            return FileResponse.builder()
                    .success(true)
                    .message("File moved to recycle bin")
                    .data(Map.of(
                            "fileId", fileId,
                            "deletedAt", file.getDeletedAt().toString(),
                            "willBePermanentlyDeleted", file.getDeletedAt().plusDays(retentionDays).toString()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to move file to recycle bin", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 恢复文件
     */
    public FileResponse restore(String fileId, Long userId) {
        try {
            FileMetadata file = fileMetadataMapper.selectById(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found")
                        .build();
            }

            // Check ownership
            if (!String.valueOf(userId).equals(file.getCreatedBy())) {
                return FileResponse.builder()
                        .success(false)
                        .message("No permission to restore this file")
                        .build();
            }

            // Check if deleted
            if (file.getDeleted() == null || !file.getDeleted()) {
                return FileResponse.builder()
                        .success(false)
                        .message("File is not in recycle bin")
                        .build();
            }

            // Restore
            file.setDeleted(false);
            file.setDeletedAt(null);
            ((FileMetadata) file).setDeletedBy((String) null);
            fileMetadataMapper.updateById(file);

            return FileResponse.builder()
                    .success(true)
                    .message("File restored successfully")
                    .data(Map.of("fileId", fileId))
                    .build();

        } catch (Exception e) {
            log.error("Failed to restore file", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 永久删除
     */
    public FileResponse permanentDelete(String fileId, Long userId) {
        try {
            FileMetadata file = fileMetadataMapper.selectById(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found")
                        .build();
            }

            // Check ownership
            if (!String.valueOf(userId).equals(file.getCreatedBy())) {
                return FileResponse.builder()
                        .success(false)
                        .message("No permission to delete this file")
                        .build();
            }

            // Delete physical file
            if (file.getStoragePath() != null) {
                try {
                    Files.deleteIfExists(Path.of(file.getStoragePath()));
                } catch (IOException e) {
                    log.warn("Failed to delete physical file: {}", file.getStoragePath(), e);
                }
            }

            // Delete from database
            fileMetadataMapper.deleteById(fileId);

            return FileResponse.builder()
                    .success(true)
                    .message("File permanently deleted")
                    .build();

        } catch (Exception e) {
            log.error("Failed to permanently delete file", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取回收站列表
     */
    public FileResponse listRecycleBin(Long userId) {
        try {
            List<FileMetadata> files = fileMetadataMapper.selectList(
                    new LambdaQueryWrapper<FileMetadata>()
                            .eq(FileMetadata::getCreatedBy, String.valueOf(userId))
                            .eq(FileMetadata::getDeleted, true)
                            .orderByDesc(FileMetadata::getDeletedAt)
            );

            List<Map<String, Object>> result = files.stream().map(f -> {
                Map<String, Object> map = new HashMap<>();
                map.put("fileId", f.getFileId());
                map.put("fileName", f.getOriginalName());
                map.put("fileSize", f.getFileSize());
                map.put("deletedAt", f.getDeletedAt() != null ? f.getDeletedAt().toString() : "");
                map.put("willExpireAt", f.getDeletedAt() != null ? f.getDeletedAt().plusDays(retentionDays).toString() : "");
                return map;
            }).collect(Collectors.toList());

            return FileResponse.builder()
                    .success(true)
                    .message("Recycle bin list retrieved")
                    .data(Map.of(
                            "files", result,
                            "total", files.size(),
                            "retentionDays", retentionDays
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to list recycle bin", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 清空回收站
     */
    public FileResponse emptyRecycleBin(Long userId) {
        try {
            List<FileMetadata> files = fileMetadataMapper.selectList(
                    new LambdaQueryWrapper<FileMetadata>()
                            .eq(FileMetadata::getCreatedBy, String.valueOf(userId))
                            .eq(FileMetadata::getDeleted, true)
            );

            int deletedCount = 0;
            for (FileMetadata file : files) {
                // Delete physical file
                if (file.getStoragePath() != null) {
                    try {
                        Files.deleteIfExists(Path.of(file.getStoragePath()));
                    } catch (IOException e) {
                        log.warn("Failed to delete physical file: {}", file.getStoragePath(), e);
                    }
                }
                // Delete from database
                fileMetadataMapper.deleteById(file.getId());
                deletedCount++;
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Recycle bin emptied")
                    .data(Map.of("deletedCount", deletedCount))
                    .build();

        } catch (Exception e) {
            log.error("Failed to empty recycle bin", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 清理过期文件
     */
    public void cleanupExpiredFiles() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<FileMetadata> files = fileMetadataMapper.selectList(
                    new LambdaQueryWrapper<FileMetadata>()
                            .eq(FileMetadata::getDeleted, true)
                            .lt(FileMetadata::getDeletedAt, cutoff)
            );

            int count = 0;
            for (FileMetadata file : files) {
                if (file.getStoragePath() != null) {
                    try {
                        Files.deleteIfExists(Path.of(file.getStoragePath()));
                    } catch (IOException e) {
                        log.warn("Failed to delete physical file: {}", file.getStoragePath(), e);
                    }
                }
                fileMetadataMapper.deleteById(file.getId());
                count++;
            }

            log.info("Cleaned up {} expired files from recycle bin", count);

        } catch (Exception e) {
            log.error("Failed to cleanup expired files", e);
        }
    }
}
