package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.MediaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Media processing REST API controller
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    @Autowired
    private MediaService mediaService;

    /**
     * Get video information
     */
    @PostMapping("/video/info")
    public ResponseEntity<FileResponse> getVideoInfo(
            @RequestBody VideoInfoRequest request) {
        log.info("REST request to get video info: {}", request.getFilePath());

        FileResponse response = mediaService.getVideoInfo(request.getFilePath());
        return ResponseEntity.ok(response);
    }

    /**
     * Get audio information
     */
    @PostMapping("/audio/info")
    public ResponseEntity<FileResponse> getAudioInfo(
            @RequestBody AudioInfoRequest request) {
        log.info("REST request to get audio info: {}", request.getFilePath());

        FileResponse response = mediaService.getAudioInfo(request.getFilePath());
        return ResponseEntity.ok(response);
    }

    /**
     * Extract video thumbnail
     */
    @PostMapping("/video/thumbnail")
    public ResponseEntity<FileResponse> extractThumbnail(
            @RequestBody @Valid MediaService.ThumbnailRequest request) {
        log.info("REST request to extract thumbnail: {}", request.getSourcePath());

        FileResponse response = mediaService.extractThumbnail(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Transcode video
     */
    @PostMapping("/video/transcode")
    public ResponseEntity<FileResponse> transcodeVideo(
            @RequestBody @Valid MediaService.TranscodeRequest request) {
        log.info("REST request to transcode video: {}", request.getSourcePath());

        FileResponse response = mediaService.transcodeVideo(request);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class VideoInfoRequest {
        private String filePath;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }

    public static class AudioInfoRequest {
        private String filePath;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}
