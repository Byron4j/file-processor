package com.fileprocessor.ocr;

/**
 * OCR 文本块 - 包含文字、置信度和位置
 */
public class TextBlock {

    private String text;
    private double confidence;
    private BoundingBox bbox;

    public TextBlock() {}

    public TextBlock(String text, double confidence, BoundingBox bbox) {
        this.text = text;
        this.confidence = confidence;
        this.bbox = bbox;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public BoundingBox getBbox() { return bbox; }
    public void setBbox(BoundingBox bbox) { this.bbox = bbox; }

    /**
     * 边界框
     */
    public static class BoundingBox {
        private int x;
        private int y;
        private int width;
        private int height;

        public BoundingBox() {}

        public BoundingBox(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
    }
}
