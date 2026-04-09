package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.FileConvertService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
public class FileConvertController {

    private static final Logger log = LoggerFactory.getLogger(FileConvertController.class);

    private final FileConvertService fileConvertService;

    public FileConvertController(FileConvertService fileConvertService) {
        this.fileConvertService = fileConvertService;
    }

    /**
     * Convert DOC file to DOCX format
     */
    @PostMapping("/convert/doc-to-docx")
    public ResponseEntity<FileResponse> convertDocToDocx(@Valid @RequestBody FileConvertRequest request) {
        log.info("REST request to convert DOC to DOCX: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = fileConvertService.convertDocToDocx(
                request.getSourcePath(),
                request.getTargetPath()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Extract index.html from ZIP file and save as text
     */
    @PostMapping("/extract/zip-index-html")
    public ResponseEntity<FileResponse> extractIndexHtmlFromZip(@Valid @RequestBody FileConvertRequest request) {
        log.info("REST request to extract index.html from ZIP: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = fileConvertService.extractIndexHtmlFromZip(
                request.getSourcePath(),
                request.getTargetPath()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get file information
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getFileInfo(@RequestParam String path) {
        log.info("REST request to get file info: {}", path);

        FileResponse response = fileConvertService.getFileInfo(path);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract text from document (DOC, DOCX, PDF, PPT, PPTX)
     */
    @PostMapping("/extract/text")
    public ResponseEntity<FileResponse> extractTextFromDocument(@Valid @RequestBody FileConvertRequest request) {
        log.info("REST request to extract text from document: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = fileConvertService.extractTextFromDocument(
                request.getSourcePath(),
                request.getTargetPath()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File Processor Service is running");
    }
}
