package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.ImageService;
import com.fileprocessor.util.ImageProcessor.ThumbnailConfig;
import com.fileprocessor.util.ImageProcessor.ThumbnailMode;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Image processing REST API controller
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    /**
     * Convert image format
     */
    @PostMapping("/convert")
    public ResponseEntity<FileResponse> convertFormat(
            @RequestBody @Valid ImageConvertRequest request) {
        log.info("REST request to convert image: {} -> {}, format: {}",
                request.getSourcePath(), request.getTargetPath(), request.getTargetFormat());

        FileResponse response = imageService.convertFormat(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getTargetFormat(),
                request.getQuality()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Create thumbnail
     */
    @PostMapping("/thumbnail")
    public ResponseEntity<FileResponse> createThumbnail(
            @RequestBody @Valid ThumbnailRequest request) {
        log.info("REST request to create thumbnail: {} -> {} ({}x{}, mode: {})",
                request.getSourcePath(), request.getTargetPath(),
                request.getWidth(), request.getHeight(), request.getMode());

        ThumbnailConfig config = new ThumbnailConfig();
        config.setWidth(request.getWidth());
        config.setHeight(request.getHeight());
        config.setMode(request.getMode());
        config.setKeepAspectRatio(request.isKeepAspectRatio());
        config.setQuality(request.getQuality());
        config.setOutputFormat(request.getOutputFormat());

        FileResponse response = imageService.createThumbnail(
                request.getSourcePath(),
                request.getTargetPath(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get image information
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getImageInfo(
            @RequestParam String path) {
        log.info("REST request to get image info: {}", path);

        FileResponse response = imageService.getImageInfo(path);
        return ResponseEntity.ok(response);
    }

    /**
     * Compress image
     */
    @PostMapping("/compress")
    public ResponseEntity<FileResponse> compress(
            @RequestBody @Valid ImageCompressRequest request) {
        log.info("REST request to compress image: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = imageService.compress(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getMaxWidth(),
                request.getMaxHeight(),
                request.getQuality()
        );

        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class ImageConvertRequest extends FileConvertRequest {
        private String targetFormat;
        private float quality = 0.9f;

        public String getTargetFormat() { return targetFormat; }
        public void setTargetFormat(String targetFormat) { this.targetFormat = targetFormat; }
        public float getQuality() { return quality; }
        public void setQuality(float quality) { this.quality = quality; }
    }

    public static class ThumbnailRequest extends FileConvertRequest {
        private int width = 200;
        private int height = 200;
        private ThumbnailMode mode = ThumbnailMode.FIT;
        private boolean keepAspectRatio = true;
        private float quality = 0.8f;
        private String outputFormat;

        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public ThumbnailMode getMode() { return mode; }
        public void setMode(ThumbnailMode mode) { this.mode = mode; }
        public boolean isKeepAspectRatio() { return keepAspectRatio; }
        public void setKeepAspectRatio(boolean keepAspectRatio) { this.keepAspectRatio = keepAspectRatio; }
        public float getQuality() { return quality; }
        public void setQuality(float quality) { this.quality = quality; }
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    }

    public static class ImageCompressRequest extends FileConvertRequest {
        private int maxWidth = 0;
        private int maxHeight = 0;
        private float quality = 0.8f;

        public int getMaxWidth() { return maxWidth; }
        public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }
        public int getMaxHeight() { return maxHeight; }
        public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }
        public float getQuality() { return quality; }
        public void setQuality(float quality) { this.quality = quality; }
    }
}
