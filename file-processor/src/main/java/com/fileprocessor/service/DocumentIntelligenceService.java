package com.fileprocessor.service;

import com.fileprocessor.ai.LlmClient;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.intelligence.DocumentType;
import com.fileprocessor.intelligence.KeywordExtractor;
import com.fileprocessor.intelligence.SensitiveInfoDetector;
import com.fileprocessor.intelligence.SensitiveType;
import com.fileprocessor.util.TextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Document intelligence service
 */
@Service
public class DocumentIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIntelligenceService.class);

    @Autowired
    private SensitiveInfoDetector sensitiveDetector;

    @Autowired
    private KeywordExtractor keywordExtractor;

    @Autowired
    private LlmClient llmClient;

    /**
     * Classify document
     */
    public FileResponse classify(String filePath) {
        log.info("Classifying document: {}", filePath);

        try {
            // Extract text from document
            String text = TextExtractor.extractText(filePath);
            if (text == null || text.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document")
                        .build();
            }

            // Simple rule-based classification
            DocumentType primaryType = classifyByRules(text);
            double confidence = 0.85;

            List<Map<String, Object>> categories = new ArrayList<>();
            for (DocumentType type : DocumentType.values()) {
                if (type != DocumentType.OTHER) {
                    double score = calculateTypeScore(text, type);
                    if (score > 0.3) {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("name", type.getChineseName());
                        cat.put("confidence", score);
                        categories.add(cat);
                    }
                }
            }

            // Sort by confidence
            categories.sort((a, b) -> Double.compare((Double) b.get("confidence"), (Double) a.get("confidence")));

            // Get keywords
            var keywords = keywordExtractor.extract(text, 5);
            List<String> keywordList = new ArrayList<>();
            for (var kw : keywords) {
                keywordList.add(kw.getWord());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("primaryCategory", primaryType.getChineseName());
            data.put("confidence", confidence);
            data.put("categories", categories);
            data.put("keywords", keywordList);

            return FileResponse.builder()
                    .success(true)
                    .message("Document classified")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to classify document", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Classification failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Detect sensitive information
     */
    public FileResponse detectSensitiveInfo(String filePath, List<SensitiveType> types) {
        log.info("Detecting sensitive info in: {}", filePath);

        try {
            String text = TextExtractor.extractText(filePath);
            if (text == null || text.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document")
                        .build();
            }

            if (types == null || types.isEmpty()) {
                types = Arrays.asList(SensitiveType.values());
            }

            var result = sensitiveDetector.detect(text, types);

            Map<String, Object> data = new HashMap<>();
            data.put("hasSensitiveInfo", result.isHasSensitiveInfo());
            data.put("totalFound", result.getTotalFound());

            List<Map<String, Object>> results = new ArrayList<>();
            for (var entry : result.getResults().entrySet()) {
                Map<String, Object> typeResult = new HashMap<>();
                typeResult.put("type", entry.getKey().getChineseName());
                typeResult.put("count", entry.getValue().size());

                List<String> examples = new ArrayList<>();
                for (var info : entry.getValue()) {
                    examples.add(info.getMaskedValue());
                }
                typeResult.put("examples", examples);
                results.add(typeResult);
            }
            data.put("results", results);

            return FileResponse.builder()
                    .success(true)
                    .message("Sensitive info detection complete")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to detect sensitive info", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Detection failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract keywords
     */
    public FileResponse extractKeywords(String filePath, int topN) {
        log.info("Extracting keywords from: {}", filePath);

        try {
            String text = TextExtractor.extractText(filePath);
            if (text == null || text.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document")
                        .build();
            }

            var keywords = keywordExtractor.extract(text, topN);

            List<Map<String, Object>> keywordList = new ArrayList<>();
            for (var kw : keywords) {
                Map<String, Object> map = new HashMap<>();
                map.put("word", kw.getWord());
                map.put("score", kw.getScore());
                map.put("rank", kw.getRank());
                keywordList.add(map);
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Keywords extracted")
                    .data(Map.of("keywords", keywordList))
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract keywords", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Keyword extraction failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate document summary
     */
    public FileResponse summarize(String filePath, int maxLength, String style) {
        log.info("Summarizing document: {}", filePath);

        try {
            String text = TextExtractor.extractText(filePath);
            if (text == null || text.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document")
                        .build();
            }

            // Limit text length for API
            if (text.length() > 10000) {
                text = text.substring(0, 10000) + "...";
            }

            var result = llmClient.summarize(text, maxLength, style);

            if (result.getError() != null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Summarization failed: " + result.getError())
                        .build();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("summary", result.getSummary());
            data.put("keyPoints", result.getKeyPoints());
            data.put("wordCount", result.getWordCount());

            return FileResponse.builder()
                    .success(true)
                    .message("Summary generated")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to summarize document", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Summarization failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Ask question about document
     */
    public FileResponse ask(String filePath, String question) {
        log.info("Asking question about document: {} - {}", filePath, question);

        try {
            String text = TextExtractor.extractText(filePath);
            if (text == null || text.isEmpty()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document")
                        .build();
            }

            // Limit text length for API
            if (text.length() > 10000) {
                text = text.substring(0, 10000) + "...";
            }

            var result = llmClient.ask(text, question);

            if (result.getError() != null) {
                return FileResponse.builder()
                        .success(false)
                        .message("Question answering failed: " + result.getError())
                        .build();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("answer", result.getAnswer());
            data.put("confidence", result.getConfidence());

            return FileResponse.builder()
                    .success(true)
                    .message("Answer generated")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Failed to answer question", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Question answering failed: " + e.getMessage())
                    .build();
        }
    }

    private DocumentType classifyByRules(String text) {
        String lowerText = text.toLowerCase();

        // Check for contract keywords
        if (text.contains("合同") || text.contains("甲方") || text.contains("乙方") ||
            text.contains("协议") || lowerText.contains("contract") || lowerText.contains("agreement")) {
            return DocumentType.CONTRACT;
        }

        // Check for invoice keywords
        if (text.contains("发票") || text.contains("金额") || text.contains("税率") ||
            lowerText.contains("invoice") || lowerText.contains("receipt")) {
            return DocumentType.INVOICE;
        }

        // Check for resume keywords
        if (text.contains("简历") || text.contains("工作经验") || text.contains("学历") ||
            lowerText.contains("resume") || lowerText.contains("cv")) {
            return DocumentType.RESUME;
        }

        // Check for report keywords
        if (text.contains("报告") || text.contains("总结") || text.contains("分析") ||
            lowerText.contains("report") || lowerText.contains("analysis")) {
            return DocumentType.REPORT;
        }

        return DocumentType.OTHER;
    }

    private double calculateTypeScore(String text, DocumentType type) {
        // Simple scoring based on keyword frequency
        int score = 0;
        String[] keywords = getKeywordsForType(type);

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score++;
            }
        }

        return Math.min(0.95, score / (double) keywords.length);
    }

    private String[] getKeywordsForType(DocumentType type) {
        return switch (type) {
            case CONTRACT -> new String[]{"合同", "协议", "甲方", "乙方", "条款", "签字", "生效"};
            case INVOICE -> new String[]{"发票", "金额", "税率", "单价", "数量", "合计"};
            case RESUME -> new String[]{"简历", "工作经验", "学历", "技能", "联系方式"};
            case REPORT -> new String[]{"报告", "总结", "分析", "数据", "结论", "建议"};
            default -> new String[]{};
        };
    }
}
