package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.PreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * File preview REST API controller
 */
@RestController
@RequestMapping("/api/preview")
public class PreviewController {

    private static final Logger log = LoggerFactory.getLogger(PreviewController.class);

    @Autowired
    private PreviewService previewService;

    /**
     * Get preview information
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileResponse> getPreviewInfo(
            @PathVariable String fileId,
            @RequestParam(required = false) String type) {
        log.info("REST request to get preview info: {} (type={})", fileId, type);

        FileResponse response = previewService.getPreviewInfo(fileId, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Get preview content
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<byte[]> getPreviewContent(
            @PathVariable String fileId,
            @RequestParam(required = false) Integer page) {
        log.info("REST request to get preview content: {} (page={})", fileId, page);

        PreviewService.PreviewContent content = previewService.getPreviewContent(fileId, page);

        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(content.getContentType()));
        headers.setContentLength(content.getContent().length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content.getContent());
    }
}
