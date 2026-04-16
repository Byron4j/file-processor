package com.fileprocessor.controller;

import com.fileprocessor.dto.ConversionRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.DocumentConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文档转换控制器 - PDF/Office 格式互转
 */
@RestController
@RequestMapping("/api/convert")
public class DocumentConversionController {

    private static final Logger log = LoggerFactory.getLogger(DocumentConversionController.class);

    @Autowired
    private DocumentConversionService conversionService;

    /**
     * PDF 转 Word
     */
    @PostMapping("/pdf-to-word")
    public ResponseEntity<FileResponse> pdfToWord(@RequestBody ConversionRequest request) {
        log.info("PDF to Word request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = conversionService.pdfToWord(
                request.getSourcePath(),
                request.getTargetPath(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Word 转 PDF
     */
    @PostMapping("/word-to-pdf")
    public ResponseEntity<FileResponse> wordToPdf(@RequestBody ConversionRequest request) {
        log.info("Word to PDF request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = conversionService.wordToPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * PPT 转 PDF
     */
    @PostMapping("/ppt-to-pdf")
    public ResponseEntity<FileResponse> pptToPdf(@RequestBody ConversionRequest request) {
        log.info("PPT to PDF request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = conversionService.pptToPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Excel 转 PDF
     */
    @PostMapping("/excel-to-pdf")
    public ResponseEntity<FileResponse> excelToPdf(@RequestBody ConversionRequest request) {
        log.info("Excel to PDF request: {}", request.getSourcePath());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = conversionService.excelToPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 通用文档转换
     */
    @PostMapping("/generic")
    public ResponseEntity<FileResponse> convert(@RequestBody ConversionRequest request) {
        log.info("Generic conversion: {} -> {}", request.getSourceFormat(), request.getTargetFormat());

        if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    FileResponse.builder()
                            .success(false)
                            .message("Source path is required")
                            .build()
            );
        }

        FileResponse response = conversionService.convert(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getSourceFormat(),
                request.getTargetFormat(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 获取支持的转换格式列表
     */
    @GetMapping("/formats")
    public ResponseEntity<FileResponse> getSupportedFormats() {
        log.info("Getting supported conversion formats");

        return ResponseEntity.ok(FileResponse.builder()
                .success(true)
                .message("Supported conversion formats")
                .data(Map.of(
                        "conversions", new String[]{
                                "pdf-to-word",
                                "word-to-pdf",
                                "ppt-to-pdf",
                                "excel-to-pdf"
                        },
                        "note", "More formats coming soon"
                ))
                .build());
    }
}
