package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.media.AudioInfo;
import com.fileprocessor.media.MediaProcessor;
import com.fileprocessor.media.VideoInfo;
import com.fileprocessor.task.TaskResult;
import com.fileprocessor.task.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Media processing service
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private TaskService taskService;

    /**
     * Get video information
     */
    public FileResponse getVideoInfo(String filePath) {
        log.info("Getting video info: {}", filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File not found: " + filePath)
                    .build();
        }

        VideoInfo info = MediaProcessor.getVideoInfo(filePath);
        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get video info")
                    .build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("format", info.getFormat());
        data.put("duration", info.getDuration());
        data.put("width", info.getWidth());
        data.put("height", info.getHeight());
        data.put("videoCodec", info.getVideoCodec());
        data.put("audioCodec", info.getAudioCodec());
        data.put("videoBitrate", info.getVideoBitrate());
        data.put("audioBitrate", info.getAudioBitrate());
        data.put("frameRate", info.getFrameRate());
        data.put("fileSize", info.getFileSize());

        return FileResponse.builder()
                .success(true)
                .message("Video info retrieved")
                .data(data)
                .build();
    }

    /**
     * Get audio information
     */
    public FileResponse getAudioInfo(String filePath) {
        log.info("Getting audio info: {}", filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File not found: " + filePath)
                    .build();
        }

        AudioInfo info = MediaProcessor.getAudioInfo(filePath);
        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get audio info")
                    .build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("format", info.getFormat());
        data.put("duration", info.getDuration());
        data.put("codec", info.getCodec());
        data.put("bitrate", info.getBitrate());
        data.put("sampleRate", info.getSampleRate());
        data.put("channels", info.getChannels());
        data.put("fileSize", info.getFileSize());

        return FileResponse.builder()
                .success(true)
                .message("Audio info retrieved")
                .data(data)
                .build();
    }

    /**
     * Extract thumbnail from video
     */
    public FileResponse extractThumbnail(ThumbnailRequest request) {
        log.info("Extracting thumbnail from: {}", request.getSourcePath());

        boolean success = MediaProcessor.extractThumbnail(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getTimestamp(),
                request.getWidth(),
                request.getHeight()
        );

        if (success) {
            File targetFile = new File(request.getTargetPath());
            return FileResponse.builder()
                    .success(true)
                    .message("Thumbnail extracted successfully")
                    .filePath(request.getTargetPath())
                    .fileSize(targetFile.length())
                    .build();
        } else {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract thumbnail")
                    .build();
        }
    }

    /**
     * Transcode video (async)
     */
    public FileResponse transcodeVideo(TranscodeRequest request) {
        log.info("Submitting video transcode task: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType(TaskType.CONVERT.getCode());
        submitRequest.setTaskName("Video Transcode");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("operation", "VIDEO_TRANSCODE");
        parameters.put("sourcePath", request.getSourcePath());
        parameters.put("targetPath", request.getTargetPath());
        parameters.put("format", request.getFormat());
        parameters.put("videoCodec", request.getVideoCodec());
        parameters.put("audioCodec", request.getAudioCodec());
        parameters.put("width", request.getWidth());
        parameters.put("height", request.getHeight());
        parameters.put("videoBitrate", request.getVideoBitrate());
        parameters.put("audioBitrate", request.getAudioBitrate());
        submitRequest.setParameters(parameters);

        var record = taskService.submitTask(submitRequest);

        return FileResponse.builder()
                .success(true)
                .message("Video transcode task submitted")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus()
                ))
                .build();
    }

    // ==================== Request DTOs ====================

    public static class ThumbnailRequest {
        private String sourcePath;
        private String targetPath;
        private double timestamp = 0;
        private Integer width;
        private Integer height;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public double getTimestamp() { return timestamp; }
        public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
    }

    public static class TranscodeRequest {
        private String sourcePath;
        private String targetPath;
        private String format = "mp4";
        private String videoCodec = "h264";
        private String audioCodec = "aac";
        private Integer width;
        private Integer height;
        private Long videoBitrate;
        private Long audioBitrate;
        private boolean async = true;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getVideoCodec() { return videoCodec; }
        public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
        public String getAudioCodec() { return audioCodec; }
        public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Long getVideoBitrate() { return videoBitrate; }
        public void setVideoBitrate(Long videoBitrate) { this.videoBitrate = videoBitrate; }
        public Long getAudioBitrate() { return audioBitrate; }
        public void setAudioBitrate(Long audioBitrate) { this.audioBitrate = audioBitrate; }
        public boolean isAsync() { return async; }
        public void setAsync(boolean async) { this.async = async; }
    }
}
