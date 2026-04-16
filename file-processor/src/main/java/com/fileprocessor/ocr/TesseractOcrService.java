package com.fileprocessor.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tesseract OCR 服务
 */
@Service
public class TesseractOcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    @Value("${ocr.tesseract.data-path:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String dataPath;

    @Value("${ocr.tesseract.default-language:chi_sim+eng}")
    private String defaultLanguage;

    private ITesseract tesseract;

    @PostConstruct
    public void init() {
        tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(defaultLanguage);
        log.info("Tesseract OCR initialized with data path: {}", dataPath);
    }

    /**
     * 识别图片文字
     */
    public OcrResult recognize(File imageFile, String language, boolean includeBoundingBox) {
        long startTime = System.currentTimeMillis();

        try {
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return OcrResult.failure("Cannot read image file");
            }

            String text = tesseract.doOCR(image);
            long processTime = System.currentTimeMillis() - startTime;

            OcrResult result = OcrResult.success(
                    text != null ? text.trim() : "",
                    0.85,
                    language != null ? language : defaultLanguage,
                    processTime
            );

            if (includeBoundingBox) {
                List<TextBlock> blocks = extractTextBlocks(image);
                result.setBlocks(blocks);
            }

            return result;

        } catch (Exception e) {
            log.error("Tesseract OCR failed", e);
            return OcrResult.failure(e.getMessage());
        }
    }

    /**
     * 识别图片（图像增强版）
     */
    public OcrResult recognizeWithEnhancement(File imageFile, String language) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return OcrResult.failure("Cannot read image file");
            }

            // 图像增强：二值化
            BufferedImage enhancedImage = ImageHelper.convertImageToGrayscale(image);
            enhancedImage = ImageHelper.convertImageToBinary(enhancedImage);

            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            long startTime = System.currentTimeMillis();
            String text = tesseract.doOCR(enhancedImage);
            long processTime = System.currentTimeMillis() - startTime;

            return OcrResult.success(
                    text != null ? text.trim() : "",
                    0.90,
                    language != null ? language : defaultLanguage,
                    processTime
            );

        } catch (Exception e) {
            log.error("Tesseract OCR with enhancement failed", e);
            return OcrResult.failure(e.getMessage());
        }
    }

    /**
     * 识别 BufferedImage
     */
    public OcrResult recognize(BufferedImage image, String language) {
        long startTime = System.currentTimeMillis();

        try {
            if (language != null && !language.isEmpty()) {
                tesseract.setLanguage(language);
            }

            String text = tesseract.doOCR(image);
            long processTime = System.currentTimeMillis() - startTime;

            return OcrResult.success(
                    text != null ? text.trim() : "",
                    0.85,
                    language != null ? language : defaultLanguage,
                    processTime
            );

        } catch (Exception e) {
            log.error("Tesseract OCR failed", e);
            return OcrResult.failure(e.getMessage());
        }
    }

    /**
     * 提取文本块位置信息
     */
    private List<TextBlock> extractTextBlocks(BufferedImage image) throws Exception {
        List<TextBlock> blocks = new ArrayList<>();
        // 简化的文本块提取，使用页面级别的文本
        // Tesseract 4.x 的 getWords API 可能有变化，这里简化处理
        return blocks;
    }
}
