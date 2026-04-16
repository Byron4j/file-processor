package com.fileprocessor.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * LLM client for Claude and OpenAI
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    @Value("${ai.provider:claude}")
    private String provider;

    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.model:claude-3-sonnet-20240229}")
    private String claudeModel;

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.model:gpt-4-turbo-preview}")
    private String openaiModel;

    @Value("${ai.max-tokens:4096}")
    private int maxTokens;

    @Value("${ai.temperature:0.5}")
    private double temperature;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        log.info("LLM client initialized with provider: {}", provider);
    }

    /**
     * Send chat completion request
     */
    public LlmResponse chat(List<Message> messages, ChatConfig config) {
        AIProvider aiProvider = AIProvider.fromCode(provider);

        switch (aiProvider) {
            case CLAUDE:
                return callClaude(messages, config);
            case OPENAI:
                return callOpenAI(messages, config);
            default:
                return createFallbackResponse("AI provider not supported: " + provider);
        }
    }

    /**
     * Generate document summary
     */
    public SummaryResult summarize(String documentText, int maxLength, String style) {
        String prompt = buildSummarizePrompt(documentText, maxLength, style);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));

        LlmResponse response = chat(messages, new ChatConfig());

        SummaryResult result = new SummaryResult();
        if (response.isSuccess()) {
            result.setSummary(response.getContent());
            result.setKeyPoints(extractKeyPoints(response.getContent()));
            result.setWordCount(response.getContent().length());
        } else {
            result.setError(response.getError());
        }

        return result;
    }

    /**
     * Answer question based on document
     */
    public AnswerResult ask(String documentText, String question) {
        String prompt = buildQaPrompt(documentText, question);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));

        LlmResponse response = chat(messages, new ChatConfig());

        AnswerResult result = new AnswerResult();
        if (response.isSuccess()) {
            result.setAnswer(response.getContent());
            result.setConfidence(0.9);
        } else {
            result.setError(response.getError());
        }

        return result;
    }

    private LlmResponse callClaude(List<Message> messages, ChatConfig config) {
        try {
            String url = "https://api.anthropic.com/v1/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", claudeApiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel() != null ? config.getModel() : claudeModel);
            body.put("max_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : maxTokens);
            body.put("temperature", config.getTemperature() >= 0 ? config.getTemperature() : temperature);

            // Format messages for Claude
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                formattedMessages.add(m);
            }
            body.put("messages", formattedMessages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map content = (Map) response.getBody().get("content");
                String text = content != null ? (String) content.get("text") : "";

                LlmResponse result = new LlmResponse();
                result.setSuccess(true);
                result.setContent(text);
                result.setModel(claudeModel);
                return result;
            } else {
                return createFallbackResponse("Claude API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call Claude API", e);
            return createFallbackResponse("Claude API error: " + e.getMessage());
        }
    }

    private LlmResponse callOpenAI(List<Message> messages, ChatConfig config) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openaiApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel() != null ? config.getModel() : openaiModel);
            body.put("max_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : maxTokens);
            body.put("temperature", config.getTemperature() >= 0 ? config.getTemperature() : temperature);

            // Format messages for OpenAI
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                formattedMessages.add(m);
            }
            body.put("messages", formattedMessages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                String text = "";
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    text = message != null ? (String) message.get("content") : "";
                }

                LlmResponse result = new LlmResponse();
                result.setSuccess(true);
                result.setContent(text);
                result.setModel(openaiModel);
                return result;
            } else {
                return createFallbackResponse("OpenAI API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call OpenAI API", e);
            return createFallbackResponse("OpenAI API error: " + e.getMessage());
        }
    }

    private LlmResponse createFallbackResponse(String error) {
        LlmResponse response = new LlmResponse();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }

    private String buildSummarizePrompt(String text, int maxLength, String style) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下文档进行摘要总结。");
        sb.append("要求：");
        sb.append("1. 摘要长度控制在").append(maxLength).append("字以内\n");
        sb.append("2. 风格：").append(style).append("\n");
        sb.append("3. 保留关键信息，去除冗余内容\n");
        sb.append("4. 列出主要观点（Key Points）\n\n");
        sb.append("文档内容：\n");
        sb.append(text);
        return sb.toString();
    }

    private String buildQaPrompt(String documentText, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("基于以下文档内容回答问题。如果文档中没有相关信息，请明确说明。\n\n");
        sb.append("文档内容：\n");
        sb.append(documentText);
        sb.append("\n\n问题：").append(question);
        sb.append("\n\n请用中文回答。");
        return sb.toString();
    }

    private List<String> extractKeyPoints(String summary) {
        List<String> keyPoints = new ArrayList<>();
        String[] lines = summary.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- ") || line.startsWith("• ") || line.matches("^\\d+[.、].*")) {
                keyPoints.add(line.replaceFirst("^[-•\\d.、\\s]+", ""));
            }
        }
        return keyPoints;
    }

    // ==================== Data Classes ====================

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class ChatConfig {
        private String model;
        private int maxTokens;
        private double temperature;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
    }

    public static class LlmResponse {
        private boolean success;
        private String content;
        private String model;
        private String error;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class SummaryResult {
        private String summary;
        private List<String> keyPoints;
        private int wordCount;
        private String error;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
        public int getWordCount() { return wordCount; }
        public void setWordCount(int wordCount) { this.wordCount = wordCount; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class AnswerResult {
        private String answer;
        private double confidence;
        private String error;

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
