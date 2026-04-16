package com.fileprocessor.intelligence;

/**
 * Document type enumeration for classification
 */
public enum DocumentType {
    CONTRACT("合同", "contract"),
    INVOICE("发票", "invoice"),
    REPORT("报告", "report"),
    RESUME("简历", "resume"),
    LETTER("信函", "letter"),
    AGREEMENT("协议", "agreement"),
    CERTIFICATE("证书", "certificate"),
    MANUAL("手册", "manual"),
    PROPOSAL("提案", "proposal"),
    OTHER("其他", "other");

    private final String chineseName;
    private final String englishName;

    DocumentType(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public static DocumentType fromName(String name) {
        for (DocumentType type : values()) {
            if (type.chineseName.equals(name) || type.englishName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return OTHER;
    }
}
