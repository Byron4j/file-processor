package com.fileprocessor.dto;

/**
 * OCR 请求
 */
public class OcrRequest {

    /**
     * 语言: eng, chi_sim, chi_tra, jpn
     */
    private String language = "chi_sim";

    /**
     * 是否图像增强
     */
    private Boolean enhance = false;

    /**
     * OCR 引擎: tesseract, paddle
     */
    private String engine = "paddle";

    /**
     * 是否返回文本块位置信息
     */
    private Boolean includeBoundingBox = true;

    // Getters and Setters

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getEnhance() {
        return enhance;
    }

    public void setEnhance(Boolean enhance) {
        this.enhance = enhance;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public Boolean getIncludeBoundingBox() {
        return includeBoundingBox;
    }

    public void setIncludeBoundingBox(Boolean includeBoundingBox) {
        this.includeBoundingBox = includeBoundingBox;
    }
}
