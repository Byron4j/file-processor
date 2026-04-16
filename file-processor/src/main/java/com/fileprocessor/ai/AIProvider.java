package com.fileprocessor.ai;

/**
 * AI provider enumeration
 */
public enum AIProvider {
    CLAUDE("Claude", "anthropic.claude"),
    OPENAI("OpenAI", "openai.gpt"),
    LOCAL("Local", "local");

    private final String displayName;
    private final String code;

    AIProvider(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public static AIProvider fromCode(String code) {
        for (AIProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code) || provider.name().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return CLAUDE;
    }
}
