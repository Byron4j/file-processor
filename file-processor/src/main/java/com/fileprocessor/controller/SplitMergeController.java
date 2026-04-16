package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.SplitMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 文件分割与合并控制器
 */
@RestController
@RequestMapping("/api/file")
public class SplitMergeController {

    private static final Logger log = LoggerFactory.getLogger(SplitMergeController.class);

    @Autowired
    private SplitMergeService splitMergeService;

    /**
     * 分割大文件
     */
    @PostMapping("/split")
    public ResponseEntity<FileResponse> splitFile(@RequestBody SplitFileRequest request) {
        log.info("Split file request: {}, chunkSize: {}", request.getFileId(), request.getChunkSize());

        if (request.getFileId() == null || request.getFileId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("File ID is required")
                            .build()
            );
        }

        if (request.getChunkSize() <= 0) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Chunk size must be greater than 0")
                            .build()
            );
        }

        FileResponse response = splitMergeService.splitFile(
                request.getFileId(),
                request.getChunkSize(),
                request.getNamingPattern(),
                request.getOutputDir()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 合并文件
     */
    @PostMapping("/merge")
    public ResponseEntity<FileResponse> mergeFile(@RequestBody MergeFileRequest request) {
        log.info("Merge file request from manifest: {}", request.getManifestPath());

        if (request.getManifestPath() == null || request.getManifestPath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Manifest path is required")
                            .build()
            );
        }

        FileResponse response = splitMergeService.mergeFile(
                request.getManifestPath(),
                request.getTargetPath(),
                request.getVerifyHash() != null ? request.getVerifyHash() : true
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class SplitFileRequest {
        private String fileId;
        private Long chunkSize;
        private String namingPattern;
        private String outputDir;

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public Long getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(Long chunkSize) {
            this.chunkSize = chunkSize;
        }

        public String getNamingPattern() {
            return namingPattern;
        }

        public void setNamingPattern(String namingPattern) {
            this.namingPattern = namingPattern;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }
    }

    public static class MergeFileRequest {
        private String manifestPath;
        private String targetPath;
        private Boolean verifyHash;

        public String getManifestPath() {
            return manifestPath;
        }

        public void setManifestPath(String manifestPath) {
            this.manifestPath = manifestPath;
        }

        public String getTargetPath() {
            return targetPath;
        }

        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }

        public Boolean getVerifyHash() {
            return verifyHash;
        }

        public void setVerifyHash(Boolean verifyHash) {
            this.verifyHash = verifyHash;
        }
    }
}