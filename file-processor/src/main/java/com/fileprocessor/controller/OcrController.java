package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.dto.OcrRequest;
import com.fileprocessor.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OCR 控制器 - 图片文字识别、PDF OCR
 */
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);

    @Autowired
    private OcrService ocrService;

    /**
     * 图片 OCR 文字提取
     */
    @PostMapping("/extract")
    public ResponseEntity<FileResponse> extractText(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute OcrRequest request) {
        log.info("OCR extract request: {}, engine={}", file.getOriginalFilename(), request.getEngine());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("File is empty")
                            .build()
            );
        }

        FileResponse response = ocrService.extractText(
                file,
                request.getEngine(),
                request.getLanguage(),
                request.getEnhance(),
                request.getIncludeBoundingBox()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * PDF OCR - 扫描件转可搜索 PDF
     */
    @PostMapping("/pdf/convert")
    public ResponseEntity<FileResponse> convertPdfToSearchable(
            @RequestBody PdfOcrRequest request) {
        log.info("PDF OCR request: {}", request.getSourcePath());

        FileResponse response = ocrService.convertToSearchablePdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getLanguage(),
                request.getDpi() != null ? request.getDpi() : 300
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 批量 OCR 处理
     */
    @PostMapping("/batch")
    public ResponseEntity<FileResponse> batchOcr(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "engine", defaultValue = "paddle") String engine,
            @RequestParam(value = "language", defaultValue = "chi_sim") String language) {
        log.info("Batch OCR request: {} files, engine={}", files.size(), engine);

        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("No files provided")
                            .build()
            );
        }

        FileResponse response = ocrService.batchOcr(files, engine, language);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class PdfOcrRequest {
        private String sourcePath;
        private String targetPath;
        private String language = "chi_sim";
        private Integer dpi = 300;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public Integer getDpi() { return dpi; }
        public void setDpi(Integer dpi) { this.dpi = dpi; }
    }
}
