package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.TextExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * File preview service
 */
@Service
public class PreviewService {

    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);

    @Autowired
    private MetadataService metadataService;

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    /**
     * Get preview information
     */
    public FileResponse getPreviewInfo(String fileId, String preferredType) {
        log.info("Getting preview info for file: {}", fileId);

        // Get file metadata
        FileResponse metadataResponse = metadataService.getFile(fileId);
        if (!metadataResponse.isSuccess()) {
            return metadataResponse;
        }

        Map<String, Object> metadata = (Map<String, Object>) metadataResponse.getData();
        String extension = (String) metadata.get("extension");
        String storagePath = (String) metadata.get("storagePath");

        PreviewType previewType = determinePreviewType(extension, preferredType);

        Map<String, Object> data = new HashMap<>();
        data.put("fileId", fileId);
        data.put("previewType", previewType.name());
        data.put("originalName", metadata.get("originalName"));
        data.put("fileSize", metadata.get("fileSize"));

        // Generate preview if needed
        if (previewType == PreviewType.PDF && !"pdf".equalsIgnoreCase(extension)) {
            // Need conversion
            String previewPath = generatePreviewPath(fileId, "pdf");
            boolean converted = convertToPdf(storagePath, previewPath);
            if (converted) {
                data.put("previewUrl", "/api/preview/" + fileId + "/content");
                data.put("converted", true);
            } else {
                // Fallback to text preview
                previewType = PreviewType.TEXT;
                data.put("previewType", "TEXT");
            }
        } else if (previewType == PreviewType.IMAGE) {
            data.put("previewUrl", "/api/preview/" + fileId + "/content");
            data.put("width", metadata.getOrDefault("width", 0));
            data.put("height", metadata.getOrDefault("height", 0));
        } else if (previewType == PreviewType.TEXT) {
            data.put("previewUrl", "/api/preview/" + fileId + "/content");
        } else if (previewType == PreviewType.PDF) {
            data.put("previewUrl", "/api/preview/" + fileId + "/content");
        }

        return FileResponse.builder()
                .success(true)
                .message("Preview info retrieved")
                .data(data)
                .build();
    }

    /**
     * Get preview content
     */
    public PreviewContent getPreviewContent(String fileId, Integer page) {
        log.info("Getting preview content for file: {}, page: {}", fileId, page);

        try {
            // Get file metadata
            FileResponse metadataResponse = metadataService.getFile(fileId);
            if (!metadataResponse.isSuccess()) {
                return null;
            }

            Map<String, Object> metadata = (Map<String, Object>) metadataResponse.getData();
            String extension = (String) metadata.get("extension");
            String storagePath = (String) metadata.get("storagePath");

            PreviewType previewType = determinePreviewType(extension, null);

            File file = new File(storagePath);
            if (!file.exists()) {
                // Try with base path
                file = new File(basePath + "/" + storagePath);
            }

            if (!file.exists()) {
                log.error("File not found: {}", storagePath);
                return null;
            }

            String contentType = getContentType(previewType, extension);
            byte[] content;

            if (previewType == PreviewType.TEXT) {
                // Extract text content
                String text = TextExtractor.extractText(file.getAbsolutePath());
                content = text != null ? text.getBytes() : new byte[0];
            } else {
                // Read binary content
                content = Files.readAllBytes(file.toPath());
            }

            return new PreviewContent(content, contentType, previewType.name());

        } catch (Exception e) {
            log.error("Failed to get preview content", e);
            return null;
        }
    }

    /**
     * Determine preview type based on file extension
     */
    private PreviewType determinePreviewType(String extension, String preferredType) {
        if (extension == null) {
            return PreviewType.TEXT;
        }

        String ext = extension.toLowerCase();

        // Image files
        if (ext.matches("jpg|jpeg|png|gif|bmp|webp|tiff")) {
            return PreviewType.IMAGE;
        }

        // PDF files
        if ("pdf".equals(ext)) {
            return PreviewType.PDF;
        }

        // Office documents - can convert to PDF
        if (ext.matches("doc|docx|xls|xlsx|ppt|pptx")) {
            if ("PDF".equals(preferredType)) {
                return PreviewType.PDF;
            }
            return PreviewType.TEXT;
        }

        // Text files
        if (ext.matches("txt|md|json|xml|html|htm|csv|log|java|js|css|py")) {
            return PreviewType.TEXT;
        }

        return PreviewType.TEXT;
    }

    /**
     * Convert file to PDF for preview
     */
    private boolean convertToPdf(String sourcePath, String targetPath) {
        // TODO: Implement Office to PDF conversion using LibreOffice or similar
        // For now, return false to fallback to text preview
        log.warn("PDF conversion not yet implemented");
        return false;
    }

    /**
     * Generate preview file path
     */
    private String generatePreviewPath(String fileId, String extension) {
        return basePath + "/previews/" + fileId + "." + extension;
    }

    /**
     * Get content type
     */
    private String getContentType(PreviewType previewType, String extension) {
        switch (previewType) {
            case PDF:
                return "application/pdf";
            case IMAGE:
                if ("png".equalsIgnoreCase(extension)) return "image/png";
                if ("gif".equalsIgnoreCase(extension)) return "image/gif";
                if ("bmp".equalsIgnoreCase(extension)) return "image/bmp";
                if ("webp".equalsIgnoreCase(extension)) return "image/webp";
                return "image/jpeg";
            case TEXT:
                return "text/plain; charset=utf-8";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Preview content holder
     */
    public static class PreviewContent {
        private final byte[] content;
        private final String contentType;
        private final String previewType;

        public PreviewContent(byte[] content, String contentType, String previewType) {
            this.content = content;
            this.contentType = contentType;
            this.previewType = previewType;
        }

        public byte[] getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getPreviewType() {
            return previewType;
        }
    }

    /**
     * Preview type enumeration
     */
    public enum PreviewType {
        PDF, IMAGE, TEXT
    }
}
