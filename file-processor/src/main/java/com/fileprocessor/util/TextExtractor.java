package com.fileprocessor.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Text extractor utility for various document formats
 */
public class TextExtractor {

    private static final Logger log = LoggerFactory.getLogger(TextExtractor.class);

    /**
     * Extract text from file based on extension and save to txt file
     *
     * @param sourcePath Source file path
     * @param outputPath Output txt file path
     * @return true if extraction successful
     */
    public static boolean extractTextToFile(String sourcePath, String outputPath) {
        String extension = getFileExtension(sourcePath).toLowerCase();

        try {
            String text = switch (extension) {
                case "doc" -> extractFromDoc(sourcePath);
                case "docx" -> extractFromDocx(sourcePath);
                case "pdf" -> extractFromPdf(sourcePath);
                case "ppt" -> extractFromPpt(sourcePath);
                case "pptx" -> extractFromPptx(sourcePath);
                default -> throw new IllegalArgumentException("Unsupported file format: " + extension);
            };

            // Write to output file
            writeTextToFile(text, outputPath);
            log.info("Successfully extracted text from {} to {}", sourcePath, outputPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to extract text from {}: {}", sourcePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract text from file and return as string
     *
     * @param sourcePath Source file path
     * @return Extracted text, or null if extraction failed
     */
    public static String extractText(String sourcePath) {
        String extension = getFileExtension(sourcePath).toLowerCase();

        try {
            return switch (extension) {
                case "doc" -> extractFromDoc(sourcePath);
                case "docx" -> extractFromDocx(sourcePath);
                case "pdf" -> extractFromPdf(sourcePath);
                case "ppt" -> extractFromPpt(sourcePath);
                case "pptx" -> extractFromPptx(sourcePath);
                default -> throw new IllegalArgumentException("Unsupported file format: " + extension);
            };
        } catch (Exception e) {
            log.error("Failed to extract text from {}: {}", sourcePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract text from DOC file
     */
    private static String extractFromDoc(String filePath) throws IOException {
        log.info("Extracting text from DOC: {}", filePath);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {

            // Extract all text from the document
            String text = extractor.getText();
            return cleanText(text);
        }
    }

    /**
     * Extract text from DOCX file
     */
    private static String extractFromDocx(String filePath) throws IOException {
        log.info("Extracting text from DOCX: {}", filePath);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             XWPFDocument docx = new XWPFDocument(is)) {

            StringBuilder text = new StringBuilder();

            // Extract from paragraphs
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.trim().isEmpty()) {
                    text.append(paraText.trim()).append("\n\n");
                }
            }

            // Extract from tables
            if (!docx.getTables().isEmpty()) {
                text.append("\n--- Tables ---\n\n");
                docx.getTables().forEach(table -> {
                    table.getRows().forEach(row -> {
                        StringBuilder rowText = new StringBuilder();
                        row.getTableCells().forEach(cell -> {
                            if (rowText.length() > 0) rowText.append(" | ");
                            rowText.append(cell.getText().trim());
                        });
                        text.append(rowText).append("\n");
                    });
                    text.append("\n");
                });
            }

            return cleanText(text.toString());
        }
    }

    /**
     * Extract text from PDF file
     */
    private static String extractFromPdf(String filePath) throws IOException {
        log.info("Extracting text from PDF: {}", filePath);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();

            // Configure stripper for better formatting
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());

            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    /**
     * Extract text from PPT file (old format)
     */
    private static String extractFromPpt(String filePath) throws IOException {
        log.info("Extracting text from PPT: {}", filePath);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             HSLFSlideShow ppt = new HSLFSlideShow(is)) {

            StringBuilder text = new StringBuilder();
            List<HSLFSlide> slides = ppt.getSlides();

            for (int i = 0; i < slides.size(); i++) {
                HSLFSlide slide = slides.get(i);
                text.append("=== Slide ").append(i + 1).append(" ===\n\n");

                // Extract text from shapes
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        String shapeText = textShape.getText();
                        if (shapeText != null && !shapeText.trim().isEmpty()) {
                            text.append(shapeText.trim()).append("\n\n");
                        }
                    }
                }
            }

            return cleanText(text.toString());
        }
    }

    /**
     * Extract text from PPTX file (new format)
     */
    private static String extractFromPptx(String filePath) throws IOException {
        log.info("Extracting text from PPTX: {}", filePath);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             XMLSlideShow pptx = new XMLSlideShow(is)) {

            StringBuilder text = new StringBuilder();
            List<XSLFSlide> slides = pptx.getSlides();

            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                text.append("=== Slide ").append(i + 1).append(" ===\n\n");

                // Extract text from shapes
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String shapeText = textShape.getText();
                        if (shapeText != null && !shapeText.trim().isEmpty()) {
                            text.append(shapeText.trim()).append("\n\n");
                        }
                    }
                }
            }

            return cleanText(text.toString());
        }
    }

    /**
     * Write text to file
     */
    private static void writeTextToFile(String text, String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(text);
        }
    }

    /**
     * Clean up extracted text
     */
    private static String cleanText(String text) {
        if (text == null) return "";

        return text
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Get file extension
     */
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Check if file format is supported for text extraction
     */
    public static boolean isSupportedFormat(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return List.of("doc", "docx", "pdf", "ppt", "pptx").contains(extension);
    }
}
