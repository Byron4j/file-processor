package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.entity.FileVersion;
import com.fileprocessor.mapper.FileMetadataMapper;
import com.fileprocessor.mapper.FileVersionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件版本管理服务
 */
@Service
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    @Autowired
    private FileVersionMapper fileVersionMapper;

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Value("${file.version.max-count:10}")
    private int maxVersionCount;

    @Value("${file.version.storage-path:./versions}")
    private String versionStoragePath;

    /**
     * 保存文件版本
     */
    @Transactional
    public FileResponse saveVersion(String fileId, String description, List<String> tags) {
        log.info("Saving version for file: {}, description: {}", fileId, description);

        try {
            // 1. 获取文件元数据
            FileMetadata file = fileMetadataMapper.findByFileId(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found: " + fileId)
                        .build();
            }

            // 2. 获取下一个版本号
            Integer maxVersion = fileVersionMapper.selectMaxVersionByFileId(fileId);
            int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

            // 3. 复制文件到版本存储
            String versionId = generateVersionId();
            String versionPath = generateVersionPath(fileId, nextVersion, file.getExtension());

            Path sourcePath = Paths.get(file.getStoragePath());
            Path targetPath = Paths.get(versionPath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 保存版本记录
            FileVersion version = new FileVersion();
            version.setVersionId(versionId);
            version.setFileId(fileId);
            version.setVersionNumber(nextVersion);
            version.setStoragePath(versionPath);
            version.setFileSize(file.getFileSize());
            version.setFileHash(file.getFileHash());
            version.setDescription(description);
            version.setTagList(tags);
            version.setCreatedAt(LocalDateTime.now());

            fileVersionMapper.insert(version);

            // 5. 清理旧版本
            cleanupOldVersions(fileId);

            log.info("Version saved: {} for file: {}", versionId, fileId);

            return FileResponse.builder()
                    .success(true)
                    .message("Version saved successfully")
                    .fileId(versionId)
                    .data(Map.of(
                            "versionId", versionId,
                            "versionNumber", nextVersion,
                            "fileId", fileId,
                            "description", description != null ? description : "",
                            "createdAt", version.getCreatedAt()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to save version for file: {}", fileId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to save version: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取文件版本列表
     */
    public FileResponse listVersions(String fileId) {
        log.info("Listing versions for file: {}", fileId);

        try {
            // 检查文件是否存在
            FileMetadata file = fileMetadataMapper.findByFileId(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found: " + fileId)
                        .build();
            }

            // 获取版本列表
            List<FileVersion> versions = fileVersionMapper.selectByFileId(fileId);
            Integer maxVersion = fileVersionMapper.selectMaxVersionByFileId(fileId);

            List<Map<String, Object>> versionList = new ArrayList<>();
            for (FileVersion v : versions) {
                Map<String, Object> versionData = new HashMap<>();
                versionData.put("versionId", v.getVersionId());
                versionData.put("versionNumber", v.getVersionNumber());
                versionData.put("description", v.getDescription());
                versionData.put("fileSize", v.getFileSize());
                versionData.put("fileHash", v.getFileHash());
                versionData.put("tags", v.getTagList());
                versionData.put("createdAt", v.getCreatedAt());
                versionData.put("isLatest", v.getVersionNumber() == (maxVersion != null ? maxVersion : 0));
                versionList.add(versionData);
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Versions retrieved")
                    .data(Map.of(
                            "fileId", fileId,
                            "total", versionList.size(),
                            "versions", versionList
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to list versions for file: {}", fileId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to list versions: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 恢复到指定版本
     */
    @Transactional
    public FileResponse restoreVersion(String fileId, String versionId) {
        log.info("Restoring file: {} to version: {}", fileId, versionId);

        try {
            // 1. 获取文件和版本信息
            FileMetadata file = fileMetadataMapper.findByFileId(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found: " + fileId)
                        .build();
            }

            FileVersion version = fileVersionMapper.selectByVersionId(versionId);
            if (version == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Version not found: " + versionId)
                        .build();
            }

            // 2. 先保存当前版本（自动备份）
            FileResponse backupResult = saveVersion(fileId, "Auto backup before restore to v" + version.getVersionNumber(), null);
            if (!backupResult.isSuccess()) {
                log.warn("Failed to create backup before restore: {}", backupResult.getMessage());
            }

            // 3. 恢复版本文件
            Path versionPath = Paths.get(version.getStoragePath());
            Path currentPath = Paths.get(file.getStoragePath());

            if (!Files.exists(versionPath)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Version file not found: " + version.getStoragePath())
                        .build();
            }

            Files.copy(versionPath, currentPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. 更新文件元数据
            file.setFileSize(version.getFileSize());
            file.setFileHash(version.getFileHash());
            file.setUpdatedAt(LocalDateTime.now());
            fileMetadataMapper.updateById(file);

            log.info("File: {} restored to version: {}", fileId, versionId);

            return FileResponse.builder()
                    .success(true)
                    .message("Version restored successfully")
                    .data(Map.of(
                            "fileId", fileId,
                            "versionId", versionId,
                            "versionNumber", version.getVersionNumber(),
                            "restoredAt", LocalDateTime.now()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to restore version: {} for file: {}", versionId, fileId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to restore version: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 删除版本
     */
    @Transactional
    public FileResponse deleteVersion(String versionId) {
        log.info("Deleting version: {}", versionId);

        try {
            FileVersion version = fileVersionMapper.selectByVersionId(versionId);
            if (version == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Version not found: " + versionId)
                        .build();
            }

            // 删除版本文件
            Path versionPath = Paths.get(version.getStoragePath());
            Files.deleteIfExists(versionPath);

            // 删除版本记录
            fileVersionMapper.deleteById(version.getId());

            log.info("Version deleted: {}", versionId);

            return FileResponse.builder()
                    .success(true)
                    .message("Version deleted successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to delete version: {}", versionId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to delete version: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取版本详情
     */
    public FileResponse getVersionDetail(String versionId) {
        log.info("Getting version detail: {}", versionId);

        try {
            FileVersion version = fileVersionMapper.selectByVersionId(versionId);
            if (version == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Version not found: " + versionId)
                        .build();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("versionId", version.getVersionId());
            data.put("versionNumber", version.getVersionNumber());
            data.put("fileId", version.getFileId());
            data.put("storagePath", version.getStoragePath());
            data.put("fileSize", version.getFileSize());
            data.put("fileHash", version.getFileHash());
            data.put("description", version.getDescription());
            data.put("tags", version.getTagList());
            data.put("createdAt", version.getCreatedAt());

            return FileResponse.builder()
                    .success(true)
                    .message("Version detail retrieved")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get version detail: {}", versionId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get version detail: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 清理旧版本（保留最近的N个）
     */
    private void cleanupOldVersions(String fileId) {
        try {
            Long versionCount = fileVersionMapper.countByFileId(fileId);
            if (versionCount != null && versionCount > maxVersionCount) {
                int deleteCount = fileVersionMapper.deleteOldVersions(fileId, maxVersionCount);
                log.info("Cleaned up {} old versions for file: {}", deleteCount, fileId);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup old versions for file: {}", fileId, e);
        }
    }

    /**
     * 生成版本ID
     */
    private String generateVersionId() {
        return "v-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成版本存储路径
     */
    private String generateVersionPath(String fileId, int versionNumber, String extension) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("%s/%s/%s_v%d.%s", versionStoragePath, datePath, fileId, versionNumber, extension);
    }
}