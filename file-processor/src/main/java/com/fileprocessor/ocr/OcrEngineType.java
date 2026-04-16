package com.fileprocessor.ocr;

/**
 * OCR 引擎类型
 */
public enum OcrEngineType {
    TESSERACT("tesseract", "Tesseract OCR"),
    PADDLE("paddle", "PaddleOCR"),
    BAIDU("baidu", "Baidu OCR API"),
    TENCENT("tencent", "Tencent OCR API");

    private final String code;
    private final String name;

    OcrEngineType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static OcrEngineType fromCode(String code) {
        for (OcrEngineType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return PADDLE; // 默认使用 PaddleOCR
    }
}
