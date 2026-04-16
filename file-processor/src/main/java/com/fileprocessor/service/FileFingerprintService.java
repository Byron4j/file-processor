package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileFingerprint;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.mapper.FileFingerprintMapper;
import com.fileprocessor.mapper.FileMetadataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 文件指纹服务 - 支持秒传功能
 */
@Service
public class FileFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(FileFingerprintService.class);

    @Autowired
    private FileFingerprintMapper fingerprintMapper;

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    /**
     * 检查文件是否存在（秒传检查）
     */
    public FileResponse checkFileExists(String md5, Long fileSize) {
        Optional<FileFingerprint> existing = fingerprintMapper.findByMd5AndSize(md5, fileSize);

        if (existing.isPresent()) {
            FileFingerprint fingerprint = existing.get();
            File file = new File(fingerprint.getStoragePath());

            if (file.exists()) {
                // Update access time
                fingerprint.setLastAccessedAt(LocalDateTime.now());
                fingerprintMapper.updateById(fingerprint);

                return FileResponse.builder()
                        .success(true)
                        .message("File exists - instant transfer available")
                        .data(java.util.Map.of(
                                "exists", true,
                                "fingerprintId", fingerprint.getId(),
                                "storagePath", fingerprint.getStoragePath()
                        ))
                        .build();
            } else {
                // File missing but fingerprint exists - cleanup
                fingerprintMapper.deleteById(fingerprint.getId());
            }
        }

        return FileResponse.builder()
                .success(true)
                .message("File not found - upload required")
                .data(java.util.Map.of("exists", false))
                .build();
    }

    /**
     * 执行秒传
     */
    @Transactional
    public FileResponse instantTransfer(String md5, Long fileSize, Long userId, String originalFilename) {
        Optional<FileFingerprint> existing = fingerprintMapper.findByMd5AndSize(md5, fileSize);

        if (existing.isEmpty()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File fingerprint not found")
                    .build();
        }

        FileFingerprint fingerprint = existing.get();

        // Increment reference count
        fingerprintMapper.incrementReferenceCount(fingerprint.getId());

        // Create file metadata reference
        FileMetadata metadata = new FileMetadata();
        metadata.setUserId(userId);
        metadata.setOriginalName(originalFilename);
        metadata.setFileSize(fileSize);
        metadata.setStoragePath(fingerprint.getStoragePath());
        metadata.setFingerprintId(fingerprint.getId());
        metadata.setMd5Hash(fingerprint.getMd5Hash());
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setUpdatedAt(LocalDateTime.now());

        fileMetadataMapper.insert(metadata);

        log.info("Instant transfer completed: {} -> {}", originalFilename, fingerprint.getStoragePath());

        return FileResponse.builder()
                .success(true)
                .message("Instant transfer successful")
                .filePath(fingerprint.getStoragePath())
                .fileSize(fileSize)
                .data(java.util.Map.of(
                        "fileId", metadata.getId(),
                        "isInstant", true
                ))
                .build();
    }

    /**
     * 注册新文件指纹
     */
    @Transactional
    public FileFingerprint registerFingerprint(String filePath, Long fileSize) {
        try {
            String md5 = calculateMd5(new File(filePath));
            String sha256 = calculateSha256(new File(filePath));

            // Check if already exists
            Optional<FileFingerprint> existing = fingerprintMapper.findByMd5AndSize(md5, fileSize);
            if (existing.isPresent()) {
                fingerprintMapper.incrementReferenceCount(existing.get().getId());
                return existing.get();
            }

            // Create new fingerprint
            FileFingerprint fingerprint = new FileFingerprint();
            fingerprint.setMd5Hash(md5);
            fingerprint.setSha256Hash(sha256);
            fingerprint.setFileSize(fileSize);
            fingerprint.setStoragePath(filePath);
            fingerprint.setReferenceCount(1);
            fingerprint.setCreatedAt(LocalDateTime.now());
            fingerprint.setLastAccessedAt(LocalDateTime.now());

            fingerprintMapper.insert(fingerprint);

            log.info("New file fingerprint registered: md5={}, size={}", md5, fileSize);

            return fingerprint;
        } catch (Exception e) {
            log.error("Failed to register fingerprint for: {}", filePath, e);
            return null;
        }
    }

    /**
     * 删除文件引用
     */
    @Transactional
    public void releaseFileReference(Long fingerprintId) {
        fingerprintMapper.decrementReferenceCount(fingerprintId);

        // Check if reference count is 0
        FileFingerprint fingerprint = fingerprintMapper.selectById(fingerprintId);
        if (fingerprint != null && fingerprint.getReferenceCount() <= 0) {
            // Delete actual file
            File file = new File(fingerprint.getStoragePath());
            if (file.exists()) {
                file.delete();
            }
            // Delete fingerprint record
            fingerprintMapper.deleteById(fingerprintId);
            log.info("File deleted due to zero references: {}", fingerprint.getStoragePath());
        }
    }

    /**
     * 计算MD5
     */
    public String calculateMd5(File file) throws IOException {
        return calculateHash(file, "MD5");
    }

    /**
     * 计算SHA256
     */
    public String calculateSha256(File file) throws IOException {
        return calculateHash(file, "SHA-256");
    }

    private String calculateHash(File file, String algorithm) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            throw new IOException("Failed to calculate " + algorithm, e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
