package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.ImageProcessor;
import com.fileprocessor.util.ImageProcessor.ImageFormat;
import com.fileprocessor.util.ImageProcessor.ImageInfo;
import com.fileprocessor.util.ImageProcessor.ThumbnailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Image processing service
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /**
     * Convert image format
     */
    public FileResponse convertFormat(String sourcePath, String targetPath,
                                      String targetFormat, float quality) {
        log.info("Service: Converting image format: {} -> {}, format: {}",
                sourcePath, targetPath, targetFormat);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate format
        ImageFormat format = ImageFormat.fromString(targetFormat);
        if (format == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported target format: " + targetFormat)
                    .build();
        }

        // Perform conversion
        boolean success = ImageProcessor.convertFormat(sourcePath, targetPath, format, quality);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to convert image format")
                    .build();
        }

        // Get output file info
        File targetFile = new File(targetPath);
        ImageInfo info = ImageProcessor.getImageInfo(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("Image converted successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .data(Map.of(
                        "format", format.name(),
                        "width", info != null ? info.getWidth() : 0,
                        "height", info != null ? info.getHeight() : 0
                ))
                .build();
    }

    /**
     * Create thumbnail
     */
    public FileResponse createThumbnail(String sourcePath, String targetPath,
                                        ThumbnailConfig config) {
        log.info("Service: Creating thumbnail: {} -> {} ({}x{})",
                sourcePath, targetPath, config.getWidth(), config.getHeight());

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate image format
        if (!ImageProcessor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported image format")
                    .build();
        }

        // Perform thumbnail creation
        boolean success = ImageProcessor.createThumbnail(sourcePath, targetPath, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to create thumbnail")
                    .build();
        }

        // Get output file info
        File targetFile = new File(targetPath);
        ImageInfo info = ImageProcessor.getImageInfo(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("Thumbnail created successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .data(Map.of(
                        "width", info != null ? info.getWidth() : 0,
                        "height", info != null ? info.getHeight() : 0
                ))
                .build();
    }

    /**
     * Get image information
     */
    public FileResponse getImageInfo(String filePath) {
        log.info("Service: Getting image info: {}", filePath);

        // Validate file
        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        // Get info
        ImageInfo info = ImageProcessor.getImageInfo(filePath);

        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get image information")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Image information retrieved")
                .filePath(filePath)
                .data(Map.of(
                        "format", info.getFormat(),
                        "width", info.getWidth(),
                        "height", info.getHeight(),
                        "colorType", info.getColorType(),
                        "dpi", info.getDpi(),
                        "fileSize", info.getFileSize(),
                        "aspectRatio", info.getAspectRatio()
                ))
                .build();
    }

    /**
     * Compress image
     */
    public FileResponse compress(String sourcePath, String targetPath,
                                 int maxWidth, int maxHeight, float quality) {
        log.info("Service: Compressing image: {} -> {}", sourcePath, targetPath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Perform compression
        boolean success = ImageProcessor.compress(sourcePath, targetPath, maxWidth, maxHeight, quality);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to compress image")
                    .build();
        }

        // Get output file info
        File targetFile = new File(targetPath);
        long originalSize = sourceFile.length();
        long compressedSize = targetFile.length();
        double compressionRatio = (1 - (double) compressedSize / originalSize) * 100;

        return FileResponse.builder()
                .success(true)
                .message("Image compressed successfully")
                .filePath(targetPath)
                .fileSize(compressedSize)
                .data(Map.of(
                        "originalSize", originalSize,
                        "compressedSize", compressedSize,
                        "compressionRatio", String.format("%.2f%%", compressionRatio)
                ))
                .build();
    }
}
