package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.BatchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Batch processing REST API controller
 */
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private BatchService batchService;

    /**
     * Batch convert files
     */
    @PostMapping("/convert")
    public ResponseEntity<FileResponse> batchConvert(
            @RequestBody @Valid BatchService.BatchConvertRequest request) {
        log.info("REST request for batch convert: {} files to {}",
                request.getFiles().size(), request.getTargetFormat());

        FileResponse response = batchService.batchConvert(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch add watermark
     */
    @PostMapping("/watermark")
    public ResponseEntity<FileResponse> batchWatermark(
            @RequestBody @Valid BatchService.BatchWatermarkRequest request) {
        log.info("REST request for batch watermark: {} files", request.getFiles().size());

        FileResponse response = batchService.batchWatermark(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch extract text
     */
    @PostMapping("/extract")
    public ResponseEntity<FileResponse> batchExtract(
            @RequestBody @Valid BatchService.BatchExtractRequest request) {
        log.info("REST request for batch extract: {} files", request.getFiles().size());

        FileResponse response = batchService.batchExtract(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch calculate hash
     */
    @PostMapping("/hash")
    public ResponseEntity<FileResponse> batchHash(
            @RequestBody @Valid BatchService.BatchHashRequest request) {
        log.info("REST request for batch hash: {} files", request.getFiles().size());

        FileResponse response = batchService.batchHash(request);
        return ResponseEntity.ok(response);
    }
}
