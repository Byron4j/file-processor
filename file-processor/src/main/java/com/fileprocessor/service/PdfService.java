package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.PdfEditor;
import com.fileprocessor.util.PdfEditor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * PDF processing service for merge, split, rotate, and page operations
 */
@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    /**
     * Merge multiple PDFs
     */
    public FileResponse merge(List<String> sourcePaths, String targetPath, boolean addBookmarks) {
        log.info("Service: Merging {} PDFs into: {}", sourcePaths.size(), targetPath);

        // Validate source files
        for (String sourcePath : sourcePaths) {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file does not exist: " + sourcePath)
                        .build();
            }
            if (!PdfEditor.isPdf(sourcePath)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Not a PDF file: " + sourcePath)
                        .build();
            }
        }

        // Perform merge
        boolean success = PdfEditor.merge(sourcePaths, targetPath, addBookmarks);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to merge PDFs")
                    .build();
        }

        // Get output file info
        File targetFile = new File(targetPath);
        PdfInfo info = PdfEditor.getInfo(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("PDFs merged successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .data(Map.of(
                        "sourceCount", sourcePaths.size(),
                        "totalPages", info != null ? info.getPageCount() : 0
                ))
                .build();
    }

    /**
     * Split PDF
     */
    public FileResponse split(String sourcePath, String outputDir, SplitConfig config) {
        log.info("Service: Splitting PDF: {} with mode: {}", sourcePath, config.getMode());

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!PdfEditor.isPdf(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        // Perform split
        List<String> outputFiles = PdfEditor.split(sourcePath, outputDir, config);

        if (outputFiles.isEmpty()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to split PDF")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("PDF split successfully into " + outputFiles.size() + " files")
                .filePath(outputDir)
                .data(Map.of(
                        "outputFiles", outputFiles,
                        "fileCount", outputFiles.size()
                ))
                .build();
    }

    /**
     * Extract specific pages
     */
    public FileResponse extractPages(String sourcePath, String targetPath, List<Integer> pages) {
        log.info("Service: Extracting pages {} from PDF: {}", pages, sourcePath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!PdfEditor.isPdf(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        // Perform extraction
        boolean success = PdfEditor.extractPages(sourcePath, targetPath, pages);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract pages")
                    .build();
        }

        File targetFile = new File(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("Pages extracted successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .data(Map.of("extractedPages", pages.size()))
                .build();
    }

    /**
     * Rotate pages
     */
    public FileResponse rotate(String sourcePath, String targetPath, List<Integer> pages, int angle) {
        log.info("Service: Rotating PDF pages by {} degrees", angle);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!PdfEditor.isPdf(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        // Perform rotation
        boolean success = PdfEditor.rotate(sourcePath, targetPath, pages, angle);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to rotate PDF")
                    .build();
        }

        File targetFile = new File(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("PDF rotated successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }

    /**
     * Delete pages
     */
    public FileResponse deletePages(String sourcePath, String targetPath, List<Integer> pages) {
        log.info("Service: Deleting pages {} from PDF", pages);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!PdfEditor.isPdf(sourcePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        // Perform deletion
        boolean success = PdfEditor.deletePages(sourcePath, targetPath, pages);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to delete pages")
                    .build();
        }

        File targetFile = new File(targetPath);

        return FileResponse.builder()
                .success(true)
                .message("Pages deleted successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }

    /**
     * Get PDF information
     */
    public FileResponse getPdfInfo(String filePath) {
        log.info("Service: Getting PDF info: {}", filePath);

        // Validate file
        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        if (!PdfEditor.isPdf(filePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + filePath)
                    .build();
        }

        // Get info
        PdfInfo info = PdfEditor.getInfo(filePath);

        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get PDF information")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("PDF information retrieved")
                .filePath(filePath)
                .data(Map.of(
                        "pageCount", info.getPageCount(),
                        "title", info.getTitle(),
                        "author", info.getAuthor(),
                        "subject", info.getSubject(),
                        "creator", info.getCreator(),
                        "producer", info.getProducer(),
                        "creationDate", info.getCreationDate(),
                        "modificationDate", info.getModificationDate(),
                        "encrypted", info.isEncrypted()
                ))
                .build();
    }
}
