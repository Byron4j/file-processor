package com.fileprocessor.controller;

import com.fileprocessor.dto.*;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.service.ArchiveCreationService;
import com.fileprocessor.service.FileTransferService;
import com.fileprocessor.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 文件传输控制器 - 上传、下载、分片上传
 */
@RestController
@RequestMapping("/api/files")
public class FileTransferController {

    private static final Logger log = LoggerFactory.getLogger(FileTransferController.class);

    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private ArchiveCreationService archiveCreationService;

    @Autowired
    private TaskService taskService;

    // ==================== 简单上传 ====================

    /**
     * 单文件上传
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute UploadRequest request) {
        log.info("Upload request: {}, size: {}", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("File is empty")
                            .build()
            );
        }

        FileResponse response = fileTransferService.upload(file, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 多文件上传
     */
    @PostMapping("/upload/batch")
    public ResponseEntity<FileResponse> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @ModelAttribute UploadRequest request) {
        log.info("Batch upload request: {} files", files.size());

        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("No files provided")
                            .build()
            );
        }

        // 逐个上传
        int successCount = 0;
        int failCount = 0;
        for (MultipartFile file : files) {
            FileResponse resp = fileTransferService.upload(file, request);
            if (resp.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        return ResponseEntity.ok(
                FileResponse.builder()
                        .success(true)
                        .message("Batch upload complete")
                        .data(java.util.Map.of(
                                "total", files.size(),
                                "success", successCount,
                                "failed", failCount
                        ))
                        .build()
        );
    }

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     */
    @PostMapping("/upload/init")
    public ResponseEntity<ChunkUploadResponse> initChunkUpload(
            @RequestBody ChunkUploadRequest request) {
        log.info("Init chunk upload: {}", request.getFileName());

        ChunkUploadResponse response = fileTransferService.initChunkUpload(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 上传分片
     */
    @PostMapping("/upload/chunk/{uploadId}/{chunkNumber}")
    public ResponseEntity<FileResponse> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable int chunkNumber,
            @RequestParam("chunk") MultipartFile chunk) {
        log.info("Upload chunk: {} for upload: {}", chunkNumber, uploadId);

        FileResponse response = fileTransferService.uploadChunk(uploadId, chunkNumber, chunk);
        return ResponseEntity.ok(response);
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/upload/complete/{uploadId}")
    public ResponseEntity<FileResponse> completeChunkUpload(
            @PathVariable String uploadId) {
        log.info("Complete chunk upload: {}", uploadId);

        FileResponse response = fileTransferService.completeChunkUpload(uploadId);
        return ResponseEntity.ok(response);
    }

    // ==================== 秒传检查 ====================

    /**
     * 秒传检查
     */
    @PostMapping("/upload/check")
    public ResponseEntity<FileResponse> fastUploadCheck(
            @RequestBody FastUploadCheckRequest request) {
        log.info("Fast upload check: {}", request.getFileHash());

        FileResponse response = fileTransferService.fastUploadCheck(
                request.getFileHash(), request.getFileSize());
        return ResponseEntity.ok(response);
    }

    // ==================== 文件下载 ====================

    /**
     * 文件下载（支持断点续传）
     */
    @GetMapping("/download/{fileId}")
    public void download(
            @PathVariable String fileId,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Download request: {}", fileId);

        // 获取文件信息
        FileMetadata metadata = fileTransferService.getFileMetadata(fileId);
        if (metadata == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path filePath = Paths.get(metadata.getStoragePath());
        if (!Files.exists(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            long fileSize = Files.size(filePath);
            String rangeHeader = request.getHeader("Range");

            // 设置响应头
            response.setContentType(metadata.getMimeType() != null ?
                    metadata.getMimeType() : "application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + metadata.getOriginalName() + "\"");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(fileSize));

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                // 处理断点续传
                handlePartialDownload(filePath, rangeHeader, response, fileSize);
            } else {
                // 完整下载
                try (InputStream is = Files.newInputStream(filePath);
                     OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
            }

            // 更新访问统计
            fileTransferService.updateAccessStats(fileId);

        } catch (IOException e) {
            log.error("Download failed: {}", fileId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理断点续传
     */
    private void handlePartialDownload(Path filePath, String rangeHeader,
                                       HttpServletResponse response, long fileSize) throws IOException {
        // 解析 Range 头
        String range = rangeHeader.substring(6); // 去掉 "bytes="
        String[] parts = range.split("-");

        long start = Long.parseLong(parts[0]);
        long end = parts.length > 1 && !parts[1].isEmpty()
                ? Long.parseLong(parts[1])
                : fileSize - 1;

        long contentLength = end - start + 1;

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range",
                String.format("bytes %d-%d/%d", start, end, fileSize));
        response.setHeader("Content-Length", String.valueOf(contentLength));

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
             OutputStream os = response.getOutputStream()) {

            raf.seek(start);

            byte[] buffer = new byte[8192];
            long remaining = contentLength;

            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                os.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    // ==================== 批量打包下载 ====================

    /**
     * 批量打包下载（同步，适合小文件）
     */
    @PostMapping("/download/batch")
    public ResponseEntity<FileResponse> batchDownload(@RequestBody BatchDownloadRequest request) {
        log.info("Batch download request: {} files", request.getFileIds() != null ? request.getFileIds().size() : 0);

        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("No file IDs provided")
                            .build()
            );
        }

        FileResponse response = archiveCreationService.createZipArchive(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 批量打包下载（异步，适合大文件）
     */
    @PostMapping("/download/batch/async")
    public ResponseEntity<FileResponse> batchDownloadAsync(@RequestBody BatchDownloadRequest request) {
        log.info("Async batch download request: {} files", request.getFileIds() != null ? request.getFileIds().size() : 0);

        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("No file IDs provided")
                            .build()
            );
        }

        FileResponse response = archiveCreationService.submitBatchDownload(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 下载打包好的压缩包
     */
    @GetMapping("/archive/download/{archiveId}")
    public void downloadArchive(@PathVariable String archiveId, HttpServletResponse response) {
        log.info("Download archive: {}", archiveId);

        // 从任务结果获取归档信息
        Map<String, Object> taskResult = taskService.getTaskResult(archiveId);
        if (taskResult == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filePath = (String) taskResult.get("filePath");
        String archiveName = (String) taskResult.get("archiveName");

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + archiveName + "\"");
            response.setHeader("Content-Length", String.valueOf(Files.size(path)));

            try (InputStream is = Files.newInputStream(path);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            log.error("Failed to download archive: {}", archiveId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== Request/Response DTOs ====================

    public static class FastUploadCheckRequest {
        private String fileHash;
        private Long fileSize;

        public String getFileHash() {
            return fileHash;
        }

        public void setFileHash(String fileHash) {
            this.fileHash = fileHash;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }
}