package com.fileprocessor;

import com.fileprocessor.service.FileConvertService;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.DocConverter;
import com.fileprocessor.util.ZipProcessor;
import com.fileprocessor.util.TextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FileConvertTest {

    @Autowired
    private FileConvertService fileConvertService;

    private static final String TEST_DIR = "./test-files/";

    @Test
    public void testDocConverterExists() {
        assertNotNull(DocConverter.class);
    }

    @Test
    public void testZipProcessorExists() {
        assertNotNull(ZipProcessor.class);
    }

    @Test
    public void testTextExtractorExists() {
        assertNotNull(TextExtractor.class);
    }

    @Test
    public void testIsSupportedFormat() {
        assertTrue(TextExtractor.isSupportedFormat("test.doc"));
        assertTrue(TextExtractor.isSupportedFormat("test.docx"));
        assertTrue(TextExtractor.isSupportedFormat("test.pdf"));
        assertTrue(TextExtractor.isSupportedFormat("test.ppt"));
        assertTrue(TextExtractor.isSupportedFormat("test.pptx"));
        assertFalse(TextExtractor.isSupportedFormat("test.txt"));
        assertFalse(TextExtractor.isSupportedFormat("test.zip"));
    }

    @Test
    public void testExtractIndexHtmlFromZip() throws IOException {
        // Create test directory
        File testDir = new File(TEST_DIR);
        testDir.mkdirs();

        // Create a test ZIP file with index.html
        String zipPath = TEST_DIR + "test.zip";
        String outputPath = TEST_DIR + "output.txt";

        createTestZipWithIndexHtml(zipPath);

        // Test extraction
        FileResponse response = fileConvertService.extractIndexHtmlFromZip(zipPath, outputPath);

        // Verify results
        assertTrue(response.isSuccess(), "Extraction should succeed: " + response.getMessage());
        assertNotNull(response.getFileSize());
        assertTrue(response.getFileSize() > 0, "Output file should have content");

        // Read and verify output content
        String outputContent = readFileContent(outputPath);
        assertTrue(outputContent.contains("Hello World"), "Should contain header text");
        assertTrue(outputContent.contains("test paragraph"), "Should contain paragraph text");
        assertTrue(outputContent.contains("Title: Test Page"), "Should contain title");

        // Cleanup
        new File(zipPath).delete();
        new File(outputPath).delete();
        testDir.delete();
    }

    @Test
    public void testExtractIndexHtmlNotFound() throws IOException {
        // Create test directory
        File testDir = new File(TEST_DIR);
        testDir.mkdirs();

        // Create a test ZIP file WITHOUT index.html
        String zipPath = TEST_DIR + "no_index.zip";
        String outputPath = TEST_DIR + "output2.txt";

        createTestZipWithoutIndexHtml(zipPath);

        // Test extraction - should fail
        FileResponse response = fileConvertService.extractIndexHtmlFromZip(zipPath, outputPath);

        assertFalse(response.isSuccess(), "Extraction should fail when index.html not found");

        // Cleanup
        new File(zipPath).delete();
        testDir.delete();
    }

    private void createTestZipWithIndexHtml(String zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            // Add index.html
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head><title>Test Page</title></head>
                <body>
                    <h1>Hello World</h1>
                    <p>This is a test paragraph.</p>
                    <ul>
                        <li>Item 1</li>
                        <li>Item 2</li>
                    </ul>
                </body>
                </html>
                """;

            ZipEntry entry = new ZipEntry("index.html");
            zos.putNextEntry(entry);
            zos.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Add another file
            ZipEntry otherEntry = new ZipEntry("readme.txt");
            zos.putNextEntry(otherEntry);
            zos.write("This is readme".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private void createTestZipWithoutIndexHtml(String zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            ZipEntry entry = new ZipEntry("readme.txt");
            zos.putNextEntry(entry);
            zos.write("Just a readme file".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    @Test
    public void testExtractTextUnsupportedFormat() throws IOException {
        // Create test directory and a txt file (unsupported format)
        File testDir = new File(TEST_DIR);
        testDir.mkdirs();
        File txtFile = new File(TEST_DIR + "test.txt");
        Files.write(txtFile.toPath(), "test content".getBytes(StandardCharsets.UTF_8));

        // Test with unsupported format
        FileResponse response = fileConvertService.extractTextFromDocument(
                txtFile.getPath(),
                TEST_DIR + "output.txt"
        );

        assertFalse(response.isSuccess(), "Should fail for unsupported format");
        assertTrue(response.getMessage().contains("Unsupported file format"),
                "Error message should mention unsupported format");

        // Cleanup
        txtFile.delete();
        testDir.delete();
    }

    @Test
    public void testExtractTextNonExistentFile() {
        FileResponse response = fileConvertService.extractTextFromDocument(
                "./non_existent.pdf",
                "./output.txt"
        );

        assertFalse(response.isSuccess(), "Should fail for non-existent file");
        assertTrue(response.getMessage().contains("does not exist"),
                "Error message should mention file does not exist");
    }

    private String readFileContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
