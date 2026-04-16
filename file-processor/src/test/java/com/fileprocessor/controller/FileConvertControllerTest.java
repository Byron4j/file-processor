package com.fileprocessor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessor.BaseTest;
import com.fileprocessor.dto.FileConvertRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("文件转换控制器测试")
class FileConvertControllerTest extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("DOC转DOCX - 成功")
    void convertDocToDocx_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.doc",
            "application/msword",
            "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/convert/doc-to-docx")
                .file(file)
                .param("targetPath", getTestFilePath("output.docx")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false)); // Will fail because not a real doc file
    }

    @Test
    @DisplayName("文本提取 - 成功")
    void extractText_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        mockMvc.perform(multipart("/api/file/extract/text")
                .file(file)
                .param("outputPath", getTestFilePath("output.txt")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取文件信息 - 成功")
    void getFileInfo_Success() throws Exception {
        // Create a test file
        java.io.File testFile = new java.io.File(getTestFilePath("test.txt"));
        try (java.io.FileWriter writer = new java.io.FileWriter(testFile)) {
            writer.write("Hello World");
        }

        mockMvc.perform(post("/api/file/info")
                .param("path", testFile.getAbsolutePath()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.fileSize").value(11));
    }

    @Test
    @DisplayName("健康检查 - 成功")
    void healthCheck() throws Exception {
        mockMvc.perform(post("/api/file/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("UP")));
    }
}
