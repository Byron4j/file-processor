package com.fileprocessor.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZIP file processor utility
 */
public class ZipProcessor {

    private static final Logger log = LoggerFactory.getLogger(ZipProcessor.class);

    /**
     * Extract index.html from ZIP file and save its text content
     *
     * @param zipPath         Source ZIP file path
     * @param outputTextPath  Output text file path for extracted content
     * @return true if extraction successful
     */
    public static boolean extractIndexHtmlToText(String zipPath, String outputTextPath) {
        log.info("Extracting index.html from ZIP: {} -> {}", zipPath, outputTextPath);

        try (ZipFile zipFile = new ZipFile(zipPath)) {

            // Find index.html entry (case-insensitive)
            ZipEntry indexEntry = findIndexHtmlEntry(zipFile);

            if (indexEntry == null) {
                log.error("index.html not found in ZIP file: {}", zipPath);
                return false;
            }

            // Extract and process HTML content
            String htmlContent = extractEntryContent(zipFile, indexEntry);

            // Convert HTML to plain text
            String plainText = convertHtmlToPlainText(htmlContent);

            // Save to output file
            writeStringToFile(plainText, outputTextPath);

            log.info("Successfully extracted index.html content to: {}", outputTextPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to extract index.html from ZIP: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract index.html content as text directly (returns the content)
     *
     * @param zipPath Source ZIP file path
     * @return Extracted text content, or null if extraction failed
     */
    public static String extractIndexHtmlContent(String zipPath) {
        log.info("Extracting index.html content from ZIP: {}", zipPath);

        try (ZipFile zipFile = new ZipFile(zipPath)) {

            ZipEntry indexEntry = findIndexHtmlEntry(zipFile);

            if (indexEntry == null) {
                log.error("index.html not found in ZIP file: {}", zipPath);
                return null;
            }

            String htmlContent = extractEntryContent(zipFile, indexEntry);
            return convertHtmlToPlainText(htmlContent);

        } catch (Exception e) {
            log.error("Failed to extract index.html from ZIP: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract index.html raw HTML content (without converting to plain text)
     *
     * @param zipPath Source ZIP file path
     * @return Raw HTML content, or null if extraction failed
     */
    public static String extractIndexHtmlRaw(String zipPath) {
        log.info("Extracting raw index.html from ZIP: {}", zipPath);

        try (ZipFile zipFile = new ZipFile(zipPath)) {

            ZipEntry indexEntry = findIndexHtmlEntry(zipFile);

            if (indexEntry == null) {
                log.error("index.html not found in ZIP file: {}", zipPath);
                return null;
            }

            return extractEntryContent(zipFile, indexEntry);

        } catch (Exception e) {
            log.error("Failed to extract index.html from ZIP: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Find index.html entry in ZIP file (case-insensitive)
     */
    private static ZipEntry findIndexHtmlEntry(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            // Match index.html at any level (case-insensitive)
            if (name.toLowerCase().endsWith("index.html")) {
                log.debug("Found index.html entry: {}", name);
                return entry;
            }
        }

        return null;
    }

    /**
     * Extract content from a ZIP entry as string
     */
    private static String extractEntryContent(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * Write string to file
     */
    private static void writeStringToFile(String content, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Convert HTML to plain text, preserving structure
     */
    private static String convertHtmlToPlainText(String html) {
        Document doc = Jsoup.parse(html);

        // Remove script and style elements
        doc.select("script, style, noscript").remove();

        // Get the body content
        Element body = doc.body();

        if (body == null) {
            return doc.text();
        }

        StringBuilder text = new StringBuilder();

        // Process elements in order
        for (Element element : body.getAllElements()) {
            String tagName = element.tagName().toLowerCase();

            switch (tagName) {
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                    String headerText = element.text().trim();
                    if (!headerText.isEmpty()) {
                        text.append("\n").append(headerText).append("\n");
                        // Add underline for headers
                        int underlineLength = Math.min(headerText.length(), 50);
                        for (int i = 0; i < underlineLength; i++) {
                            text.append("=");
                        }
                        text.append("\n");
                    }
                    break;
                case "p":
                    String paraText = element.text().trim();
                    if (!paraText.isEmpty()) {
                        text.append(paraText).append("\n\n");
                    }
                    break;
                case "br":
                    text.append("\n");
                    break;
                case "li":
                    String itemText = element.text().trim();
                    if (!itemText.isEmpty()) {
                        text.append("  - ").append(itemText).append("\n");
                    }
                    break;
                case "tr":
                    // Table row - extract cell texts
                    Elements cells = element.select("td, th");
                    if (!cells.isEmpty()) {
                        StringBuilder row = new StringBuilder();
                        for (Element cell : cells) {
                            if (row.length() > 0) row.append(" | ");
                            row.append(cell.text().trim());
                        }
                        text.append(row).append("\n");
                    }
                    break;
                case "div":
                    // Only process divs that are direct text containers
                    if (element.children().isEmpty()) {
                        String divText = element.text().trim();
                        if (!divText.isEmpty()) {
                            text.append(divText).append("\n");
                        }
                    }
                    break;
            }
        }

        // Clean up excessive whitespace
        String result = text.toString()
                .replaceAll("\n{3,}", "\n\n")
                .trim();

        // Add title if available
        String title = doc.title();
        if (!title.isEmpty()) {
            result = "Title: " + title + "\n\n" + result;
        }

        return result;
    }

    /**
     * Extract all files from ZIP to a directory
     *
     * @param zipPath     Source ZIP file path
     * @param outputDir   Output directory path
     * @return true if extraction successful
     */
    public static boolean extractAll(String zipPath, String outputDir) {
        log.info("Extracting all files from ZIP: {} -> {}", zipPath, outputDir);

        try (ZipFile zipFile = new ZipFile(zipPath)) {
            File outDir = new File(outputDir);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File outFile = new File(outDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (InputStream is = zipFile.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            log.info("Successfully extracted ZIP to: {}", outputDir);
            return true;

        } catch (Exception e) {
            log.error("Failed to extract ZIP: {}", e.getMessage(), e);
            return false;
        }
    }
}
