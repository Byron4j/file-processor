package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.FileVerifyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * File verification REST API controller for hash calculation and verification
 */
@RestController
@RequestMapping("/api/file")
public class FileVerifyController {

    private static final Logger log = LoggerFactory.getLogger(FileVerifyController.class);

    @Autowired
    private FileVerifyService fileVerifyService;

    /**
     * Calculate file hash (single algorithm)
     */
    @PostMapping("/hash")
    public ResponseEntity<FileResponse> calculateHash(
            @RequestBody @Valid HashCalculateRequest request) {
        log.info("REST request to calculate hash: {}, algorithm: {}",
                request.getFilePath(), request.getAlgorithm());

        FileResponse response = fileVerifyService.calculateHash(
                request.getFilePath(),
                request.getAlgorithm()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Calculate multiple hashes for file
     */
    @PostMapping("/hashes")
    public ResponseEntity<FileResponse> calculateHashes(
            @RequestBody @Valid HashesCalculateRequest request) {
        log.info("REST request to calculate hashes: {}, algorithms: {}",
                request.getFilePath(), request.getAlgorithms());

        FileResponse response = fileVerifyService.calculateHashes(
                request.getFilePath(),
                request.getAlgorithms()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Verify file hash
     */
    @PostMapping("/hash/verify")
    public ResponseEntity<FileResponse> verifyHash(
            @RequestBody @Valid HashVerifyRequest request) {
        log.info("REST request to verify hash: {}, algorithm: {}",
                request.getFilePath(), request.getAlgorithm());

        FileResponse response = fileVerifyService.verifyHash(
                request.getFilePath(),
                request.getAlgorithm(),
                request.getExpectedHash()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get supported hash algorithms
     */
    @GetMapping("/hash/algorithms")
    public ResponseEntity<FileResponse> getSupportedAlgorithms() {
        log.info("REST request to get supported hash algorithms");

        FileResponse response = fileVerifyService.getSupportedAlgorithms();
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class HashCalculateRequest {
        private String filePath;
        private String algorithm = "MD5";

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    public static class HashesCalculateRequest {
        private String filePath;
        private List<String> algorithms = List.of("MD5", "SHA-1", "SHA-256");

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public List<String> getAlgorithms() { return algorithms; }
        public void setAlgorithms(List<String> algorithms) { this.algorithms = algorithms; }
    }

    public static class HashVerifyRequest {
        private String filePath;
        private String algorithm = "MD5";
        private String expectedHash;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getExpectedHash() { return expectedHash; }
        public void setExpectedHash(String expectedHash) { this.expectedHash = expectedHash; }
    }
}
