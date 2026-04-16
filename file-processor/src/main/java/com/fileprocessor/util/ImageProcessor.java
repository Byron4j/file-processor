package com.fileprocessor.util;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

/**
 * Image processing utility for format conversion, thumbnails, and info extraction
 */
public class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    /**
     * Supported image formats
     */
    public enum ImageFormat {
        JPEG("jpg", "jpeg"),
        PNG("png"),
        GIF("gif"),
        BMP("bmp"),
        WEBP("webp"),
        TIFF("tiff", "tif");

        private final String[] extensions;

        ImageFormat(String... extensions) {
            this.extensions = extensions;
        }

        public String[] getExtensions() {
            return extensions;
        }

        public String getPrimaryExtension() {
            return extensions[0];
        }

        public static ImageFormat fromString(String format) {
            if (format == null) return null;
            String lower = format.toLowerCase();
            for (ImageFormat f : values()) {
                for (String ext : f.extensions) {
                    if (ext.equals(lower) || f.name().toLowerCase().equals(lower)) {
                        return f;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Image information holder
     */
    public static class ImageInfo {
        private String format;
        private int width;
        private int height;
        private String colorType;
        private int dpi;
        private long fileSize;
        private double aspectRatio;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public String getColorType() { return colorType; }
        public void setColorType(String colorType) { this.colorType = colorType; }
        public int getDpi() { return dpi; }
        public void setDpi(int dpi) { this.dpi = dpi; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public double getAspectRatio() { return aspectRatio; }
        public void setAspectRatio(double aspectRatio) { this.aspectRatio = aspectRatio; }
    }

    /**
     * Thumbnail configuration
     */
    public static class ThumbnailConfig {
        private int width;
        private int height;
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

    public enum ThumbnailMode {
        FIT,      // Fit within dimensions, keep aspect ratio
        FILL,     // Fill dimensions, crop if necessary
        SCALE     // Scale to exact dimensions
    }

    /**
     * Convert image format
     *
     * @param sourcePath Source image path
     * @param targetPath Target image path
     * @param targetFormat Target format
     * @param quality Quality (0.0 - 1.0), only for JPEG/WebP
     * @return true if conversion successful
     */
    public static boolean convertFormat(String sourcePath, String targetPath,
                                        ImageFormat targetFormat, float quality) {
        log.info("Converting image format: {} -> {}, format: {}",
                sourcePath, targetPath, targetFormat);

        try {
            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            BufferedImage image = ImageIO.read(new File(sourcePath));
            if (image == null) {
                log.error("Failed to read image: {}", sourcePath);
                return false;
            }

            String formatName = targetFormat.name();

            // Handle quality setting for JPEG
            if (targetFormat == ImageFormat.JPEG || targetFormat == ImageFormat.WEBP) {
                Thumbnails.of(image)
                        .scale(1.0)
                        .outputQuality(quality)
                        .outputFormat(formatName.toLowerCase())
                        .toFile(targetFile);
            } else {
                ImageIO.write(image, formatName, targetFile);
            }

            log.info("Successfully converted image to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to convert image format: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Create thumbnail
     *
     * @param sourcePath Source image path
     * @param targetPath Target thumbnail path
     * @param config Thumbnail configuration
     * @return true if creation successful
     */
    public static boolean createThumbnail(String sourcePath, String targetPath,
                                          ThumbnailConfig config) {
        log.info("Creating thumbnail: {} -> {} ({}x{}, mode: {})",
                sourcePath, targetPath, config.getWidth(), config.getHeight(), config.getMode());

        try {
            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Thumbnails.Builder<File> builder = Thumbnails.of(new File(sourcePath));

            switch (config.getMode()) {
                case FIT:
                    builder.size(config.getWidth(), config.getHeight());
                    if (!config.isKeepAspectRatio()) {
                        builder.forceSize(config.getWidth(), config.getHeight());
                    }
                    break;
                case FILL:
                    builder.size(config.getWidth(), config.getHeight());
                    builder.crop(Positions.CENTER);
                    break;
                case SCALE:
                    builder.forceSize(config.getWidth(), config.getHeight());
                    break;
            }

            builder.outputQuality(config.getQuality());

            if (config.getOutputFormat() != null) {
                builder.outputFormat(config.getOutputFormat().toLowerCase());
            }

            builder.toFile(targetFile);

            log.info("Successfully created thumbnail: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to create thumbnail: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Get image information
     *
     * @param filePath Image file path
     * @return ImageInfo object
     */
    public static ImageInfo getImageInfo(String filePath) {
        log.info("Getting image info: {}", filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("Image file not found: {}", filePath);
                return null;
            }

            ImageInfo info = new ImageInfo();
            info.setFileSize(file.length());

            // Try to use ImageIO readers for format detection
            try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    info.setFormat(reader.getFormatName());
                    reader.setInput(iis);
                    info.setWidth(reader.getWidth(0));
                    info.setHeight(reader.getHeight(0));
                    info.setAspectRatio((double) info.getWidth() / info.getHeight());
                    reader.dispose();
                }
            }

            // Read image for color type
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                info.setColorType(getColorTypeName(image.getType()));

                // Estimate DPI (default 72 if not available)
                info.setDpi(72);
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to get image info: {}", filePath, e);
            return null;
        }
    }

    /**
     * Compress image
     *
     * @param sourcePath Source image path
     * @param targetPath Target image path
     * @param maxWidth Max width (0 for no limit)
     * @param maxHeight Max height (0 for no limit)
     * @param quality Quality (0.0 - 1.0)
     * @return true if compression successful
     */
    public static boolean compress(String sourcePath, String targetPath,
                                   int maxWidth, int maxHeight, float quality) {
        log.info("Compressing image: {} -> {} (max: {}x{}, quality: {})",
                sourcePath, targetPath, maxWidth, maxHeight, quality);

        try {
            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Thumbnails.Builder<File> builder = Thumbnails.of(new File(sourcePath));

            if (maxWidth > 0 && maxHeight > 0) {
                builder.size(maxWidth, maxHeight);
            } else if (maxWidth > 0) {
                builder.width(maxWidth);
            } else if (maxHeight > 0) {
                builder.height(maxHeight);
            }

            builder.outputQuality(quality);
            builder.toFile(targetFile);

            log.info("Successfully compressed image: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to compress image: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Batch process images
     *
     * @param sourcePaths List of source image paths
     * @param outputDir Output directory
     * @param config Thumbnail configuration
     * @return Map of source path to result (true/false)
     */
    public static java.util.Map<String, Boolean> batchThumbnail(
            List<String> sourcePaths, String outputDir, ThumbnailConfig config) {

        log.info("Batch creating thumbnails for {} images", sourcePaths.size());

        java.util.Map<String, Boolean> results = new java.util.HashMap<>();

        for (String sourcePath : sourcePaths) {
            File sourceFile = new File(sourcePath);
            String fileName = sourceFile.getName();
            String targetPath = outputDir + File.separator + "thumb_" + fileName;

            boolean success = createThumbnail(sourcePath, targetPath, config);
            results.put(sourcePath, success);
        }

        return results;
    }

    // ==================== Helper Methods ====================

    private static String getColorTypeName(int imageType) {
        return switch (imageType) {
            case BufferedImage.TYPE_INT_RGB -> "RGB";
            case BufferedImage.TYPE_INT_ARGB -> "ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR -> "BGR";
            case BufferedImage.TYPE_3BYTE_BGR -> "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4BYTE_ABGR";
            case BufferedImage.TYPE_BYTE_GRAY -> "GRAY";
            case BufferedImage.TYPE_BYTE_BINARY -> "BINARY";
            case BufferedImage.TYPE_BYTE_INDEXED -> "INDEXED";
            default -> "UNKNOWN";
        };
    }

    /**
     * Check if file is supported image format
     */
    public static boolean isSupportedFormat(String filePath) {
        if (filePath == null) return false;

        String lowerPath = filePath.toLowerCase();
        for (ImageFormat format : ImageFormat.values()) {
            for (String ext : format.getExtensions()) {
                if (lowerPath.endsWith("." + ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get image format from file path
     */
    public static ImageFormat getImageFormat(String filePath) {
        if (filePath == null) return null;

        String lowerPath = filePath.toLowerCase();
        for (ImageFormat format : ImageFormat.values()) {
            for (String ext : format.getExtensions()) {
                if (lowerPath.endsWith("." + ext)) {
                    return format;
                }
            }
        }
        return null;
    }

    /**
     * Rotate image
     *
     * @param sourcePath Source image path
     * @param targetPath Target image path
     * @param angle Rotation angle (90, 180, 270)
     * @return true if rotation successful
     */
    public static boolean rotate(String sourcePath, String targetPath, int angle) {
        log.info("Rotating image: {} by {} degrees", sourcePath, angle);

        try {
            // Normalize angle to 0, 90, 180, 270
            angle = ((angle % 360) + 360) % 360;
            if (angle % 90 != 0) {
                log.error("Rotation angle must be multiple of 90: {}", angle);
                return false;
            }

            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Thumbnails.of(new File(sourcePath))
                    .scale(1.0)
                    .rotate(angle)
                    .toFile(targetFile);

            log.info("Successfully rotated image: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to rotate image: {}", sourcePath, e);
            return false;
        }
    }
}
