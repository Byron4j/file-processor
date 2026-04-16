package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 视频编辑服务 - 合并、剪辑、GIF生成、字幕烧录、水印
 */
@Service
public class VideoEditingService {

    private static final Logger log = LoggerFactory.getLogger(VideoEditingService.class);

    @Value("${media.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${media.ffmpeg.timeout:600}")
    private int ffmpegTimeout;

    @Value("${file.output.path:./outputs}")
    private String outputPath;

    /**
     * 合并多个视频
     */
    public FileResponse mergeVideos(List<String> sourcePaths, String targetPath, String transition) {
        log.info("Merging {} videos with transition: {}", sourcePaths.size(), transition);

        try {
            // Validate source files
            for (String path : sourcePaths) {
                if (!Files.exists(Paths.get(path))) {
                    return FileResponse.builder()
                            .success(false)
                            .message("Source file not found: " + path)
                            .build();
                }
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath("merged", ".mp4");
            }

            // Create concat file list
            String concatFile = createConcatFile(sourcePaths);

            // Build FFmpeg command
            StringBuilder cmd = new StringBuilder();
            cmd.append(ffmpegPath).append(" -f concat -safe 0 -i ").append(concatFile);

            // Add transition if specified
            if ("fade".equals(transition)) {
                cmd.append(" -vf 'fade=st=0:d=0.5:alpha=1'")
                   .append(" -c:v libx264 -preset fast -crf 23");
            } else {
                cmd.append(" -c copy");
            }

            cmd.append(" -y ").append(targetPath);

            // Execute command
            boolean success = executeCommand(cmd.toString());

            // Clean up concat file
            Files.deleteIfExists(Paths.get(concatFile));

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("Video merge failed")
                        .build();
            }

            File outputFile = new File(targetPath);
            return FileResponse.builder()
                    .success(true)
                    .message("Videos merged successfully")
                    .filePath(targetPath)
                    .fileSize(outputFile.length())
                    .data(Map.of(
                            "sourceCount", sourcePaths.size(),
                            "transition", transition != null ? transition : "none"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Video merge failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Video merge failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 视频剪辑 - 提取单个片段
     */
    public FileResponse trimVideo(String sourcePath, String targetPath, double startTime, double endTime, boolean keepAudio) {
        log.info("Trimming video: {} from {} to {}", sourcePath, startTime, endTime);

        try {
            if (!Files.exists(Paths.get(sourcePath))) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath("trimmed", ".mp4");
            }

            double duration = endTime - startTime;

            StringBuilder cmd = new StringBuilder();
            cmd.append(ffmpegPath)
               .append(" -i ").append(sourcePath)
               .append(" -ss ").append(startTime)
               .append(" -t ").append(duration);

            if (keepAudio) {
                cmd.append(" -c copy");
            } else {
                cmd.append(" -an -c:v libx264 -preset fast");
            }

            cmd.append(" -y ").append(targetPath);

            boolean success = executeCommand(cmd.toString());

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("Video trim failed")
                        .build();
            }

            File outputFile = new File(targetPath);
            return FileResponse.builder()
                    .success(true)
                    .message("Video trimmed successfully")
                    .filePath(targetPath)
                    .fileSize(outputFile.length())
                    .data(Map.of(
                            "startTime", startTime,
                            "endTime", endTime,
                            "duration", duration
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Video trim failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Video trim failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 生成 GIF
     */
    public FileResponse generateGif(String sourcePath, String targetPath, double startTime, double duration,
                                     int width, int height, int fps, int quality, boolean optimize) {
        log.info("Generating GIF from: {}, start={}, duration={}", sourcePath, startTime, duration);

        try {
            if (!Files.exists(Paths.get(sourcePath))) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath("animation", ".gif");
            }

            // Default values
            if (width <= 0) width = 480;
            if (height <= 0) height = 270;
            if (fps <= 0) fps = 15;
            if (quality <= 0) quality = 80;

            StringBuilder cmd = new StringBuilder();
            cmd.append(ffmpegPath).append(" -i ").append(sourcePath);

            // Time range
            if (startTime > 0) {
                cmd.append(" -ss ").append(startTime);
            }
            if (duration > 0) {
                cmd.append(" -t ").append(duration);
            }

            // Video filter for GIF optimization
            cmd.append(" -vf 'fps=").append(fps)
               .append(",scale=").append(width).append(":").append(height)
               .append(":flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=")
               .append(optimize ? 128 : 256)
               .append("[p];[s1][p]paletteuse'");

            // Optimization
            if (optimize) {
                cmd.append(" -colors 128");
            }

            cmd.append(" -loop 0 -y ").append(targetPath);

            boolean success = executeCommand(cmd.toString());

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("GIF generation failed")
                        .build();
            }

            File outputFile = new File(targetPath);
            int frameCount = (int) (duration * fps);

            return FileResponse.builder()
                    .success(true)
                    .message("GIF generated successfully")
                    .filePath(targetPath)
                    .fileSize(outputFile.length())
                    .data(Map.of(
                            "width", width,
                            "height", height,
                            "duration", duration,
                            "frameCount", frameCount,
                            "fps", fps
                    ))
                    .build();

        } catch (Exception e) {
            log.error("GIF generation failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("GIF generation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 字幕烧录到视频
     */
    public FileResponse burnSubtitles(String videoPath, String subtitlePath, String targetPath,
                                       String fontName, int fontSize, String primaryColor, String position) {
        log.info("Burning subtitles into video: {}", videoPath);

        try {
            if (!Files.exists(Paths.get(videoPath))) {
                return FileResponse.builder()
                        .success(false)
                        .message("Video file not found: " + videoPath)
                        .build();
            }

            if (!Files.exists(Paths.get(subtitlePath))) {
                return FileResponse.builder()
                        .success(false)
                        .message("Subtitle file not found: " + subtitlePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath("subtitled", ".mp4");
            }

            // Default values
            if (fontName == null || fontName.isEmpty()) fontName = "Arial";
            if (fontSize <= 0) fontSize = 24;
            if (primaryColor == null || primaryColor.isEmpty()) primaryColor = "#FFFFFF";

            // Escape subtitle path for filter
            String escapedSubtitlePath = subtitlePath.replace(":", "\\:").replace("\\", "/");

            StringBuilder filter = new StringBuilder();
            filter.append("subtitles=").append(escapedSubtitlePath)
                  .append(":force_style='FontName=").append(fontName)
                  .append(",FontSize=").append(fontSize)
                  .append(",PrimaryColour=").append(primaryColor);

            // Position alignment
            int alignment = 2; // bottom-center default
            if ("top-left".equals(position)) alignment = 5;
            else if ("top-right".equals(position)) alignment = 6;
            else if ("bottom-left".equals(position)) alignment = 1;
            else if ("bottom-right".equals(position)) alignment = 3;
            else if ("center".equals(position)) alignment = 10;

            filter.append(",Alignment=").append(alignment).append("'");

            StringBuilder cmd = new StringBuilder();
            cmd.append(ffmpegPath)
               .append(" -i ").append(videoPath)
               .append(" -vf \"").append(filter).append("\"")
               .append(" -c:v libx264 -preset fast -crf 23")
               .append(" -c:a copy")
               .append(" -y ").append(targetPath);

            boolean success = executeCommand(cmd.toString());

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("Subtitle burn failed")
                        .build();
            }

            File outputFile = new File(targetPath);
            return FileResponse.builder()
                    .success(true)
                    .message("Subtitles burned successfully")
                    .filePath(targetPath)
                    .fileSize(outputFile.length())
                    .build();

        } catch (Exception e) {
            log.error("Subtitle burn failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Subtitle burn failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 视频添加水印
     */
    public FileResponse addWatermark(String sourcePath, String targetPath, String watermarkType,
                                      String text, String imagePath, int fontSize, String color,
                                      double opacity, String position, int marginX, int marginY) {
        log.info("Adding watermark to video: {}", sourcePath);

        try {
            if (!Files.exists(Paths.get(sourcePath))) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath("watermarked", ".mp4");
            }

            // Default values
            if (fontSize <= 0) fontSize = 24;
            if (color == null || color.isEmpty()) color = "white";
            if (opacity <= 0 || opacity > 1) opacity = 0.5;
            if (position == null || position.isEmpty()) position = "bottom-right";

            StringBuilder filter = new StringBuilder();

            if ("text".equals(watermarkType)) {
                // Text watermark
                String x = String.valueOf(marginX);
                String y = String.valueOf(marginY);

                switch (position) {
                    case "top-left": x = String.valueOf(marginX); y = String.valueOf(marginY); break;
                    case "top-right": x = "w-text_w-" + marginX; y = String.valueOf(marginY); break;
                    case "bottom-left": x = String.valueOf(marginX); y = "h-text_h-" + marginY; break;
                    case "bottom-right": x = "w-text_w-" + marginX; y = "h-text_h-" + marginY; break;
                    case "center": x = "(w-text_w)/2"; y = "(h-text_h)/2"; break;
                }

                filter.append("drawtext=text='").append(text.replace("'", "'\\''"))
                      .append("':fontsize=").append(fontSize)
                      .append(":fontcolor=").append(color).append("@").append(opacity)
                      .append(":x=").append(x)
                      .append(":y=").append(y);
            } else if ("image".equals(watermarkType) && imagePath != null) {
                // Image watermark
                if (!Files.exists(Paths.get(imagePath))) {
                    return FileResponse.builder()
                            .success(false)
                            .message("Watermark image not found: " + imagePath)
                            .build();
                }

                // Use overlay filter for image watermark
                String overlayX = String.valueOf(marginX);
                String overlayY = String.valueOf(marginY);

                switch (position) {
                    case "top-left": overlayX = String.valueOf(marginX); overlayY = String.valueOf(marginY); break;
                    case "top-right": overlayX = "W-w-" + marginX; overlayY = String.valueOf(marginY); break;
                    case "bottom-left": overlayX = String.valueOf(marginX); overlayY = "H-h-" + marginY; break;
                    case "bottom-right": overlayX = "W-w-" + marginX; overlayY = "H-h-" + marginY; break;
                    case "center": overlayX = "(W-w)/2"; overlayY = "(H-h)/2"; break;
                }

                // Complex filter with overlay
                StringBuilder cmd = new StringBuilder();
                cmd.append(ffmpegPath)
                   .append(" -i ").append(sourcePath)
                   .append(" -i ").append(imagePath)
                   .append(" -filter_complex \"[1:v]format=rgba,colorchannelmixer=aa=").append(opacity).append("[logo];[0:v][logo]overlay=").append(overlayX).append(":").append(overlayY).append("\"")
                   .append(" -c:v libx264 -preset fast -crf 23")
                   .append(" -c:a copy")
                   .append(" -y ").append(targetPath);

                boolean success = executeCommand(cmd.toString());

                if (!success) {
                    return FileResponse.builder()
                            .success(false)
                            .message("Image watermark failed")
                            .build();
                }

                File outputFile = new File(targetPath);
                return FileResponse.builder()
                        .success(true)
                        .message("Image watermark added successfully")
                        .filePath(targetPath)
                        .fileSize(outputFile.length())
                        .build();
            }

            StringBuilder cmd = new StringBuilder();
            cmd.append(ffmpegPath)
               .append(" -i ").append(sourcePath)
               .append(" -vf \"").append(filter).append("\"")
               .append(" -c:v libx264 -preset fast -crf 23")
               .append(" -c:a copy")
               .append(" -y ").append(targetPath);

            boolean success = executeCommand(cmd.toString());

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("Text watermark failed")
                        .build();
            }

            File outputFile = new File(targetPath);
            return FileResponse.builder()
                    .success(true)
                    .message("Text watermark added successfully")
                    .filePath(targetPath)
                    .fileSize(outputFile.length())
                    .build();

        } catch (Exception e) {
            log.error("Watermark failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Watermark failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 创建 concat 文件列表
     */
    private String createConcatFile(List<String> sourcePaths) throws Exception {
        String concatFile = "/tmp/concat_" + UUID.randomUUID() + ".txt";
        StringBuilder content = new StringBuilder();
        for (String path : sourcePaths) {
            content.append("file '").append(path).append("'\n");
        }
        Files.writeString(Paths.get(concatFile), content.toString());
        return concatFile;
    }

    /**
     * 执行 FFmpeg 命令
     */
    private boolean executeCommand(String command) {
        log.debug("Executing: {}", command);

        try {
            Process process = Runtime.getRuntime().exec(command);

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }

            while ((line = errorReader.readLine()) != null) {
                log.debug("FFmpeg err: {}", line);
            }

            boolean completed = process.waitFor(ffmpegTimeout, java.util.concurrent.TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                log.error("FFmpeg command timeout");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg exited with code: {}", exitCode);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Command execution failed: {}", command, e);
            return false;
        }
    }

    /**
     * 生成输出路径
     */
    private String generateOutputPath(String prefix, String extension) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s/%s/%s%s", outputPath, datePath, fileName, extension);
    }
}
