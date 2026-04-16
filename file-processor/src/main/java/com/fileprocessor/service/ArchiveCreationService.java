package com.fileprocessor.service;

import com.fileprocessor.dto.BatchDownloadRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.mapper.FileMetadataMapper;
import net.sf.sevenzipjbinding.IOutCreateArchiveZip;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 压缩包创建服务
 */
@Service
public class ArchiveCreationService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveCreationService.class);

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProgressNotifier progressNotifier;

    /**
     * 创建压缩包（ZIP格式）
     */
    public FileResponse createZipArchive(BatchDownloadRequest request) {
        String taskId = generateTaskId();
        log.info("Creating ZIP archive: {}, files: {}", taskId, request.getFileIds().size());

        try {
            // 验证文件
            List<FileMetadata> files = validateFiles(request.getFileIds());
            if (files.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("No valid files found")
                        .build();
            }

            // 生成输出路径
            String archiveName = request.getArchiveName() != null ?
                    request.getArchiveName() : "archive_" + taskId + ".zip";
            if (!archiveName.endsWith(".zip")) {
                archiveName += ".zip";
            }

            String outputPath = generateArchivePath(taskId, archiveName);
            Path targetPath = Paths.get(outputPath);
            Files.createDirectories(targetPath.getParent());

            // 创建ZIP
            long totalSize = 0;
            int fileCount = 0;
            int compressionLevel = request.getCompressionLevel() != null ?
                    request.getCompressionLevel() : 6;

            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                zos.setLevel(compressionLevel);

                for (FileMetadata file : files) {
                    Path filePath = Paths.get(file.getStoragePath());
                    if (!Files.exists(filePath)) {
                        log.warn("File not found: {}", file.getStoragePath());
                        continue;
                    }

                    // 添加文件到ZIP
                    ZipEntry entry = new ZipEntry(file.getOriginalName());
                    zos.putNextEntry(entry);

                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                        }
                    }

                    zos.closeEntry();
                    totalSize += file.getFileSize();
                    fileCount++;

                    log.debug("Added to archive: {}", file.getOriginalName());
                }
            }

            long archiveSize = Files.size(targetPath);
            double compressionRatio = totalSize > 0 ? (double) archiveSize / totalSize : 1.0;

            log.info("Archive created: {}, files: {}, size: {} bytes",
                    outputPath, fileCount, archiveSize);

            return FileResponse.builder()
                    .success(true)
                    .message("Archive created successfully")
                    .fileId(taskId)
                    .filePath(outputPath)
                    .fileSize(archiveSize)
                    .data(Map.of(
                            "archiveId", taskId,
                            "archiveName", archiveName,
                            "totalFiles", fileCount,
                            "originalSize", totalSize,
                            "compressedSize", archiveSize,
                            "compressionRatio", String.format("%.2f%%", compressionRatio * 100),
                            "downloadUrl", "/api/files/archive/download/" + taskId
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create archive", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Archive creation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 异步创建压缩包（用于大文件）
     */
    @Async("taskExecutor")
    public void createZipArchiveAsync(String taskId, BatchDownloadRequest request) {
        log.info("Async archive creation started: {}", taskId);

        // 更新任务状态为处理中
        taskService.updateTaskProgress(taskId, 10, "正在准备文件...");

        try {
            List<FileMetadata> files = validateFiles(request.getFileIds());
            int totalFiles = files.size();

            String archiveName = request.getArchiveName() != null ?
                    request.getArchiveName() : "archive_" + taskId + ".zip";
            String outputPath = generateArchivePath(taskId, archiveName);

            Path targetPath = Paths.get(outputPath);
            Files.createDirectories(targetPath.getParent());

            long totalSize = 0;
            int processedFiles = 0;

            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                zos.setLevel(request.getCompressionLevel() != null ?
                        request.getCompressionLevel() : 6);

                for (FileMetadata file : files) {
                    Path filePath = Paths.get(file.getStoragePath());
                    if (!Files.exists(filePath)) {
                        continue;
                    }

                    // 添加文件
                    ZipEntry entry = new ZipEntry(file.getOriginalName());
                    zos.putNextEntry(entry);

                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                        }
                    }

                    zos.closeEntry();
                    totalSize += file.getFileSize();
                    processedFiles++;

                    // 更新进度
                    int progress = (int) ((processedFiles * 100.0) / totalFiles);
                    taskService.updateTaskProgress(taskId, progress,
                            String.format("正在压缩 %d/%d: %s", processedFiles, totalFiles, file.getOriginalName()));

                    // 发送WebSocket通知
                    progressNotifier.notifyArchiveProgress("1", taskId, progress, processedFiles, totalFiles, file.getOriginalName());
                }
            }

            long archiveSize = Files.size(targetPath);

            // 完成任务
            String downloadUrl = "/api/files/archive/download/" + taskId;
            taskService.completeTask(taskId, Map.of(
                    "archiveId", taskId,
                    "archiveName", archiveName,
                    "filePath", outputPath,
                    "fileSize", archiveSize,
                    "totalFiles", processedFiles,
                    "downloadUrl", downloadUrl
            ));

            // 发送完成通知
            progressNotifier.notifyArchiveComplete("1", taskId, downloadUrl);

            log.info("Async archive creation completed: {}", taskId);

        } catch (Exception e) {
            log.error("Async archive creation failed: {}", taskId, e);
            taskService.failTask(taskId, e.getMessage());
        }
    }

    /**
     * 提交批量下载任务
     */
    public FileResponse submitBatchDownload(BatchDownloadRequest request) {
        // 创建任务记录
        String taskId = taskService.submitTask("BATCH_DOWNLOAD", "批量打包下载",
                request.getFileIds().size(), Map.of(
                        "fileIds", request.getFileIds(),
                        "archiveName", request.getArchiveName(),
                        "archiveFormat", request.getArchiveFormat()
                ));

        // 异步执行
        createZipArchiveAsync(taskId, request);

        return FileResponse.builder()
                .success(true)
                .message("Batch download task submitted")
                .fileId(taskId)
                .data(Map.of(
                        "taskId", taskId,
                        "status", "PROCESSING",
                        "totalFiles", request.getFileIds().size()
                ))
                .build();
    }

    /**
     * 验证文件列表
     */
    private List<FileMetadata> validateFiles(List<String> fileIds) {
        return fileIds.stream()
                .map(fileMetadataMapper::findByFileId)
                .filter(file -> file != null && file.getStatus() == 1)
                .toList();
    }

    private String generateTaskId() {
        return "arch-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String generateArchivePath(String taskId, String archiveName) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("archives/%s/%s", datePath, archiveName);
    }
}
