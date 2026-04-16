package com.fileprocessor.ocr;

import java.util.List;

/**
 * OCR 页面结果
 */
public class PageResult {

    private int pageNum;
    private String text;
    private List<TextBlock> blocks;
    private double confidence;

    public PageResult() {}

    public PageResult(int pageNum, String text, List<TextBlock> blocks, double confidence) {
        this.pageNum = pageNum;
        this.text = text;
        this.blocks = blocks;
        this.confidence = confidence;
    }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<TextBlock> getBlocks() { return blocks; }
    public void setBlocks(List<TextBlock> blocks) { this.blocks = blocks; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
