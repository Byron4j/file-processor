package com.fileprocessor.service;

import com.fileprocessor.BaseTest;
import com.fileprocessor.dto.FileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文件转换服务测试")
class FileConvertServiceTest extends BaseTest {

    @Autowired
    private FileConvertService fileConvertService;

    @Test
    @DisplayName("获取文件信息 - 成功")
    void getFileInfo_Success() throws Exception {
        // Given
        String testFile = getTestFilePath("test.txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("Hello World");
        }

        // When
        FileResponse response = fileConvertService.getFileInfo(testFile);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(testFile, response.getFilePath());
        assertEquals(11L, response.getFileSize());
    }

    @Test
    @DisplayName("获取文件信息 - 文件不存在")
    void getFileInfo_FileNotExists() {
        // Given
        String nonExistentFile = getTestFilePath("non-existent.txt");

        // When
        FileResponse response = fileConvertService.getFileInfo(nonExistentFile);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("DOC转DOCX - 源文件不存在")
    void convertDocToDocx_SourceNotExists() {
        // Given
        String sourcePath = getTestFilePath("non-existent.doc");
        String targetPath = getTestFilePath("output.docx");

        // When
        FileResponse response = fileConvertService.convertDocToDocx(sourcePath, targetPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("DOC转DOCX - 文件格式错误")
    void convertDocToDocx_WrongExtension() throws Exception {
        // Given
        String sourcePath = getTestFilePath("test.txt");
        Files.write(new File(sourcePath).toPath(), "test".getBytes());
        String targetPath = getTestFilePath("output.docx");

        // When
        FileResponse response = fileConvertService.convertDocToDocx(sourcePath, targetPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("must be a .doc file"));
    }

    @Test
    @DisplayName("ZIP提取HTML - 源文件不存在")
    void extractIndexHtmlFromZip_SourceNotExists() {
        // Given
        String zipPath = getTestFilePath("non-existent.zip");
        String outputPath = getTestFilePath("output.txt");

        // When
        FileResponse response = fileConvertService.extractIndexHtmlFromZip(zipPath, outputPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("ZIP提取HTML - 文件格式错误")
    void extractIndexHtmlFromZip_WrongExtension() throws Exception {
        // Given
        String zipPath = getTestFilePath("test.txt");
        Files.write(new File(zipPath).toPath(), "test".getBytes());
        String outputPath = getTestFilePath("output.txt");

        // When
        FileResponse response = fileConvertService.extractIndexHtmlFromZip(zipPath, outputPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("must be a .zip file"));
    }

    @Test
    @DisplayName("文本提取 - 源文件不存在")
    void extractTextFromDocument_SourceNotExists() {
        // Given
        String sourcePath = getTestFilePath("non-existent.docx");
        String outputPath = getTestFilePath("output.txt");

        // When
        FileResponse response = fileConvertService.extractTextFromDocument(sourcePath, outputPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("文本提取 - 不支持的格式")
    void extractTextFromDocument_UnsupportedFormat() throws Exception {
        // Given
        String sourcePath = getTestFilePath("test.xyz");
        Files.write(new File(sourcePath).toPath(), "test".getBytes());
        String outputPath = getTestFilePath("output.txt");

        // When
        FileResponse response = fileConvertService.extractTextFromDocument(sourcePath, outputPath);

        // Then
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Unsupported file format"));
    }
}
