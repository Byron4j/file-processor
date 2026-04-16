package com.fileprocessor.intelligence;

/**
 * Sensitive information type enumeration
 */
public enum SensitiveType {
    ID_CARD("身份证号", "id-card"),
    PHONE("手机号", "phone"),
    EMAIL("邮箱", "email"),
    BANK_CARD("银行卡号", "bank-card"),
    ADDRESS("地址", "address"),
    NAME("姓名", "name");

    private final String chineseName;
    private final String code;

    SensitiveType(String chineseName, String code) {
        this.chineseName = chineseName;
        this.code = code;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getCode() {
        return code;
    }

    public static SensitiveType fromCode(String code) {
        for (SensitiveType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
