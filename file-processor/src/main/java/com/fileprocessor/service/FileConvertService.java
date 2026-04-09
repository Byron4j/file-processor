package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.DocConverter;
import com.fileprocessor.util.ZipProcessor;
import com.fileprocessor.util.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileConvertService {

    private static final Logger log = LoggerFactory.getLogger(FileConvertService.class);

    /**
     * Convert DOC file to DOCX format
     */
    public FileResponse convertDocToDocx(String sourcePath, String targetPath) {
        log.info("Service: Converting DOC to DOCX - {} to {}", sourcePath, targetPath);

        // Validate source file exists
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file extension
        if (!sourcePath.toLowerCase().endsWith(".doc")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file must be a .doc file")
                    .build();
        }

        // Ensure target has .docx extension
        if (!targetPath.toLowerCase().endsWith(".docx")) {
            targetPath = targetPath + ".docx";
        }

        // Create target directory if not exists
        File targetFile = new File(targetPath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Perform conversion
        boolean success = DocConverter.convertDocToDocx(sourcePath, targetPath);

        if (success) {
            long fileSize = targetFile.length();
            return FileResponse.builder()
                    .success(true)
                    .message("DOC to DOCX conversion successful")
                    .filePath(targetPath)
                    .fileSize(fileSize)
                    .build();
        } else {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to convert DOC to DOCX")
                    .build();
        }
    }

    /**
     * Extract index.html from ZIP and save as text file
     */
    public FileResponse extractIndexHtmlFromZip(String zipPath, String outputPath) {
        log.info("Service: Extracting index.html from ZIP - {} to {}", zipPath, outputPath);

        // Validate source file exists
        File zipFile = new File(zipPath);
        if (!zipFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("ZIP file does not exist: " + zipPath)
                    .build();
        }

        // Validate file extension
        if (!zipPath.toLowerCase().endsWith(".zip")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file must be a .zip file")
                    .build();
        }

        // Create output directory if not exists
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Perform extraction
        boolean success = ZipProcessor.extractIndexHtmlToText(zipPath, outputPath);

        if (success) {
            long fileSize = outputFile.length();
            return FileResponse.builder()
                    .success(true)
                    .message("index.html extracted successfully")
                    .filePath(outputPath)
                    .fileSize(fileSize)
                    .build();
        } else {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract index.html from ZIP")
                    .build();
        }
    }

    /**
     * Get file information
     */
    public FileResponse getFileInfo(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("File information retrieved")
                .filePath(filePath)
                .fileSize(file.length())
                .build();
    }

    /**
     * Extract text from document (DOC, DOCX, PDF, PPT, PPTX) and save as text file
     */
    public FileResponse extractTextFromDocument(String sourcePath, String outputPath) {
        log.info("Service: Extracting text from document - {} to {}", sourcePath, outputPath);

        // Validate source file exists
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate file format
        if (!TextExtractor.isSupportedFormat(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported file format. Supported formats: DOC, DOCX, PDF, PPT, PPTX")
                    .build();
        }

        // Ensure target has .txt extension
        if (!outputPath.toLowerCase().endsWith(".txt")) {
            outputPath = outputPath + ".txt";
        }

        // Create output directory if not exists
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Perform extraction
        boolean success = TextExtractor.extractTextToFile(sourcePath, outputPath);

        if (success) {
            long fileSize = outputFile.length();
            return FileResponse.builder()
                    .success(true)
                    .message("Text extracted successfully")
                    .filePath(outputPath)
                    .fileSize(fileSize)
                    .build();
        } else {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract text from document")
                    .build();
        }
    }
}
