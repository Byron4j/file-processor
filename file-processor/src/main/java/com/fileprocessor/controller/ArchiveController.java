package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.ArchiveService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Archive processing REST API controller for 7z and RAR
 */
@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    private static final Logger log = LoggerFactory.getLogger(ArchiveController.class);

    @Autowired
    private ArchiveService archiveService;

    /**
     * Extract 7z archive
     */
    @PostMapping("/extract/7z")
    public ResponseEntity<FileResponse> extract7z(
            @RequestBody @Valid ArchiveExtractRequest request) {
        log.info("REST request to extract 7z: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = archiveService.extract7z(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Extract RAR archive
     */
    @PostMapping("/extract/rar")
    public ResponseEntity<FileResponse> extractRar(
            @RequestBody @Valid ArchiveExtractRequest request) {
        log.info("REST request to extract RAR: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = archiveService.extractRar(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Auto-detect and extract archive
     */
    @PostMapping("/extract")
    public ResponseEntity<FileResponse> extract(
            @RequestBody @Valid ArchiveExtractRequest request) {
        log.info("REST request to extract archive: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = archiveService.extract(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get archive information
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getArchiveInfo(
            @RequestParam String path) {
        log.info("REST request to get archive info: {}", path);

        FileResponse response = archiveService.getArchiveInfo(path);
        return ResponseEntity.ok(response);
    }

    /**
     * Create archive (ZIP/7z/TAR)
     */
    @PostMapping("/create")
    public ResponseEntity<FileResponse> createArchive(
            @RequestBody CreateArchiveRequest request) {
        log.info("Create archive request: format={}, files={}",
                request.getFormat(),
                request.getSourcePaths() != null ? request.getSourcePaths().size() : 0);

        if (request.getSourcePaths() == null || request.getSourcePaths().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source paths are required")
                            .build()
            );
        }

        FileResponse response = archiveService.createArchive(
                request.getSourcePaths(),
                request.getTargetPath(),
                request.getFormat(),
                request.getCompressionLevel(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class ArchiveExtractRequest extends FileConvertRequest {
        private String password;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class CreateArchiveRequest {
        private java.util.List<String> sourcePaths;
        private String targetPath;
        private String format;  // ZIP, 7Z, TAR, TAR_GZ
        private Integer compressionLevel;  // 0-9
        private String password;

        public java.util.List<String> getSourcePaths() { return sourcePaths; }
        public void setSourcePaths(java.util.List<String> sourcePaths) { this.sourcePaths = sourcePaths; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public Integer getCompressionLevel() { return compressionLevel; }
        public void setCompressionLevel(Integer compressionLevel) { this.compressionLevel = compressionLevel; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
