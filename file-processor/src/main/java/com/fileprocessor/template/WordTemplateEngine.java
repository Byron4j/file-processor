package com.fileprocessor.template;

import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word template engine for document generation
 */
@Component
public class WordTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(WordTemplateEngine.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Render Word template with data
     */
    public boolean render(String templatePath, String targetPath, Map<String, Object> data) {
        log.info("Rendering Word template: {} -> {}", templatePath, targetPath);

        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Process paragraphs
            processParagraphs(document, data);

            // Process tables
            processTables(document, data);

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            targetFile.getParentFile().mkdirs();

            // Save document
            try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                document.write(fos);
            }

            log.info("Word template rendered successfully: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to render Word template", e);
            return false;
        }
    }

    /**
     * Get all placeholder names from template
     */
    public List<String> getPlaceholders(String templatePath) {
        Set<String> placeholders = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(templatePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Extract from paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                extractPlaceholders(paragraph.getText(), placeholders);
            }

            // Extract from tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            extractPlaceholders(paragraph.getText(), placeholders);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to get placeholders", e);
        }

        return new ArrayList<>(placeholders);
    }

    private void processParagraphs(XWPFDocument document, Map<String, Object> data) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceInParagraph(paragraph, data);
        }
    }

    private void processTables(XWPFDocument document, Map<String, Object> data) {
        for (XWPFTable table : document.getTables()) {
            processTable(table, data);
        }
    }

    private void processTable(XWPFTable table, Map<String, Object> data) {
        for (int i = 0; i < table.getNumberOfRows(); i++) {
            XWPFTableRow row = table.getRow(i);

            // Check if this is a dynamic row (contains ${items...} pattern)
            boolean isDynamicRow = false;
            String listKey = null;

            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text.contains("${")) {
                        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if (key.contains(".")) {
                                listKey = key.substring(0, key.indexOf("."));
                                if (data.get(listKey) instanceof List) {
                                    isDynamicRow = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (isDynamicRow) break;
            }

            if (isDynamicRow && listKey != null) {
                // Process dynamic rows
                processDynamicRows(table, row, i, listKey, data);
                return; // Dynamic row processing replaces all rows
            } else {
                // Process static row
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceInParagraph(paragraph, data);
                    }
                }
            }
        }
    }

    private void processDynamicRows(XWPFTable table, XWPFTableRow templateRow, int rowIndex,
                                    String listKey, Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get(listKey);

        if (items == null || items.isEmpty()) {
            // Remove the template row if no data
            table.removeRow(rowIndex);
            return;
        }

        // Create rows for each item
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);

            XWPFTableRow newRow;
            if (i == 0) {
                // Use the template row for the first item
                newRow = templateRow;
            } else {
                // Create new row for subsequent items
                newRow = table.createRow();
                // Copy cell structure from template
                for (int j = 0; j < templateRow.getTableCells().size(); j++) {
                    XWPFTableCell templateCell = templateRow.getCell(j);
                    XWPFTableCell newCell = newRow.getCell(j);
                    if (newCell != null && templateCell != null) {
                        newCell.removeParagraph(0);
                        XWPFParagraph newPara = newCell.addParagraph();
                        newPara.createRun().setText(templateCell.getText());
                    }
                }
            }

            // Replace placeholders in the row
            for (XWPFTableCell cell : newRow.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replaceInParagraphWithItem(paragraph, data, listKey, item);
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, Object> data) {
        String text = paragraph.getText();
        String newText = replacePlaceholders(text, data);

        if (!text.equals(newText)) {
            // Clear paragraph and add new text
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            XWPFRun run = paragraph.createRun();
            run.setText(newText);
        }
    }

    private void replaceInParagraphWithItem(XWPFParagraph paragraph, Map<String, Object> data,
                                           String listKey, Map<String, Object> item) {
        String text = paragraph.getText();

        // Replace ${listKey.field} with item value
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.startsWith(listKey + ".")) {
                String field = key.substring(listKey.length() + 1);
                Object value = item.getOrDefault(field, "");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            } else {
                Object value = data.getOrDefault(key, "");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(sb);

        String newText = sb.toString();
        if (!text.equals(newText)) {
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            XWPFRun run = paragraph.createRun();
            run.setText(newText);
        }
    }

    private String replacePlaceholders(String text, Map<String, Object> data) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private void extractPlaceholders(String text, Set<String> placeholders) {
        if (text == null) return;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            // For dynamic list items, only store the list key
            if (key.contains(".")) {
                placeholders.add(key.substring(0, key.indexOf(".")) + "[]");
            } else {
                placeholders.add(key);
            }
        }
    }
}
