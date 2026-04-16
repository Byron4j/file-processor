package com.fileprocessor.util;

import org.apache.poi.xwpf.usermodel.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Watermark utility for PDF and Word documents
 */
public class WatermarkUtil {

    private static final Logger log = LoggerFactory.getLogger(WatermarkUtil.class);

    /**
     * Text watermark configuration
     */
    public static class TextWatermarkConfig {
        private String text;
        private int fontSize = 48;
        private String color = "#FF0000";
        private float opacity = 0.3f;
        private int rotation = 45;
        private WatermarkPosition position = WatermarkPosition.CENTER;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public float getOpacity() { return opacity; }
        public void setOpacity(float opacity) { this.opacity = opacity; }
        public int getRotation() { return rotation; }
        public void setRotation(int rotation) { this.rotation = rotation; }
        public WatermarkPosition getPosition() { return position; }
        public void setPosition(WatermarkPosition position) { this.position = position; }
    }

    /**
     * Image watermark configuration
     */
    public static class ImageWatermarkConfig {
        private String imagePath;
        private float scale = 0.5f;
        private float opacity = 0.3f;
        private WatermarkPosition position = WatermarkPosition.CENTER;
        private int marginX = 50;
        private int marginY = 50;

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public float getScale() { return scale; }
        public void setScale(float scale) { this.scale = scale; }
        public float getOpacity() { return opacity; }
        public void setOpacity(float opacity) { this.opacity = opacity; }
        public WatermarkPosition getPosition() { return position; }
        public void setPosition(WatermarkPosition position) { this.position = position; }
        public int getMarginX() { return marginX; }
        public void setMarginX(int marginX) { this.marginX = marginX; }
        public int getMarginY() { return marginY; }
        public void setMarginY(int marginY) { this.marginY = marginY; }
    }

    public enum WatermarkPosition {
        CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP_CENTER, BOTTOM_CENTER, LEFT_CENTER, RIGHT_CENTER
    }

    /**
     * Add text watermark to PDF
     */
    public static boolean addTextWatermarkToPdf(String sourcePath, String targetPath, TextWatermarkConfig config) {
        log.info("Adding text watermark to PDF: {} -> {}", sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            for (PDPage page : document.getPages()) {
                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

                // Set transparency
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(config.getOpacity());
                graphicsState.setStrokingAlphaConstant(config.getOpacity());
                contentStream.setGraphicsStateParameters(graphicsState);

                // Parse color
                Color color = Color.decode(config.getColor());
                contentStream.setNonStrokingColor(color);

                // Set font
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                contentStream.setFont(font, config.getFontSize());

                // Calculate position
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float x = getXPosition(config.getPosition(), pageWidth, config.getText().length() * config.getFontSize() * 0.5f);
                float y = getYPosition(config.getPosition(), pageHeight, config.getFontSize());

                // Add text with rotation
                contentStream.beginText();
                contentStream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(config.getRotation()), x, y));
                contentStream.showText(config.getText());
                contentStream.endText();

                contentStream.close();
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);
            log.info("Successfully added text watermark to PDF: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to add text watermark to PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Add image watermark to PDF
     */
    public static boolean addImageWatermarkToPdf(String sourcePath, String targetPath, ImageWatermarkConfig config) {
        log.info("Adding image watermark to PDF: {} -> {}", sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            PDImageXObject pdImage = PDImageXObject.createFromFile(config.getImagePath(), document);

            for (PDPage page : document.getPages()) {
                PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);

                // Set transparency
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(config.getOpacity());
                graphicsState.setStrokingAlphaConstant(config.getOpacity());
                contentStream.setGraphicsStateParameters(graphicsState);

                // Calculate scaled dimensions
                float scaledWidth = pdImage.getWidth() * config.getScale();
                float scaledHeight = pdImage.getHeight() * config.getScale();

                // Calculate position
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float x = getXPosition(config.getPosition(), pageWidth, scaledWidth) + config.getMarginX();
                float y = getYPosition(config.getPosition(), pageHeight, scaledHeight) + config.getMarginY();

                // Draw image
                contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
                contentStream.close();
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);
            log.info("Successfully added image watermark to PDF: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to add image watermark to PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Add text watermark to Word document
     */
    public static boolean addTextWatermarkToWord(String sourcePath, String targetPath, TextWatermarkConfig config) {
        log.info("Adding text watermark to Word: {} -> {}", sourcePath, targetPath);

        try (FileInputStream fis = new FileInputStream(sourcePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Create header with watermark (simplified)
            // Note: Full Word watermark with rotation requires VML/DrawingML
            // This is a basic implementation adding text to header

            // Create header using correct enum type
            XWPFHeader header = document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            XWPFParagraph paragraph = header.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun run = paragraph.createRun();
            run.setText(config.getText());
            run.setFontSize(config.getFontSize());
            run.setColor(config.getColor().replace("#", ""));

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(targetPath)) {
                document.write(fos);
            }

            log.info("Successfully added text watermark to Word: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to add text watermark to Word: {}", sourcePath, e);
            return false;
        }
    }

    // ==================== Helper Methods ====================

    private static float getXPosition(WatermarkPosition position, float pageWidth, float elementWidth) {
        return switch (position) {
            case CENTER, TOP_CENTER, BOTTOM_CENTER -> (pageWidth - elementWidth) / 2;
            case TOP_RIGHT, BOTTOM_RIGHT, RIGHT_CENTER -> pageWidth - elementWidth - 50;
            default -> 50; // LEFT positions
        };
    }

    private static float getYPosition(WatermarkPosition position, float pageHeight, float elementHeight) {
        return switch (position) {
            case CENTER, LEFT_CENTER, RIGHT_CENTER -> (pageHeight - elementHeight) / 2;
            case TOP_LEFT, TOP_RIGHT, TOP_CENTER -> pageHeight - elementHeight - 50;
            default -> 50; // BOTTOM positions
        };
    }
}
