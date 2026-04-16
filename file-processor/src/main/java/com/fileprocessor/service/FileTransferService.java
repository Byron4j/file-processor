package com.fileprocessor.service;

import com.fileprocessor.dto.*;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.entity.FileUploadSession;
import com.fileprocessor.mapper.FileMetadataMapper;
import com.fileprocessor.mapper.FileUploadSessionMapper;
import com.fileprocessor.service.storage.StorageManager;
import com.fileprocessor.service.storage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 文件传输服务 - 处理上传、下载、分片、秒传
 */
@Service
public class FileTransferService {

    private static final Logger log = LoggerFactory.getLogger(FileTransferService.class);

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private FileUploadSessionMapper uploadSessionMapper;

    @Autowired
    private StorageManager storageManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private ProgressNotifier progressNotifier;

    private static final String UPLOAD_SESSION_PREFIX = "upload:session:";

    // User ID for anonymous users (should be replaced with actual user context)
    private static final Long DEFAULT_USER_ID = 1L;
    private static final long SESSION_EXPIRE_HOURS = 24;
    private static final long CHUNK_EXPIRE_HOURS = 48;

    // ==================== 简单上传 ====================

    /**
     * 简单文件上传
     */
    @Transactional
    public FileResponse upload(MultipartFile file, UploadRequest request) {
        log.info("Uploading file: {}, size: {}", file.getOriginalFilename(), file.getSize());

        try {
            // 1. 检查用户配额
            Long userId = getCurrentUserId(request);
            QuotaService.QuotaCheckResult quotaCheck = quotaService.checkUploadQuota(userId, file.getSize());
            if (!quotaCheck.isAllowed()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Quota exceeded: " + quotaCheck.getMessage())
                        .build();
            }

            // 2. 计算文件哈希
            String fileHash = calculateHash(file.getInputStream(), "SHA-256");

            // 2. 秒传检查
            FileMetadata existingFile = findByHash(fileHash, file.getSize());
            if (existingFile != null) {
                log.info("Fast upload detected for hash: {}", fileHash);
                String newFileId = generateFileId();
                createFileReference(existingFile, newFileId, request);
                return buildUploadResponse(newFileId, existingFile, true);
            }

            // 3. 保存文件
            String fileId = generateFileId();
            String extension = getExtension(file.getOriginalFilename());
            String storagePath = generateStoragePath(fileId, extension);

            Path targetPath = Paths.get(storagePath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);

            // 4. 保存元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setOriginalName(file.getOriginalFilename());
            metadata.setStorageType(StorageType.LOCAL.name());
            metadata.setStoragePath(storagePath);
            metadata.setFileSize(file.getSize());
            metadata.setMimeType(file.getContentType());
            metadata.setExtension(extension);
            metadata.setFileHash(fileHash);
            metadata.setHashAlgorithm("SHA-256");
            metadata.setCategoryId(request.getCategoryId());
            metadata.setTags(request.getTags());
            metadata.setDescription(request.getDescription());
            metadata.setStatus(1);
            metadata.setIsDeleted(0);
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setUpdatedAt(LocalDateTime.now());

            fileMetadataMapper.insert(metadata);

            // 5. 更新配额
            quotaService.recordUploadComplete(userId, file.getSize());

            // 6. 病毒扫描（异步）
            // virusScanService.scanAsync(storagePath);

            return buildUploadResponse(fileId, metadata, false);

        } catch (Exception e) {
            log.error("Failed to upload file", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     */
    public ChunkUploadResponse initChunkUpload(ChunkUploadRequest request) {
        log.info("Initializing chunk upload: {}, size: {}", request.getFileName(), request.getFileSize());

        Long userId = getCurrentUserId(request);

        // 0. 检查配额
        QuotaService.QuotaCheckResult quotaCheck = quotaService.checkUploadQuota(userId, request.getFileSize());
        if (!quotaCheck.isAllowed()) {
            return ChunkUploadResponse.builder()
                    .success(false)
                    .message("Quota exceeded: " + quotaCheck.getMessage())
                    .build();
        }

        // 1. 秒传检查
        if (request.getFileHash() != null && !request.getFileHash().isEmpty()) {
            FileMetadata existingFile = findByHash(request.getFileHash(), request.getFileSize());
            if (existingFile != null) {
                log.info("Fast upload for chunk upload: {}", request.getFileHash());
                String newFileId = generateFileId();
                createFileReference(existingFile, newFileId, null);

                return ChunkUploadResponse.builder()
                        .uploadId(null)
                        .totalChunks(0)
                        .chunkSize(0)
                        .uploadedChunks(Collections.emptyList())
                        .expireAt(LocalDateTime.now())
                        .fastUpload(true)
                        .fileId(newFileId)
                        .build();
            }
        }

        // 2. 计算分片信息
        int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 5 * 1024 * 1024;
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);

        // 3. 创建上传会话
        String uploadId = generateUploadId();
        FileUploadSession session = new FileUploadSession();
        session.setUploadId(uploadId);
        session.setFileName(request.getFileName());
        session.setFileSize(request.getFileSize());
        session.setFileHash(request.getFileHash());
        session.setChunkSize(chunkSize);
        session.setTotalChunks(totalChunks);
        session.setUploadedChunkList(new ArrayList<>());
        session.setStatus("PENDING");
        session.setExpireAt(LocalDateTime.now().plusHours(SESSION_EXPIRE_HOURS));
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        uploadSessionMapper.insert(session);

        // 4. 缓存到Redis
        cacheUploadSession(session);

        return ChunkUploadResponse.builder()
                .uploadId(uploadId)
                .totalChunks(totalChunks)
                .chunkSize(chunkSize)
                .uploadedChunks(Collections.emptyList())
                .expireAt(session.getExpireAt())
                .fastUpload(false)
                .build();
    }

    /**
     * 上传分片
     */
    @Transactional
    public FileResponse uploadChunk(String uploadId, int chunkNumber, MultipartFile chunk) {
        log.info("Uploading chunk: {} for upload: {}", chunkNumber, uploadId);

        try {
            // 1. 获取上传会话
            FileUploadSession session = getUploadSession(uploadId);
            if (session == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Upload session not found or expired")
                        .build();
            }

            // 2. 验证分片号
            if (chunkNumber < 0 || chunkNumber >= session.getTotalChunks()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Invalid chunk number")
                        .build();
            }

            // 3. 检查是否已上传
            if (session.isChunkUploaded(chunkNumber)) {
                return FileResponse.builder()
                        .success(true)
                        .message("Chunk already uploaded")
                        .data(Map.of("progress", session.getProgress()))
                        .build();
            }

            // 4. 保存分片
            String chunkDir = getChunkDir(uploadId);
            Path chunkPath = Paths.get(chunkDir, String.valueOf(chunkNumber));
            Files.createDirectories(chunkPath.getParent());
            chunk.transferTo(chunkPath);

            // 5. 更新会话
            session.addUploadedChunk(chunkNumber);
            session.setStatus("UPLOADING");
            session.setUpdatedAt(LocalDateTime.now());

            uploadSessionMapper.updateById(session);
            cacheUploadSession(session);

            // 6. 发送WebSocket进度通知
            Long userId = getCurrentUserId(null);
            progressNotifier.notifyChunkProgress(String.valueOf(userId), uploadId, chunkNumber + 1, session.getTotalChunks());

            // 7. 检查是否完成
            if (session.isAllChunksUploaded()) {
                return completeChunkUpload(uploadId);
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Chunk uploaded")
                    .data(Map.of(
                            "chunkNumber", chunkNumber,
                            "progress", session.getProgress(),
                            "uploadedChunks", session.getUploadedChunks()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload chunk", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Chunk upload failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 完成分片上传
     */
    @Transactional
    public FileResponse completeChunkUpload(String uploadId) {
        log.info("Completing chunk upload: {}", uploadId);

        try {
            FileUploadSession session = getUploadSession(uploadId);
            if (session == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Upload session not found")
                        .build();
            }

            if (!session.isAllChunksUploaded()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Not all chunks uploaded")
                        .data(Map.of(
                                "totalChunks", session.getTotalChunks(),
                                "uploadedChunks", session.getUploadedChunkList().size()
                        ))
                        .build();
            }

            // 1. 合并分片
            String fileId = generateFileId();
            String extension = getExtension(session.getFileName());
            String storagePath = generateStoragePath(fileId, extension);

            mergeChunks(session, storagePath);

            // 2. 计算合并后文件的哈希
            String fileHash = calculateHash(Files.newInputStream(Paths.get(storagePath)), "SHA-256");

            // 3. 检查秒传（防止并发上传）
            FileMetadata existingFile = findByHash(fileHash, session.getFileSize());
            if (existingFile != null) {
                // 删除已合并的文件，使用已有文件
                Files.deleteIfExists(Paths.get(storagePath));
                createFileReference(existingFile, fileId, null);
                session.setTargetFileId(fileId);
                session.setStatus("COMPLETED");
                uploadSessionMapper.updateById(session);
                clearChunkDir(uploadId);

                return buildUploadResponse(fileId, existingFile, true);
            }

            // 4. 保存元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setOriginalName(session.getFileName());
            metadata.setStorageType(StorageType.LOCAL.name());
            metadata.setStoragePath(storagePath);
            metadata.setFileSize(session.getFileSize());
            metadata.setExtension(extension);
            metadata.setFileHash(fileHash);
            metadata.setHashAlgorithm("SHA-256");
            metadata.setStatus(1);
            metadata.setIsDeleted(0);
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setUpdatedAt(LocalDateTime.now());

            fileMetadataMapper.insert(metadata);

            // 5. 更新配额
            quotaService.recordUploadComplete(DEFAULT_USER_ID, session.getFileSize());

            // 6. 发送完成通知
            progressNotifier.notifyUploadComplete(String.valueOf(DEFAULT_USER_ID), uploadId, fileId, session.getFileSize());

            // 7. 更新会话
            session.setTargetFileId(fileId);
            session.setStoragePath(storagePath);
            session.setStatus("COMPLETED");
            session.setUpdatedAt(LocalDateTime.now());
            uploadSessionMapper.updateById(session);

            // 8. 清理分片
            clearChunkDir(uploadId);
            redisTemplate.delete(UPLOAD_SESSION_PREFIX + uploadId);

            return buildUploadResponse(fileId, metadata, false);

        } catch (Exception e) {
            log.error("Failed to complete chunk upload", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Complete upload failed: " + e.getMessage())
                    .build();
        }
    }

    // ==================== 秒传检查 ====================

    /**
     * 秒传检查
     */
    public FileResponse fastUploadCheck(String fileHash, Long fileSize) {
        FileMetadata existingFile = findByHash(fileHash, fileSize);

        if (existingFile != null && storageManager.exists(existingFile.getStoragePath())) {
            return FileResponse.builder()
                    .success(true)
                    .message("File exists, fast upload available")
                    .data(Map.of(
                            "exists", true,
                            "fileId", existingFile.getFileId(),
                            "fileName", existingFile.getOriginalName(),
                            "fileSize", existingFile.getFileSize()
                    ))
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("File not exists, need upload")
                .data(Map.of("exists", false))
                .build();
    }

    // ==================== 辅助方法 ====================

    private FileMetadata findByHash(String fileHash, Long fileSize) {
        if (fileHash == null || fileHash.isEmpty()) {
            return null;
        }
        // 使用 MyBatis Plus 查询
        return fileMetadataMapper.selectByHash(fileHash, fileSize);
    }

    private void createFileReference(FileMetadata existingFile, String newFileId, UploadRequest request) {
        FileMetadata newFile = new FileMetadata();
        newFile.setFileId(newFileId);
        newFile.setOriginalName(existingFile.getOriginalName());
        newFile.setStorageType(existingFile.getStorageType());
        newFile.setStoragePath(existingFile.getStoragePath());
        newFile.setFileSize(existingFile.getFileSize());
        newFile.setMimeType(existingFile.getMimeType());
        newFile.setExtension(existingFile.getExtension());
        newFile.setFileHash(existingFile.getFileHash());
        newFile.setHashAlgorithm(existingFile.getHashAlgorithm());
        newFile.setCategoryId(request != null ? request.getCategoryId() : null);
        newFile.setTags(request != null ? request.getTags() : null);
        newFile.setDescription(request != null ? request.getDescription() : null);
        newFile.setStatus(1);
        newFile.setIsDeleted(0);
        newFile.setCreatedAt(LocalDateTime.now());
        newFile.setUpdatedAt(LocalDateTime.now());

        fileMetadataMapper.insert(newFile);

        // 增加引用计数
        fileMetadataMapper.incrementReferenceCount(existingFile.getFileId());
    }

    private void mergeChunks(FileUploadSession session, String targetPath) throws IOException {
        String chunkDir = getChunkDir(session.getUploadId());
        Path target = Paths.get(targetPath);
        Files.createDirectories(target.getParent());

        try (var outputStream = Files.newOutputStream(target)) {
            for (int i = 0; i < session.getTotalChunks(); i++) {
                Path chunkPath = Paths.get(chunkDir, String.valueOf(i));
                if (!Files.exists(chunkPath)) {
                    throw new IOException("Missing chunk: " + i);
                }
                Files.copy(chunkPath, outputStream);
            }
        }
    }

    private void clearChunkDir(String uploadId) {
        try {
            String chunkDir = getChunkDir(uploadId);
            Path dir = Paths.get(chunkDir);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("Failed to delete: {}", p);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to clear chunk dir: {}", uploadId, e);
        }
    }

    private FileUploadSession getUploadSession(String uploadId) {
        // 先查Redis
        String cached = redisTemplate.opsForValue().get(UPLOAD_SESSION_PREFIX + uploadId);
        if (cached != null) {
            // 可以反序列化，简化处理直接查DB
        }

        // 查数据库
        return uploadSessionMapper.selectByUploadId(uploadId);
    }

    private void cacheUploadSession(FileUploadSession session) {
        try {
            redisTemplate.opsForValue().set(
                    UPLOAD_SESSION_PREFIX + session.getUploadId(),
                    "1",
                    CHUNK_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Failed to cache upload session", e);
        }
    }

    private String calculateHash(InputStream inputStream, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String generateFileId() {
        return "f-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateUploadId() {
        return "u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String generateStoragePath(String fileId, String extension) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("uploads/%s/%s.%s", datePath, fileId, extension);
    }

    private String getChunkDir(String uploadId) {
        return String.format("temp/chunks/%s", uploadId);
    }

    private FileResponse buildUploadResponse(String fileId, FileMetadata metadata, boolean isFastUpload) {
        return FileResponse.builder()
                .success(true)
                .message(isFastUpload ? "Fast upload success" : "Upload success")
                .fileId(fileId)
                .filePath(metadata.getStoragePath())
                .fileSize(metadata.getFileSize())
                .data(Map.of(
                        "fileId", fileId,
                        "fileName", metadata.getOriginalName(),
                        "fileSize", metadata.getFileSize(),
                        "mimeType", metadata.getMimeType(),
                        "extension", metadata.getExtension(),
                        "hash", metadata.getFileHash(),
                        "isFastUpload", isFastUpload,
                        "createdAt", metadata.getCreatedAt()
                ))
                .build();
    }

    /**
     * 获取文件元数据（供下载使用）
     */
    public FileMetadata getFileMetadata(String fileId) {
        return fileMetadataMapper.findByFileId(fileId);
    }

    /**
     * 更新访问统计
     */
    public void updateAccessStats(String fileId) {
        try {
            fileMetadataMapper.incrementAccessCount(fileId);
        } catch (Exception e) {
            log.warn("Failed to update access stats: {}", fileId, e);
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Object request) {
        // TODO: 从Spring Security上下文获取实际用户ID
        // 目前从请求中获取或使用默认值
        if (request instanceof UploadRequest && ((UploadRequest) request).getUserId() != null) {
            return ((UploadRequest) request).getUserId();
        }
        if (request instanceof ChunkUploadRequest && ((ChunkUploadRequest) request).getUserId() != null) {
            return ((ChunkUploadRequest) request).getUserId();
        }
        return DEFAULT_USER_ID;
    }
}
