package com.fileprocessor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileShare;
import com.fileprocessor.mapper.FileShareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class FileShareService {

    private static final Logger log = LoggerFactory.getLogger(FileShareService.class);

    @Autowired
    private FileShareMapper fileShareMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 创建文件分享
     */
    public FileResponse createShare(String fileId, Long userId, String password,
                                     Integer expireHours, Integer maxDownloads, Boolean allowPreview) {
        try {
            String shareId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String shareToken = UUID.randomUUID().toString().replace("-", "");

            FileShare share = new FileShare();
            share.setShareId(shareId);
            share.setFileId(fileId);
            share.setCreatedBy(String.valueOf(userId));
            share.setShareToken(shareToken);
            share.setDownloadCount(0);
            share.setStatus(1);
            share.setCreatedAt(LocalDateTime.now());

            if (password != null && !password.isEmpty()) {
                share.setPasswordHash(passwordEncoder.encode(password));
            }

            if (expireHours != null && expireHours > 0) {
                share.setExpireAt(LocalDateTime.now().plusHours(expireHours));
            }

            if (maxDownloads != null && maxDownloads > 0) {
                share.setMaxDownloads(maxDownloads);
            }

            share.setAllowPreview(allowPreview != null ? allowPreview : true);

            fileShareMapper.insert(share);

            return FileResponse.builder()
                    .success(true)
                    .message("Share created successfully")
                    .data(Map.of(
                            "shareId", shareId,
                            "shareToken", shareToken,
                            "shareUrl", "/api/share/" + shareId + "?token=" + shareToken,
                            "expireAt", share.getExpireAt() != null ? share.getExpireAt().toString() : "never",
                            "passwordProtected", share.getPasswordHash() != null
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create share", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to create share: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 验证分享访问
     */
    public FileResponse validateShare(String shareId, String token, String password) {
        try {
            FileShare share = fileShareMapper.selectOne(
                    new LambdaQueryWrapper<FileShare>().eq(FileShare::getShareId, shareId)
            );

            if (share == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Share not found")
                        .build();
            }

            // Check status
            if (share.getStatus() == null || share.getStatus() != 1) {
                return FileResponse.builder()
                        .success(false)
                        .message("Share is invalid")
                        .build();
            }

            // Check expiration
            if (share.getExpireAt() != null && share.getExpireAt().isBefore(LocalDateTime.now())) {
                return FileResponse.builder()
                        .success(false)
                        .message("Share has expired")
                        .build();
            }

            // Check download limit
            if (share.getMaxDownloads() != null && share.getMaxDownloads() > 0) {
                if (share.getDownloadCount() != null && share.getDownloadCount() >= share.getMaxDownloads()) {
                    return FileResponse.builder()
                            .success(false)
                            .message("Download limit reached")
                            .build();
                }
            }

            // Check token
            if (!share.getShareToken().equals(token)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Invalid share token")
                        .build();
                        }

            // Check password
            if (share.getPasswordHash() != null) {
                if (password == null || !passwordEncoder.matches(password, share.getPasswordHash())) {
                    return FileResponse.builder()
                            .success(false)
                            .message("Invalid password")
                            .build();
                }
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Share validated")
                    .data(Map.of(
                            "fileId", share.getFileId(),
                            "allowPreview", share.getAllowPreview()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to validate share", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to validate share: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 记录下载
     */
    public void recordDownload(String shareId) {
        try {
            FileShare share = fileShareMapper.selectOne(
                    new LambdaQueryWrapper<FileShare>().eq(FileShare::getShareId, shareId)
            );
            if (share != null) {
                share.setDownloadCount(share.getDownloadCount() != null ? share.getDownloadCount() + 1 : 1);
                fileShareMapper.updateById(share);
            }
        } catch (Exception e) {
            log.error("Failed to record download", e);
        }
    }

    /**
     * 取消分享
     */
    public FileResponse revokeShare(String shareId, Long userId) {
        try {
            FileShare share = fileShareMapper.selectOne(
                    new LambdaQueryWrapper<FileShare>()
                            .eq(FileShare::getShareId, shareId)
                            .eq(FileShare::getCreatedBy, String.valueOf(userId))
            );

            if (share == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Share not found or no permission")
                        .build();
            }

            share.setStatus(0);
            fileShareMapper.updateById(share);

            return FileResponse.builder()
                    .success(true)
                    .message("Share revoked successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to revoke share", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to revoke share: " + e.getMessage())
                    .build();
        }
    }
}
