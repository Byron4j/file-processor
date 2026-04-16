package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.dto.TranscriptionRequest;
import com.fileprocessor.service.AudioTranscriptionService;
import com.fileprocessor.service.VideoEditingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 音视频增强控制器 - 转录、字幕、视频编辑
 */
@RestController
@RequestMapping("/api/media")
public class MediaEnhancementController {

    private static final Logger log = LoggerFactory.getLogger(MediaEnhancementController.class);

    @Autowired
    private AudioTranscriptionService transcriptionService;

    @Autowired
    private VideoEditingService videoEditingService;

    /**
     * 音频转录
     */
    @PostMapping("/audio/transcribe")
    public ResponseEntity<FileResponse> transcribeAudio(@RequestBody TranscriptionRequest request) {
        log.info("Audio transcription request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = transcriptionService.transcribe(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成字幕
     */
    @PostMapping("/video/subtitle/generate")
    public ResponseEntity<FileResponse> generateSubtitle(@RequestBody SubtitleRequest request) {
        log.info("Generate subtitle request: {} -> {}", request.getSourcePath(), request.getTargetPath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        TranscriptionRequest transRequest = new TranscriptionRequest();
        transRequest.setSourcePath(request.getSourcePath());
        transRequest.setLanguage(request.getLanguage());
        transRequest.setResponseFormat(request.getSubtitleFormat() != null ? request.getSubtitleFormat().toLowerCase() : "srt");

        String targetPath = request.getTargetPath() != null ? request.getTargetPath() :
                request.getSourcePath().replaceFirst("\\.[^.]+$", "." + (request.getSubtitleFormat() != null ? request.getSubtitleFormat().toLowerCase() : "srt"));

        FileResponse response = transcriptionService.generateSubtitle(transRequest, targetPath,
                request.getSubtitleFormat() != null ? request.getSubtitleFormat().toLowerCase() : "srt");
        return ResponseEntity.ok(response);
    }

    /**
     * 字幕烧录到视频
     */
    @PostMapping("/video/subtitle/burn")
    public ResponseEntity<FileResponse> burnSubtitles(@RequestBody SubtitleBurnRequest request) {
        log.info("Burn subtitles request: {}", request.getVideoPath());

        if (request.getVideoPath() == null || request.getSubtitlePath() == null) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Video path and subtitle path are required")
                            .build()
            );
        }

        FileResponse response = videoEditingService.burnSubtitles(
                request.getVideoPath(),
                request.getSubtitlePath(),
                request.getTargetPath(),
                request.getFontName(),
                request.getFontSize(),
                request.getPrimaryColor(),
                request.getPosition()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 合并视频
     */
    @PostMapping("/video/merge")
    public ResponseEntity<FileResponse> mergeVideos(@RequestBody MergeVideoRequest request) {
        log.info("Merge videos request: {} files", request.getSourcePaths() != null ? request.getSourcePaths().size() : 0);

        if (request.getSourcePaths() == null || request.getSourcePaths().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source paths are required")
                            .build()
            );
        }

        FileResponse response = videoEditingService.mergeVideos(
                request.getSourcePaths(),
                request.getTargetPath(),
                request.getTransition()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 剪辑视频
     */
    @PostMapping("/video/trim")
    public ResponseEntity<FileResponse> trimVideo(@RequestBody TrimVideoRequest request) {
        log.info("Trim video request: {} from {} to {}",
                request.getSourcePath(), request.getStartTime(), request.getEndTime());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = videoEditingService.trimVideo(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getStartTime(),
                request.getEndTime(),
                request.getKeepAudio() != null ? request.getKeepAudio() : true
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 生成 GIF
     */
    @PostMapping("/video/gif")
    public ResponseEntity<FileResponse> generateGif(@RequestBody GifRequest request) {
        log.info("Generate GIF request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = videoEditingService.generateGif(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getStartTime() != null ? request.getStartTime() : 0,
                request.getDuration() != null ? request.getDuration() : 5.0,
                request.getWidth() != null ? request.getWidth() : 480,
                request.getHeight() != null ? request.getHeight() : 270,
                request.getFps() != null ? request.getFps() : 15,
                request.getQuality() != null ? request.getQuality() : 80,
                request.getOptimize() != null ? request.getOptimize() : true
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 添加水印
     */
    @PostMapping("/video/watermark")
    public ResponseEntity<FileResponse> addWatermark(@RequestBody WatermarkRequest request) {
        log.info("Add watermark request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = videoEditingService.addWatermark(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getWatermarkType(),
                request.getText(),
                request.getImagePath(),
                request.getFontSize() != null ? request.getFontSize() : 24,
                request.getColor(),
                request.getOpacity() != null ? request.getOpacity() : 0.5,
                request.getPosition(),
                request.getMarginX() != null ? request.getMarginX() : 20,
                request.getMarginY() != null ? request.getMarginY() : 20
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class SubtitleRequest {
        private String sourcePath;
        private String targetPath;
        private String language = "zh";
        private String subtitleFormat = "SRT";

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getSubtitleFormat() { return subtitleFormat; }
        public void setSubtitleFormat(String subtitleFormat) { this.subtitleFormat = subtitleFormat; }
    }

    public static class SubtitleBurnRequest {
        private String videoPath;
        private String subtitlePath;
        private String targetPath;
        private String fontName;
        private int fontSize;
        private String primaryColor;
        private String position;

        public String getVideoPath() { return videoPath; }
        public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
        public String getSubtitlePath() { return subtitlePath; }
        public void setSubtitlePath(String subtitlePath) { this.subtitlePath = subtitlePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getFontName() { return fontName; }
        public void setFontName(String fontName) { this.fontName = fontName; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
    }

    public static class MergeVideoRequest {
        private List<String> sourcePaths;
        private String targetPath;
        private String transition;

        public List<String> getSourcePaths() { return sourcePaths; }
        public void setSourcePaths(List<String> sourcePaths) { this.sourcePaths = sourcePaths; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getTransition() { return transition; }
        public void setTransition(String transition) { this.transition = transition; }
    }

    public static class TrimVideoRequest {
        private String sourcePath;
        private String targetPath;
        private double startTime;
        private double endTime;
        private Boolean keepAudio;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public double getStartTime() { return startTime; }
        public void setStartTime(double startTime) { this.startTime = startTime; }
        public double getEndTime() { return endTime; }
        public void setEndTime(double endTime) { this.endTime = endTime; }
        public Boolean getKeepAudio() { return keepAudio; }
        public void setKeepAudio(Boolean keepAudio) { this.keepAudio = keepAudio; }
    }

    public static class GifRequest {
        private String sourcePath;
        private String targetPath;
        private Double startTime;
        private Double duration;
        private Integer width;
        private Integer height;
        private Integer fps;
        private Integer quality;
        private Boolean optimize;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public Double getStartTime() { return startTime; }
        public void setStartTime(Double startTime) { this.startTime = startTime; }
        public Double getDuration() { return duration; }
        public void setDuration(Double duration) { this.duration = duration; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Integer getFps() { return fps; }
        public void setFps(Integer fps) { this.fps = fps; }
        public Integer getQuality() { return quality; }
        public void setQuality(Integer quality) { this.quality = quality; }
        public Boolean getOptimize() { return optimize; }
        public void setOptimize(Boolean optimize) { this.optimize = optimize; }
    }

    public static class WatermarkRequest {
        private String sourcePath;
        private String targetPath;
        private String watermarkType;
        private String text;
        private String imagePath;
        private Integer fontSize;
        private String color;
        private Double opacity;
        private String position;
        private Integer marginX;
        private Integer marginY;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getWatermarkType() { return watermarkType; }
        public void setWatermarkType(String watermarkType) { this.watermarkType = watermarkType; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Double getOpacity() { return opacity; }
        public void setOpacity(Double opacity) { this.opacity = opacity; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public Integer getMarginX() { return marginX; }
        public void setMarginX(Integer marginX) { this.marginX = marginX; }
        public Integer getMarginY() { return marginY; }
        public void setMarginY(Integer marginY) { this.marginY = marginY; }
    }
}