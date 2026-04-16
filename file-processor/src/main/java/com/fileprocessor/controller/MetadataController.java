package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.MetadataService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * File metadata management REST API controller
 */
@RestController
@RequestMapping("/api/files")
public class MetadataController {

    private static final Logger log = LoggerFactory.getLogger(MetadataController.class);

    @Autowired
    private MetadataService metadataService;

    /**
     * Register file metadata
     */
    @PostMapping("/register")
    public ResponseEntity<FileResponse> registerFile(
            @RequestBody @Valid MetadataService.RegisterRequest request) {
        log.info("REST request to register file: {}", request.getOriginalName());

        FileResponse response = metadataService.registerFile(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get file metadata
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponse> getFile(
            @PathVariable String fileId) {
        log.info("REST request to get file: {}", fileId);

        FileResponse response = metadataService.getFile(fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * List files
     */
    @GetMapping
    public ResponseEntity<FileResponse> listFiles(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String extension,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        log.info("REST request to list files: categoryId={}, extension={}", categoryId, extension);

        MetadataService.ListRequest request = new MetadataService.ListRequest();
        request.setCategoryId(categoryId);
        request.setExtension(extension);
        request.setStatus(status);
        request.setStorageType(storageType);
        request.setTag(tag);
        request.setPage(page);
        request.setSize(size);

        FileResponse response = metadataService.listFiles(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update file tags
     */
    @PostMapping("/{fileId}/tags")
    public ResponseEntity<FileResponse> updateTags(
            @PathVariable String fileId,
            @RequestBody Map<String, List<String>> request) {
        log.info("REST request to update tags for file: {}", fileId);

        List<String> tags = request.get("tags");
        FileResponse response = metadataService.updateTags(fileId, tags);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete file (soft delete)
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<FileResponse> deleteFile(
            @PathVariable String fileId) {
        log.info("REST request to delete file: {}", fileId);

        FileResponse response = metadataService.deleteFile(fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get file categories
     */
    @GetMapping("/categories")
    public ResponseEntity<FileResponse> getCategories() {
        log.info("REST request to get file categories");

        FileResponse response = metadataService.getCategories();
        return ResponseEntity.ok(response);
    }

    /**
     * Get file statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<FileResponse> getStatistics() {
        log.info("REST request to get file statistics");

        FileResponse response = metadataService.getStatistics();
        return ResponseEntity.ok(response);
    }
}
