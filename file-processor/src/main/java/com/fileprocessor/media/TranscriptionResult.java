package com.fileprocessor.media;

import java.util.List;

/**
 * 音频转录结果
 */
public class TranscriptionResult {

    private boolean success;
    private String text;
    private String language;
    private double duration;
    private List<Segment> segments;
    private int wordCount;
    private long processingTime;
    private String errorMessage;

    public static TranscriptionResult success(String text, String language, double duration,
                                               List<Segment> segments, long processingTime) {
        TranscriptionResult result = new TranscriptionResult();
        result.success = true;
        result.text = text;
        result.language = language;
        result.duration = duration;
        result.segments = segments;
        result.processingTime = processingTime;
        if (text != null) {
            result.wordCount = text.split("\\s+").length;
        }
        return result;
    }

    public static TranscriptionResult failure(String errorMessage) {
        TranscriptionResult result = new TranscriptionResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    public List<Segment> getSegments() { return segments; }
    public void setSegments(List<Segment> segments) { this.segments = segments; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /**
     * 转录片段
     */
    public static class Segment {
        private int id;
        private double start;
        private double end;
        private String text;
        private double confidence;

        public Segment() {}

        public Segment(int id, double start, double end, String text, double confidence) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.text = text;
            this.confidence = confidence;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public double getStart() { return start; }
        public void setStart(double start) { this.start = start; }
        public double getEnd() { return end; }
        public void setEnd(double end) { this.end = end; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
