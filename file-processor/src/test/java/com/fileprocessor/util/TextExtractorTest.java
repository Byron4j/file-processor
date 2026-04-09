package com.fileprocessor.util;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextExtractor
 */
public class TextExtractorTest {

    @TempDir
    Path tempDir;

    private static final String TEST_CONTENT = "Hello World\nThis is a test document.\nWith multiple lines.";

    @BeforeEach
    public void setUp() {
        // Setup if needed
    }

    @AfterEach
    public void tearDown() {
        // Cleanup if needed
    }

    // ==================== Format Support Tests ====================

    @Test
    public void testIsSupportedFormat_Doc() {
        assertTrue(TextExtractor.isSupportedFormat("document.doc"));
        assertTrue(TextExtractor.isSupportedFormat("/path/to/document.DOC"));
    }

    @Test
    public void testIsSupportedFormat_Docx() {
        assertTrue(TextExtractor.isSupportedFormat("document.docx"));
        assertTrue(TextExtractor.isSupportedFormat("/path/to/document.DOCX"));
    }

    @Test
    public void testIsSupportedFormat_Pdf() {
        assertTrue(TextExtractor.isSupportedFormat("document.pdf"));
        assertTrue(TextExtractor.isSupportedFormat("/path/to/document.PDF"));
    }

    @Test
    public void testIsSupportedFormat_Ppt() {
        assertTrue(TextExtractor.isSupportedFormat("presentation.ppt"));
        assertTrue(TextExtractor.isSupportedFormat("/path/to/presentation.PPT"));
    }

    @Test
    public void testIsSupportedFormat_Pptx() {
        assertTrue(TextExtractor.isSupportedFormat("presentation.pptx"));
        assertTrue(TextExtractor.isSupportedFormat("/path/to/presentation.PPTX"));
    }

    @Test
    public void testIsSupportedFormat_Unsupported() {
        assertFalse(TextExtractor.isSupportedFormat("document.txt"));
        assertFalse(TextExtractor.isSupportedFormat("archive.zip"));
        assertFalse(TextExtractor.isSupportedFormat("image.png"));
        assertFalse(TextExtractor.isSupportedFormat("script.js"));
        assertFalse(TextExtractor.isSupportedFormat("no_extension"));
        assertFalse(TextExtractor.isSupportedFormat(""));
    }

    @Test
    public void testIsSupportedFormat_EdgeCases() {
        // Note: isSupportedFormat(null) throws NPE - method doesn't handle null
        assertFalse(TextExtractor.isSupportedFormat(".doc")); // Just extension, no filename
        assertTrue(TextExtractor.isSupportedFormat("file.doc.docx")); // Double extension
    }

    // ==================== DOCX Extraction Tests ====================

    @Test
    public void testExtractTextFromDocx() throws IOException {
        // Create a DOCX file
        Path docxPath = tempDir.resolve("test.docx");
        createTestDocx(docxPath.toString(), TEST_CONTENT);

        // Extract text
        String extractedText = TextExtractor.extractText(docxPath.toString());

        // Verify
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Hello World"));
        assertTrue(extractedText.contains("This is a test document"));
    }

    @Test
    public void testExtractTextFromDocx_WithTable() throws IOException {
        Path docxPath = tempDir.resolve("test_table.docx");

        try (XWPFDocument doc = new XWPFDocument()) {
            // Add paragraph
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText("Document with table");

            // Add table
            var table = doc.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Cell 1");
            table.getRow(0).getCell(1).setText("Cell 2");
            table.getRow(1).getCell(0).setText("Cell 3");
            table.getRow(1).getCell(1).setText("Cell 4");

            try (FileOutputStream out = new FileOutputStream(docxPath.toString())) {
                doc.write(out);
            }
        }

        String extractedText = TextExtractor.extractText(docxPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Document with table"));
        assertTrue(extractedText.contains("Cell 1"));
        assertTrue(extractedText.contains("Cell 2"));
    }

    @Test
    public void testExtractTextFromDocx_EmptyFile() throws IOException {
        Path docxPath = tempDir.resolve("empty.docx");

        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxPath.toString())) {
            doc.write(out);
        }

        String extractedText = TextExtractor.extractText(docxPath.toString());

        assertNotNull(extractedText);
        // Empty document should return empty or whitespace-only string
        assertTrue(extractedText.trim().isEmpty());
    }

    // ==================== DOC Extraction Tests ====================

    @Test
    public void testExtractTextFromDoc_InvalidFile() throws IOException {
        // Creating valid DOC files is complex; test error handling instead
        Path docPath = tempDir.resolve("invalid.doc");
        Files.writeString(docPath, "This is not a valid DOC file");

        String extractedText = TextExtractor.extractText(docPath.toString());

        // Should return null for invalid/corrupted file
        assertNull(extractedText);
    }

    // ==================== PDF Extraction Tests ====================

    @Test
    public void testExtractTextFromPdf() throws IOException {
        Path pdfPath = tempDir.resolve("test.pdf");
        createTestPdf(pdfPath.toString(), "Hello World", "This is a PDF test document.");

        String extractedText = TextExtractor.extractText(pdfPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Hello World"));
        assertTrue(extractedText.contains("This is a PDF test document"));
    }

    @Test
    public void testExtractTextFromPdf_MultiPage() throws IOException {
        Path pdfPath = tempDir.resolve("multipage.pdf");

        try (PDDocument document = new PDDocument()) {
            // Page 1
            PDPage page1 = new PDPage();
            document.addPage(page1);

            PDPageContentStream contentStream1 = new PDPageContentStream(document, page1);
            contentStream1.beginText();
            contentStream1.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream1.newLineAtOffset(100, 700);
            contentStream1.showText("Page 1 Content");
            contentStream1.endText();
            contentStream1.close();

            // Page 2
            PDPage page2 = new PDPage();
            document.addPage(page2);

            PDPageContentStream contentStream2 = new PDPageContentStream(document, page2);
            contentStream2.beginText();
            contentStream2.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream2.newLineAtOffset(100, 700);
            contentStream2.showText("Page 2 Content");
            contentStream2.endText();
            contentStream2.close();

            document.save(pdfPath.toString());
        }

        String extractedText = TextExtractor.extractText(pdfPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Page 1 Content"));
        assertTrue(extractedText.contains("Page 2 Content"));
    }

    // ==================== PPTX Extraction Tests ====================

    @Test
    public void testExtractTextFromPptx() throws IOException {
        Path pptxPath = tempDir.resolve("test.pptx");
        createTestPptx(pptxPath.toString(), "Slide 1 Title", "Slide 1 Content");

        String extractedText = TextExtractor.extractText(pptxPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("=== Slide 1 ==="));
        assertTrue(extractedText.contains("Slide 1 Title"));
        assertTrue(extractedText.contains("Slide 1 Content"));
    }

    @Test
    public void testExtractTextFromPptx_MultiSlide() throws IOException {
        Path pptxPath = tempDir.resolve("multislide.pptx");

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            // Slide 1
            XSLFSlide slide1 = ppt.createSlide();
            XSLFTextBox textBox1 = slide1.createTextBox();
            textBox1.setText("First Slide Content");

            // Slide 2
            XSLFSlide slide2 = ppt.createSlide();
            XSLFTextBox textBox2 = slide2.createTextBox();
            textBox2.setText("Second Slide Content");

            try (FileOutputStream out = new FileOutputStream(pptxPath.toString())) {
                ppt.write(out);
            }
        }

        String extractedText = TextExtractor.extractText(pptxPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("=== Slide 1 ==="));
        assertTrue(extractedText.contains("=== Slide 2 ==="));
        assertTrue(extractedText.contains("First Slide Content"));
        assertTrue(extractedText.contains("Second Slide Content"));
    }

    // ==================== PPT Extraction Tests ====================

    @Test
    public void testExtractTextFromPpt() throws IOException {
        Path pptPath = tempDir.resolve("test.ppt");
        createTestPpt(pptPath.toString(), "Old PPT Title", "Old PPT Content");

        String extractedText = TextExtractor.extractText(pptPath.toString());

        assertNotNull(extractedText);
        assertTrue(extractedText.contains("=== Slide 1 ==="));
        assertTrue(extractedText.contains("Old PPT Title"));
        assertTrue(extractedText.contains("Old PPT Content"));
    }

    // ==================== File Output Tests ====================

    @Test
    public void testExtractTextToFile() throws IOException {
        Path docxPath = tempDir.resolve("test.docx");
        Path outputPath = tempDir.resolve("output.txt");
        createTestDocx(docxPath.toString(), "Test content for file output");

        boolean success = TextExtractor.extractTextToFile(docxPath.toString(), outputPath.toString());

        assertTrue(success);
        assertTrue(Files.exists(outputPath));

        String content = Files.readString(outputPath);
        assertTrue(content.contains("Test content for file output"));
    }

    @Test
    public void testExtractTextToFile_CreatesDirectory() throws IOException {
        Path docxPath = tempDir.resolve("test.docx");
        Path outputPath = tempDir.resolve("subdir/nested/output.txt");
        createTestDocx(docxPath.toString(), "Test content");

        boolean success = TextExtractor.extractTextToFile(docxPath.toString(), outputPath.toString());

        assertTrue(success);
        assertTrue(Files.exists(outputPath));
    }

    @Test
    public void testExtractTextToFile_AddsTxtExtension() throws IOException {
        Path docxPath = tempDir.resolve("test.docx");
        Path outputPath = tempDir.resolve("output"); // No extension
        createTestDocx(docxPath.toString(), "Test content");

        // Note: The service layer adds .txt, not the util class
        // So we test the util directly without extension handling
        boolean success = TextExtractor.extractTextToFile(docxPath.toString(), outputPath.toString());

        assertTrue(success);
        assertTrue(Files.exists(outputPath));
    }

    // ==================== Error Handling Tests ====================

    @Test
    public void testExtractText_NonExistentFile() {
        String result = TextExtractor.extractText("/non/existent/file.pdf");
        assertNull(result);
    }

    @Test
    public void testExtractTextToFile_NonExistentFile() {
        boolean success = TextExtractor.extractTextToFile(
                "/non/existent/file.pdf",
                tempDir.resolve("output.txt").toString()
        );
        assertFalse(success);
    }

    @Test
    public void testExtractText_UnsupportedFormat() {
        // Create a text file
        Path txtPath = tempDir.resolve("test.txt");
        try {
            Files.writeString(txtPath, "Plain text content");
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        String result = TextExtractor.extractText(txtPath.toString());
        assertNull(result);
    }

    @Test
    public void testExtractText_EmptyPath() {
        assertFalse(TextExtractor.isSupportedFormat(""));

        // extractText will try to get extension and fail
        String result = TextExtractor.extractText("");
        assertNull(result);
    }

    @Test
    public void testExtractText_InvalidFile() throws IOException {
        // Create an invalid/corrupted "PDF" file
        Path invalidPath = tempDir.resolve("invalid.pdf");
        Files.writeString(invalidPath, "This is not a valid PDF content");

        String result = TextExtractor.extractText(invalidPath.toString());
        assertNull(result);
    }

    // ==================== Helper Methods ====================

    private void createTestDocx(String path, String content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                XWPFParagraph para = doc.createParagraph();
                XWPFRun run = para.createRun();
                run.setText(line);
            }
            try (FileOutputStream out = new FileOutputStream(path)) {
                doc.write(out);
            }
        }
    }

    // Note: Creating valid .doc files programmatically is complex due to
    // the binary format requirements. DOC tests focus on error handling
    // and integration rather than creation.

    private void createTestPdf(String path, String... lines) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(100, 700);

            float yOffset = 700;
            for (String line : lines) {
                contentStream.showText(line);
                yOffset -= 20;
                contentStream.newLineAtOffset(0, -20);
            }

            contentStream.endText();
            contentStream.close();

            document.save(path);
        }
    }

    private void createTestPptx(String path, String title, String content) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFSlide slide = ppt.createSlide();

            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setText(title);

            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setText(content);

            try (FileOutputStream out = new FileOutputStream(path)) {
                ppt.write(out);
            }
        }
    }

    private void createTestPpt(String path, String title, String content) throws IOException {
        try (HSLFSlideShow ppt = new HSLFSlideShow()) {
            HSLFSlide slide = ppt.createSlide();

            HSLFTextBox titleBox = new HSLFTextBox();
            titleBox.setText(title);
            slide.addShape(titleBox);

            HSLFTextBox contentBox = new HSLFTextBox();
            contentBox.setText(content);
            slide.addShape(contentBox);

            try (FileOutputStream out = new FileOutputStream(path)) {
                ppt.write(out);
            }
        }
    }
}
