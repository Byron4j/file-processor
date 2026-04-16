package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.ExcelProcessor;
import com.fileprocessor.util.ExcelProcessor.ExcelContent;
import com.fileprocessor.util.ExcelProcessor.ExcelToJsonConfig;
import com.fileprocessor.util.ExcelProcessor.SheetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Excel processing service
 */
@Service
public class ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelService.class);

    /**
     * Extract text from Excel file
     */
    public FileResponse extractText(String sourcePath, int sheetIndex) {
        log.info("Service: Extracting text from Excel: {}, sheet: {}", sourcePath, sheetIndex);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file format
        if (!ExcelProcessor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported file format. Supported: XLS, XLSX")
                    .build();
        }

        // Extract text
        ExcelContent content = ExcelProcessor.extractText(sourcePath, sheetIndex);

        if (content == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract text from Excel")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Text extracted successfully")
                .filePath(sourcePath)
                .data(Map.of(
                        "sheetName", content.getSheetName(),
                        "rowCount", content.getRowCount(),
                        "columnCount", content.getColumnCount(),
                        "content", content.getContent()
                ))
                .build();
    }

    /**
     * Convert Excel to CSV
     */
    public FileResponse convertToCsv(String sourcePath, String targetPath,
                                     int sheetIndex, char delimiter) {
        log.info("Service: Converting Excel to CSV: {} -> {}", sourcePath, targetPath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file format
        if (!ExcelProcessor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported file format. Supported: XLS, XLSX")
                    .build();
        }

        // Perform conversion
        boolean success = ExcelProcessor.convertToCsv(sourcePath, targetPath, sheetIndex, delimiter);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to convert Excel to CSV")
                    .build();
        }

        // Get output file size
        File targetFile = new File(targetPath);
        long fileSize = targetFile.exists() ? targetFile.length() : 0;

        return FileResponse.builder()
                .success(true)
                .message("Excel converted to CSV successfully")
                .filePath(targetPath)
                .fileSize(fileSize)
                .build();
    }

    /**
     * Convert Excel to JSON
     */
    public FileResponse convertToJson(String sourcePath, String targetPath,
                                      ExcelToJsonConfig config) {
        log.info("Service: Converting Excel to JSON: {} -> {}", sourcePath, targetPath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file format
        if (!ExcelProcessor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported file format. Supported: XLS, XLSX")
                    .build();
        }

        // Perform conversion
        boolean success = ExcelProcessor.convertToJson(sourcePath, targetPath, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to convert Excel to JSON")
                    .build();
        }

        // Get output file size
        File targetFile = new File(targetPath);
        long fileSize = targetFile.exists() ? targetFile.length() : 0;

        return FileResponse.builder()
                .success(true)
                .message("Excel converted to JSON successfully")
                .filePath(targetPath)
                .fileSize(fileSize)
                .build();
    }

    /**
     * Get Excel sheet information
     */
    public FileResponse getSheetInfo(String sourcePath) {
        log.info("Service: Getting sheet info from: {}", sourcePath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file format
        if (!ExcelProcessor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported file format. Supported: XLS, XLSX")
                    .build();
        }

        // Get sheet info
        List<SheetInfo> sheets = ExcelProcessor.getSheetInfo(sourcePath);

        if (sheets == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get sheet information")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Sheet information retrieved")
                .filePath(sourcePath)
                .data(Map.of("sheets", sheets))
                .build();
    }
}
