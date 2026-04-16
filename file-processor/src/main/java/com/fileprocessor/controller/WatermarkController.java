package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.WatermarkService;
import com.fileprocessor.util.WatermarkUtil.ImageWatermarkConfig;
import com.fileprocessor.util.WatermarkUtil.TextWatermarkConfig;
import com.fileprocessor.util.WatermarkUtil.WatermarkPosition;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Watermark REST API controller
 */
@RestController
@RequestMapping("/api/watermark")
public class WatermarkController {

    private static final Logger log = LoggerFactory.getLogger(WatermarkController.class);

    @Autowired
    private WatermarkService watermarkService;

    /**
     * Add text watermark to PDF
     */
    @PostMapping("/pdf/text")
    public ResponseEntity<FileResponse> addTextWatermarkToPdf(
            @RequestBody @Valid PdfTextWatermarkRequest request) {
        log.info("REST request to add text watermark to PDF: {}", request.getSourcePath());

        TextWatermarkConfig config = new TextWatermarkConfig();
        config.setText(request.getText());
        config.setFontSize(request.getFontSize());
        config.setColor(request.getColor());
        config.setOpacity(request.getOpacity());
        config.setRotation(request.getRotation());
        config.setPosition(request.getPosition());

        FileResponse response = watermarkService.addTextWatermarkToPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Add image watermark to PDF
     */
    @PostMapping("/pdf/image")
    public ResponseEntity<FileResponse> addImageWatermarkToPdf(
            @RequestBody @Valid PdfImageWatermarkRequest request) {
        log.info("REST request to add image watermark to PDF: {}", request.getSourcePath());

        ImageWatermarkConfig config = new ImageWatermarkConfig();
        config.setImagePath(request.getImagePath());
        config.setScale(request.getScale());
        config.setOpacity(request.getOpacity());
        config.setPosition(request.getPosition());
        config.setMarginX(request.getMarginX());
        config.setMarginY(request.getMarginY());

        FileResponse response = watermarkService.addImageWatermarkToPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Add text watermark to Word
     */
    @PostMapping("/word/text")
    public ResponseEntity<FileResponse> addTextWatermarkToWord(
            @RequestBody @Valid WordTextWatermarkRequest request) {
        log.info("REST request to add text watermark to Word: {}", request.getSourcePath());

        TextWatermarkConfig config = new TextWatermarkConfig();
        config.setText(request.getText());
        config.setFontSize(request.getFontSize());
        config.setColor(request.getColor());

        FileResponse response = watermarkService.addTextWatermarkToWord(
                request.getSourcePath(),
                request.getTargetPath(),
                config
        );

        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class PdfTextWatermarkRequest extends FileConvertRequest {
        private String text = "CONFIDENTIAL";
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

    public static class PdfImageWatermarkRequest extends FileConvertRequest {
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

    public static class WordTextWatermarkRequest extends FileConvertRequest {
        private String text = "DRAFT";
        private int fontSize = 72;
        private String color = "#CCCCCC";

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}
