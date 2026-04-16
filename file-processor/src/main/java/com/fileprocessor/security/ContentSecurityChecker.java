package com.fileprocessor.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ContentSecurityChecker {

    private static final Logger log = LoggerFactory.getLogger(ContentSecurityChecker.class);

    // XSS patterns
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe[^>]*>[\\s\\S]*?</iframe>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>[\\s\\S]*?</object>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        // Documents
        "txt", "doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "csv", "rtf",
        // Images
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg",
        // Audio/Video
        "mp3", "wav", "ogg", "mp4", "avi", "mov", "webm", "mkv",
        // Archives
        "zip", "rar", "7z", "tar", "gz",
        // Code
        "java", "py", "js", "html", "css", "xml", "json", "yaml", "yml", "md"
    );

    // Dangerous file extensions
    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
        "exe", "dll", "bat", "cmd", "sh", "bin", "msi", "apk", "ipa",
        "jar", "war", "ear", "jsp", "asp", "aspx", "php"
    );

    public boolean containsXss(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("XSS pattern detected: {}", pattern.pattern());
                return true;
            }
        }
        return false;
    }

    public String sanitizeXss(String content) {
        if (content == null) {
            return null;
        }

        String sanitized = content;
        for (Pattern pattern : XSS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("");
        }
        return sanitized;
    }

    public boolean isAllowedFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();

        // Check dangerous extensions first
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            log.warn("Dangerous file type detected: {}", filename);
            return false;
        }

        return ALLOWED_EXTENSIONS.contains(extension);
    }

    public boolean validateFileContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (!isAllowedFileType(filename)) {
            log.warn("File type not allowed: {}", filename);
            return false;
        }

        // Check content for text files
        String contentType = file.getContentType();
        if (contentType != null && (contentType.startsWith("text/") ||
            contentType.equals("application/json") ||
            contentType.equals("application/xml"))) {
            try {
                String content = readContent(file);
                if (containsXss(content)) {
                    log.warn("XSS content detected in file: {}", filename);
                    return false;
                }
            } catch (IOException e) {
                log.error("Failed to read file content: {}", filename, e);
                return false;
            }
        }

        return true;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private String readContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
