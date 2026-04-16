package com.fileprocessor.intelligence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sensitive information detector
 */
@Component
public class SensitiveInfoDetector {

    private static final Logger log = LoggerFactory.getLogger(SensitiveInfoDetector.class);

    // Regex patterns for sensitive information
    private final Map<SensitiveType, Pattern> patterns = new HashMap<>();

    public SensitiveInfoDetector() {
        initPatterns();
    }

    private void initPatterns() {
        // Chinese ID Card (15 or 18 digits)
        patterns.put(SensitiveType.ID_CARD, Pattern.compile("\\b\\d{17}[\\dXx]|\\d{15}\\b"));

        // Chinese Mobile Phone
        patterns.put(SensitiveType.PHONE, Pattern.compile("\\b1[3-9]\\d{9}\\b"));

        // Email
        patterns.put(SensitiveType.EMAIL, Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"));

        // Bank Card (16-19 digits)
        patterns.put(SensitiveType.BANK_CARD, Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9]{2})[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})\\b"));

        // Address (Chinese address keywords)
        patterns.put(SensitiveType.ADDRESS, Pattern.compile("(?:省|市|区|县|街道|路|号|栋|单元|室)[^\\n]{5,30}"));

        // Name (Chinese name - simplified detection)
        patterns.put(SensitiveType.NAME, Pattern.compile("(?:姓名|联系人|收件人|甲方|乙方)[：:]\\s*([\\u4e00-\\u9fa5]{2,4})"));
    }

    /**
     * Detect sensitive information in text
     */
    public SensitiveDetectionResult detect(String text, List<SensitiveType> types) {
        log.debug("Detecting sensitive info, types: {}", types);

        SensitiveDetectionResult result = new SensitiveDetectionResult();
        int totalFound = 0;

        for (SensitiveType type : types) {
            Pattern pattern = patterns.get(type);
            if (pattern == null) continue;

            List<SensitiveInfo> found = findMatches(text, pattern, type);
            if (!found.isEmpty()) {
                result.addResult(type, found);
                totalFound += found.size();
            }
        }

        result.setHasSensitiveInfo(totalFound > 0);
        result.setTotalFound(totalFound);

        log.info("Sensitive info detection complete: {} items found", totalFound);
        return result;
    }

    /**
     * Detect all types of sensitive information
     */
    public SensitiveDetectionResult detectAll(String text) {
        return detect(text, Arrays.asList(SensitiveType.values()));
    }

    /**
     * Mask sensitive information
     */
    public String mask(String text, SensitiveType type) {
        Pattern pattern = patterns.get(type);
        if (pattern == null) return text;

        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            String masked = maskValue(match, type);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Mask all sensitive information
     */
    public String maskAll(String text) {
        for (SensitiveType type : SensitiveType.values()) {
            text = mask(text, type);
        }
        return text;
    }

    private List<SensitiveInfo> findMatches(String text, Pattern pattern, SensitiveType type) {
        List<SensitiveInfo> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            SensitiveInfo info = new SensitiveInfo();
            info.setType(type);
            info.setValue(matcher.group());
            info.setMaskedValue(maskValue(matcher.group(), type));
            info.setPosition(matcher.start());
            results.add(info);
        }

        return results;
    }

    private String maskValue(String value, SensitiveType type) {
        if (value == null || value.length() < 4) return "****";

        switch (type) {
            case ID_CARD:
                return value.substring(0, 6) + "********" + value.substring(value.length() - 4);
            case PHONE:
                return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
            case EMAIL:
                int atIndex = value.indexOf('@');
                if (atIndex > 2) {
                    return value.substring(0, 2) + "***" + value.substring(atIndex);
                }
                return "***" + value.substring(value.length() - 4);
            case BANK_CARD:
                return "**** **** **** " + value.substring(value.length() - 4);
            case NAME:
                return value.charAt(0) + "**";
            default:
                return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
    }

    /**
     * Sensitive detection result
     */
    public static class SensitiveDetectionResult {
        private boolean hasSensitiveInfo;
        private int totalFound;
        private Map<SensitiveType, List<SensitiveInfo>> results = new HashMap<>();

        public boolean isHasSensitiveInfo() {
            return hasSensitiveInfo;
        }

        public void setHasSensitiveInfo(boolean hasSensitiveInfo) {
            this.hasSensitiveInfo = hasSensitiveInfo;
        }

        public int getTotalFound() {
            return totalFound;
        }

        public void setTotalFound(int totalFound) {
            this.totalFound = totalFound;
        }

        public Map<SensitiveType, List<SensitiveInfo>> getResults() {
            return results;
        }

        public void setResults(Map<SensitiveType, List<SensitiveInfo>> results) {
            this.results = results;
        }

        public void addResult(SensitiveType type, List<SensitiveInfo> infos) {
            results.put(type, infos);
        }
    }

    /**
     * Single sensitive info item
     */
    public static class SensitiveInfo {
        private SensitiveType type;
        private String value;
        private String maskedValue;
        private int position;

        public SensitiveType getType() {
            return type;
        }

        public void setType(SensitiveType type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getMaskedValue() {
            return maskedValue;
        }

        public void setMaskedValue(String maskedValue) {
            this.maskedValue = maskedValue;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }
}
