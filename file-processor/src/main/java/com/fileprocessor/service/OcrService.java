package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.ocr.*;
import net.sourceforge.tess4j.util.ImageHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OCR 服务 - 整合多种 OCR 引擎
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Autowired
    private TesseractOcrService tesseractOcrService;

    @Autowired
    private PaddleOcrService paddleOcrService;

    @Value("${ocr.default-engine:paddle}")
    private String defaultEngine;

    @Value("${file.output.path:./outputs}")
    private String outputPath;

    /**
     * 图片 OCR 识别
     */
    public FileResponse extractText(MultipartFile file, String engine, String language, boolean enhance, boolean includeBoundingBox) {
        log.info("OCR extract: file={}, engine={}, language={}", file.getOriginalFilename(), engine, language);

        try {
            // 保存临时文件
            Path tempFile = Files.createTempFile("ocr_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            OcrResult result;
            OcrEngineType engineType = OcrEngineType.fromCode(engine != null ? engine : defaultEngine);

            switch (engineType) {
                case PADDLE:
                    if (paddleOcrService.isInitialized()) {
                        result = paddleOcrService.recognize(tempFile.toFile(), includeBoundingBox);
                    } else {
                        log.warn("PaddleOCR not available, fallback to Tesseract");
                        result = tesseractOcrService.recognize(tempFile.toFile(), language, includeBoundingBox);
                    }
                    break;
                case TESSERACT:
                default:
                    if (enhance) {
                        result = tesseractOcrService.recognizeWithEnhancement(tempFile.toFile(), language);
                    } else {
                        result = tesseractOcrService.recognize(tempFile.toFile(), language, includeBoundingBox);
                    }
                    break;
            }

            // 清理临时文件
            Files.deleteIfExists(tempFile);

            if (!result.isSuccess()) {
                return FileResponse.builder()
                        .success(false)
                        .message("OCR failed: " + result.getErrorMessage())
                        .build();
            }

            return FileResponse.builder()
                    .success(true)
                    .message("OCR completed")
                    .data(Map.of(
                            "text", result.getText(),
                            "confidence", result.getConfidence(),
                            "language", result.getLanguage(),
                            "processTime", result.getProcessTime(),
                            "blocks", result.getBlocks() != null ? result.getBlocks() : new ArrayList<>()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("OCR extract failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("OCR failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * PDF OCR - 扫描件转可搜索 PDF
     */
    public FileResponse convertToSearchablePdf(String sourcePath, String targetPath, String language, int dpi) {
        log.info("Converting scanned PDF to searchable: {}, language={}", sourcePath, language);

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            // 生成输出路径
            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath(sourcePath, "_searchable.pdf");
            }

            try (PDDocument sourceDoc = Loader.loadPDF(sourceFile);
                 PDDocument targetDoc = new PDDocument()) {

                PDFRenderer renderer = new PDFRenderer(sourceDoc);
                int pageCount = sourceDoc.getNumberOfPages();
                int processedPages = 0;
                double totalConfidence = 0;

                for (int i = 0; i < pageCount; i++) {
                    // 渲染页面为图片
                    BufferedImage image = renderer.renderImageWithDPI(i, dpi);

                    // OCR 识别
                    OcrResult ocrResult = tesseractOcrService.recognize(image, language);

                    if (ocrResult.isSuccess()) {
                        // 创建新页面（保持原尺寸）
                        PDPage sourcePage = sourceDoc.getPage(i);
                        PDRectangle mediaBox = sourcePage.getMediaBox();
                        PDPage newPage = new PDPage(mediaBox);
                        targetDoc.addPage(newPage);

                        // 添加图片层
                        PDPageContentStream contentStream = new PDPageContentStream(targetDoc, newPage);

                        // 添加文字层（简化实现 - 直接添加文本）
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.newLineAtOffset(50, mediaBox.getHeight() - 50);

                        String[] lines = ocrResult.getText().split("\n");
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                contentStream.showText(line.length() > 100 ? line.substring(0, 100) : line);
                                contentStream.newLineAtOffset(0, -14);
                            }
                        }

                        contentStream.endText();
                        contentStream.close();

                        totalConfidence += ocrResult.getConfidence();
                        processedPages++;
                    }
                }

                // 保存输出文件
                File outputFile = new File(targetPath);
                outputFile.getParentFile().mkdirs();
                targetDoc.save(outputFile);

                double avgConfidence = processedPages > 0 ? totalConfidence / processedPages : 0;

                return FileResponse.builder()
                        .success(true)
                        .message("PDF OCR completed")
                        .filePath(targetPath)
                        .fileSize(outputFile.length())
                        .data(Map.of(
                                "pagesProcessed", processedPages,
                                "totalPages", pageCount,
                                "textLayerAdded", processedPages > 0,
                                "ocrConfidence", avgConfidence
                        ))
                        .build();
            }

        } catch (Exception e) {
            log.error("PDF OCR failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("PDF OCR failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 批量 OCR 处理
     */
    public FileResponse batchOcr(List<MultipartFile> files, String engine, String language) {
        log.info("Batch OCR: {} files, engine={}", files.size(), engine);

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (MultipartFile file : files) {
            FileResponse response = extractText(file, engine, language, false, false);

            if (response.isSuccess()) {
                results.add(Map.of(
                        "fileName", file.getOriginalFilename(),
                        "success", true,
                        "data", response.getData()
                ));
                successCount++;
            } else {
                results.add(Map.of(
                        "fileName", file.getOriginalFilename(),
                        "success", false,
                        "error", response.getMessage()
                ));
                failCount++;
            }
        }

        return FileResponse.builder()
                .success(true)
                .message("Batch OCR completed")
                .data(Map.of(
                        "total", files.size(),
                        "success", successCount,
                        "failed", failCount,
                        "results", results
                ))
                .build();
    }

    /**
     * 生成输出路径
     */
    private String generateOutputPath(String sourcePath, String suffix) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = new File(sourcePath).getName();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
        return String.format("%s/%s/%s%s", outputPath, datePath, baseName, suffix);
    }
}
