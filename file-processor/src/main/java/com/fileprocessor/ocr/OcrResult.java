package com.fileprocessor.ocr;

import java.util.List;

/**
 * OCR 识别结果
 */
public class OcrResult {

    private boolean success;
    private String text;
    private double confidence;
    private String language;
    private long processTime;
    private List<TextBlock> blocks;
    private List<PageResult> pages;
    private String errorMessage;

    public static OcrResult success(String text, double confidence, String language, long processTime) {
        OcrResult result = new OcrResult();
        result.success = true;
        result.text = text;
        result.confidence = confidence;
        result.language = language;
        result.processTime = processTime;
        return result;
    }

    public static OcrResult success(String text, double confidence, String language, long processTime, List<TextBlock> blocks) {
        OcrResult result = success(text, confidence, language, processTime);
        result.blocks = blocks;
        return result;
    }

    public static OcrResult failure(String errorMessage) {
        OcrResult result = new OcrResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public long getProcessTime() { return processTime; }
    public void setProcessTime(long processTime) { this.processTime = processTime; }
    public List<TextBlock> getBlocks() { return blocks; }
    public void setBlocks(List<TextBlock> blocks) { this.blocks = blocks; }
    public List<PageResult> getPages() { return pages; }
    public void setPages(List<PageResult> pages) { this.pages = pages; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
