package com.fileprocessor.service;

import com.fileprocessor.dto.ConversionRequest;
import com.fileprocessor.dto.FileResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 文档转换服务 - PDF/Office 格式互转
 */
@Service
public class DocumentConversionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentConversionService.class);

    @Value("${file.output.path:./outputs}")
    private String outputPath;

    @Value("${conversion.libreoffice.path:/usr/lib/libreoffice}")
    private String libreOfficePath;

    /**
     * PDF 转 Word
     */
    public FileResponse pdfToWord(String sourcePath, String targetPath, ConversionRequest options) {
        log.info("Converting PDF to Word: {}", sourcePath);

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath(sourcePath, ".docx");
            }

            try (PDDocument pdfDoc = Loader.loadPDF(sourceFile);
                 XWPFDocument wordDoc = new XWPFDocument()) {

                PDFTextStripper stripper = new PDFTextStripper();
                int pageCount = pdfDoc.getNumberOfPages();

                for (int i = 1; i <= pageCount; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String text = stripper.getText(pdfDoc);

                    // 创建段落
                    XWPFParagraph paragraph = wordDoc.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(text);
                }

                // 保存 Word 文件
                File outputFile = new File(targetPath);
                outputFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    wordDoc.write(fos);
                }

                return FileResponse.builder()
                        .success(true)
                        .message("PDF to Word conversion completed")
                        .filePath(targetPath)
                        .fileSize(outputFile.length())
                        .data(Map.of(
                                "pagesConverted", pageCount,
                                "preserveFormatting", options != null && Boolean.TRUE.equals(options.getPreserveFormatting())
                        ))
                        .build();
            }

        } catch (Exception e) {
            log.error("PDF to Word conversion failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Conversion failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Word 转 PDF - 使用 LibreOffice 命令行
     */
    public FileResponse wordToPdf(String sourcePath, String targetPath, ConversionRequest options) {
        log.info("Converting Word to PDF: {}", sourcePath);
        return convertWithLibreOffice(sourcePath, targetPath, "pdf", options);
    }

    /**
     * PPT 转 PDF
     */
    public FileResponse pptToPdf(String sourcePath, String targetPath, ConversionRequest options) {
        log.info("Converting PPT to PDF: {}", sourcePath);

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath(sourcePath, ".pdf");
            }

            try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(sourceFile.toPath()));
                 PDDocument pdfDoc = new PDDocument()) {

                Dimension slideSize = ppt.getPageSize();
                int slideCount = ppt.getSlides().size();

                for (XSLFSlide slide : ppt.getSlides()) {
                    // 创建 PDF 页面
                    PDRectangle pageSize = new PDRectangle(
                            (float) slideSize.getWidth(),
                            (float) slideSize.getHeight()
                    );
                    PDPage pdfPage = new PDPage(pageSize);
                    pdfDoc.addPage(pdfPage);

                    // 渲染幻灯片为图片
                    BufferedImage image = new BufferedImage(
                            (int) slideSize.getWidth(),
                            (int) slideSize.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );
                    Graphics2D graphics = image.createGraphics();
                    graphics.setPaint(Color.WHITE);
                    graphics.fill(new Rectangle2D.Float(0, 0, slideSize.width, slideSize.height));
                    slide.draw(graphics);
                    graphics.dispose();

                    // 将图片添加到 PDF
                    PDImageXObject pdImage = JPEGFactory.createFromImage(pdfDoc, image, 0.9f);
                    PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, pdfPage);
                    contentStream.drawImage(pdImage, 0, 0, pageSize.getWidth(), pageSize.getHeight());
                    contentStream.close();
                }

                // 保存 PDF
                File outputFile = new File(targetPath);
                outputFile.getParentFile().mkdirs();
                pdfDoc.save(outputFile);

                return FileResponse.builder()
                        .success(true)
                        .message("PPT to PDF conversion completed")
                        .filePath(targetPath)
                        .fileSize(outputFile.length())
                        .data(Map.of(
                                "slidesConverted", slideCount,
                                "quality", options != null ? options.getQuality() : "high"
                        ))
                        .build();
            }

        } catch (Exception e) {
            log.error("PPT to PDF conversion failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Conversion failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Excel 转 PDF
     */
    public FileResponse excelToPdf(String sourcePath, String targetPath, ConversionRequest options) {
        log.info("Converting Excel to PDF: {}", sourcePath);

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = generateOutputPath(sourcePath, ".pdf");
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(sourceFile.toPath()));
                 PDDocument pdfDoc = new PDDocument()) {

                int sheetIndex = options != null && options.getSheetIndex() != null ? options.getSheetIndex() : 0;
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);

                // 创建 PDF 页面
                PDPage page = new PDPage(PDRectangle.A4);
                pdfDoc.addPage(page);

                PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

                // 简单的表格转换
                int lastRow = sheet.getLastRowNum();
                float yPosition = page.getMediaBox().getHeight() - 50;
                float rowHeight = 15;

                for (int i = 0; i <= Math.min(lastRow, 50); i++) { // 限制处理前50行
                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                    if (row != null) {
                        StringBuilder rowText = new StringBuilder();
                        for (int j = 0; j < row.getLastCellNum(); j++) {
                            org.apache.poi.ss.usermodel.Cell cell = row.getCell(j);
                            if (cell != null) {
                                rowText.append(cell.toString()).append(" | ");
                            }
                        }

                        contentStream.beginText();
                        contentStream.newLineAtOffset(50, yPosition);
                        String text = rowText.toString();
                        contentStream.showText(text.length() > 100 ? text.substring(0, 100) : text);
                        contentStream.endText();

                        yPosition -= rowHeight;

                        // 分页
                        if (yPosition < 50) {
                            contentStream.close();
                            page = new PDPage(PDRectangle.A4);
                            pdfDoc.addPage(page);
                            contentStream = new PDPageContentStream(pdfDoc, page);
                            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                            yPosition = page.getMediaBox().getHeight() - 50;
                        }
                    }
                }

                contentStream.close();

                // 保存 PDF
                File outputFile = new File(targetPath);
                outputFile.getParentFile().mkdirs();
                pdfDoc.save(outputFile);

                return FileResponse.builder()
                        .success(true)
                        .message("Excel to PDF conversion completed")
                        .filePath(targetPath)
                        .fileSize(outputFile.length())
                        .data(Map.of(
                                "sheetIndex", sheetIndex,
                                "rowsProcessed", Math.min(lastRow + 1, 50)
                        ))
                        .build();
            }

        } catch (Exception e) {
            log.error("Excel to PDF conversion failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Conversion failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用 LibreOffice 进行转换
     */
    private FileResponse convertWithLibreOffice(String sourcePath, String targetPath, String format, ConversionRequest options) {
        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            // 生成输出目录
            Path outputDir = Paths.get(outputPath, "conversions");
            Files.createDirectories(outputDir);

            // 构建命令
            String[] cmd = {
                    "soffice",
                    "--headless",
                    "--convert-to", format,
                    "--outdir", outputDir.toString(),
                    sourcePath
            };

            Process process = Runtime.getRuntime().exec(cmd);
            boolean completed = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return FileResponse.builder()
                        .success(false)
                        .message("Conversion timeout")
                        .build();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return FileResponse.builder()
                        .success(false)
                        .message("LibreOffice conversion failed with exit code: " + exitCode)
                        .build();
            }

            // 查找生成的文件
            String baseName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
            String generatedFileName = baseName + "." + format;
            Path generatedPath = outputDir.resolve(generatedFileName);

            if (!Files.exists(generatedPath)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Generated file not found")
                        .build();
            }

            // 如果指定了目标路径，移动文件
            if (targetPath != null && !targetPath.isEmpty()) {
                Path target = Paths.get(targetPath);
                Files.createDirectories(target.getParent());
                Files.move(generatedPath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                generatedPath = target;
            }

            File outputFile = generatedPath.toFile();
            return FileResponse.builder()
                    .success(true)
                    .message("Conversion completed")
                    .filePath(outputFile.getAbsolutePath())
                    .fileSize(outputFile.length())
                    .build();

        } catch (Exception e) {
            log.error("LibreOffice conversion failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Conversion failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 通用文档转换
     */
    public FileResponse convert(String sourcePath, String targetPath, String sourceFormat, String targetFormat, ConversionRequest options) {
        sourceFormat = sourceFormat.toLowerCase();
        targetFormat = targetFormat.toLowerCase();

        return switch (sourceFormat + "-" + targetFormat) {
            case "pdf-docx", "pdf-doc" -> pdfToWord(sourcePath, targetPath, options);
            case "docx-pdf", "doc-pdf" -> wordToPdf(sourcePath, targetPath, options);
            case "pptx-pdf", "ppt-pdf" -> pptToPdf(sourcePath, targetPath, options);
            case "xlsx-pdf", "xls-pdf" -> excelToPdf(sourcePath, targetPath, options);
            default -> FileResponse.builder()
                    .success(false)
                    .message("Unsupported conversion: " + sourceFormat + " to " + targetFormat)
                    .build();
        };
    }

    /**
     * 生成输出路径
     */
    private String generateOutputPath(String sourcePath, String newExtension) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = new File(sourcePath).getName();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
        return String.format("%s/%s/%s%s", outputPath, datePath, baseName, newExtension);
    }
}
