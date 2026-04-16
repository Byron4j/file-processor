package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.intelligence.SensitiveType;
import com.fileprocessor.service.DocumentIntelligenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document intelligence REST API controller
 */
@RestController
@RequestMapping("/api/intelligence")
public class DocumentIntelligenceController {

    private static final Logger log = LoggerFactory.getLogger(DocumentIntelligenceController.class);

    @Autowired
    private DocumentIntelligenceService intelligenceService;

    /**
     * Classify document
     */
    @PostMapping("/classify")
    public ResponseEntity<FileResponse> classify(
            @RequestBody ClassifyRequest request) {
        log.info("REST request to classify document: {}", request.getFilePath());

        FileResponse response = intelligenceService.classify(request.getFilePath());
        return ResponseEntity.ok(response);
    }

    /**
     * Detect sensitive information
     */
    @PostMapping("/sensitive-detect")
    public ResponseEntity<FileResponse> detectSensitive(
            @RequestBody SensitiveDetectRequest request) {
        log.info("REST request to detect sensitive info: {}", request.getFilePath());

        List<SensitiveType> types = null;
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            types = request.getTypes().stream()
                    .map(SensitiveType::fromCode)
                    .filter(t -> t != null)
                    .collect(Collectors.toList());
        }

        FileResponse response = intelligenceService.detectSensitiveInfo(request.getFilePath(), types);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract keywords
     */
    @PostMapping("/extract-keywords")
    public ResponseEntity<FileResponse> extractKeywords(
            @RequestBody KeywordRequest request) {
        log.info("REST request to extract keywords: {}", request.getFilePath());

        FileResponse response = intelligenceService.extractKeywords(
                request.getFilePath(),
                request.getTopN() != null ? request.getTopN() : 10
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Generate document summary
     */
    @PostMapping("/summarize")
    public ResponseEntity<FileResponse> summarize(
            @RequestBody SummarizeRequest request) {
        log.info("REST request to summarize document: {}", request.getFilePath());

        FileResponse response = intelligenceService.summarize(
                request.getFilePath(),
                request.getMaxLength() != null ? request.getMaxLength() : 500,
                request.getStyle() != null ? request.getStyle() : "concise"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Ask question about document
     */
    @PostMapping("/ask")
    public ResponseEntity<FileResponse> ask(
            @RequestBody AskRequest request) {
        log.info("REST request to ask question: {}", request.getFilePath());

        FileResponse response = intelligenceService.ask(
                request.getFilePath(),
                request.getQuestion()
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class ClassifyRequest {
        private String filePath;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }

    public static class SensitiveDetectRequest {
        private String filePath;
        private List<String> types;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
    }

    public static class KeywordRequest {
        private String filePath;
        private Integer topN;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Integer getTopN() { return topN; }
        public void setTopN(Integer topN) { this.topN = topN; }
    }

    public static class SummarizeRequest {
        private String filePath;
        private Integer maxLength;
        private String style;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
    }

    public static class AskRequest {
        private String filePath;
        private String question;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }
}
