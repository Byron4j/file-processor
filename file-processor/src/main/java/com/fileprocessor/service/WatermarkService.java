package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.WatermarkUtil;
import com.fileprocessor.util.WatermarkUtil.ImageWatermarkConfig;
import com.fileprocessor.util.WatermarkUtil.TextWatermarkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Watermark service for PDF and Word documents
 */
@Service
public class WatermarkService {

    private static final Logger log = LoggerFactory.getLogger(WatermarkService.class);

    /**
     * Add text watermark to PDF
     */
    public FileResponse addTextWatermarkToPdf(String sourcePath, String targetPath, TextWatermarkConfig config) {
        log.info("Service: Adding text watermark to PDF: {}", sourcePath);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!sourcePath.toLowerCase().endsWith(".pdf")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        boolean success = WatermarkUtil.addTextWatermarkToPdf(sourcePath, targetPath, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to add text watermark")
                    .build();
        }

        File targetFile = new File(targetPath);
        return FileResponse.builder()
                .success(true)
                .message("Text watermark added successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }

    /**
     * Add image watermark to PDF
     */
    public FileResponse addImageWatermarkToPdf(String sourcePath, String targetPath, ImageWatermarkConfig config) {
        log.info("Service: Adding image watermark to PDF: {}", sourcePath);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!sourcePath.toLowerCase().endsWith(".pdf")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        File imageFile = new File(config.getImagePath());
        if (!imageFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Watermark image does not exist: " + config.getImagePath())
                    .build();
        }

        boolean success = WatermarkUtil.addImageWatermarkToPdf(sourcePath, targetPath, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to add image watermark")
                    .build();
        }

        File targetFile = new File(targetPath);
        return FileResponse.builder()
                .success(true)
                .message("Image watermark added successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }

    /**
     * Add text watermark to Word
     */
    public FileResponse addTextWatermarkToWord(String sourcePath, String targetPath, TextWatermarkConfig config) {
        log.info("Service: Adding text watermark to Word: {}", sourcePath);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!sourcePath.toLowerCase().endsWith(".docx")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a DOCX file: " + sourcePath)
                    .build();
        }

        boolean success = WatermarkUtil.addTextWatermarkToWord(sourcePath, targetPath, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to add text watermark")
                    .build();
        }

        File targetFile = new File(targetPath);
        return FileResponse.builder()
                .success(true)
                .message("Text watermark added successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }
}
