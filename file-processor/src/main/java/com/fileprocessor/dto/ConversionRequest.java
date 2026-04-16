package com.fileprocessor.dto;

/**
 * 文档转换请求
 */
public class ConversionRequest {

    private String sourcePath;
    private String targetPath;
    private String sourceFormat;
    private String targetFormat;

    // PDF 转 Word 选项
    private Boolean preserveFormatting = true;
    private Boolean extractImages = true;

    // PDF 转 Excel 选项
    private Boolean detectTables = true;
    private Boolean ocrEnabled = false;

    // PPT 转 PDF 选项
    private String quality = "high";
    private Boolean includeNotes = false;

    // Word 转 PDF 选项
    private String pdfStandard = "PDF/A-1b";

    // Excel 转 PDF 选项
    private Integer sheetIndex = 0;
    private Boolean fitToPage = true;
    private String orientation = "portrait";

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat; }
    public String getTargetFormat() { return targetFormat; }
    public void setTargetFormat(String targetFormat) { this.targetFormat = targetFormat; }
    public Boolean getPreserveFormatting() { return preserveFormatting; }
    public void setPreserveFormatting(Boolean preserveFormatting) { this.preserveFormatting = preserveFormatting; }
    public Boolean getExtractImages() { return extractImages; }
    public void setExtractImages(Boolean extractImages) { this.extractImages = extractImages; }
    public Boolean getDetectTables() { return detectTables; }
    public void setDetectTables(Boolean detectTables) { this.detectTables = detectTables; }
    public Boolean getOcrEnabled() { return ocrEnabled; }
    public void setOcrEnabled(Boolean ocrEnabled) { this.ocrEnabled = ocrEnabled; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public Boolean getIncludeNotes() { return includeNotes; }
    public void setIncludeNotes(Boolean includeNotes) { this.includeNotes = includeNotes; }
    public String getPdfStandard() { return pdfStandard; }
    public void setPdfStandard(String pdfStandard) { this.pdfStandard = pdfStandard; }
    public Integer getSheetIndex() { return sheetIndex; }
    public void setSheetIndex(Integer sheetIndex) { this.sheetIndex = sheetIndex; }
    public Boolean getFitToPage() { return fitToPage; }
    public void setFitToPage(Boolean fitToPage) { this.fitToPage = fitToPage; }
    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
}
