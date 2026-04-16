package com.fileprocessor.service;

import com.fileprocessor.BaseTest;
import com.fileprocessor.dto.FileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PDF处理服务测试")
class PdfServiceTest extends BaseTest {

    @Autowired
    private PdfService pdfService;

    @Test
    @DisplayName("PDF合并 - 成功")
    void mergePdfs_Success() throws Exception {
        String pdf1 = getTestFilePath("test1.pdf");
        String pdf2 = getTestFilePath("test2.pdf");
        String output = getTestFilePath("merged.pdf");

        createSimplePdf(pdf1);
        createSimplePdf(pdf2);

        FileResponse response = pdfService.merge(Arrays.asList(pdf1, pdf2), output, false);

        assertTrue(response.isSuccess());
        assertTrue(new File(output).exists());
    }

    @Test
    @DisplayName("PDF提取页面 - 成功")
    void extractPages_Success() throws Exception {
        String input = getTestFilePath("test.pdf");
        String output = getTestFilePath("extracted.pdf");
        createSimplePdf(input);

        FileResponse response = pdfService.extractPages(input, output, Collections.singletonList(1));

        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("PDF旋转 - 成功")
    void rotatePdf_Success() throws Exception {
        String input = getTestFilePath("test.pdf");
        String output = getTestFilePath("rotated.pdf");
        createSimplePdf(input);

        FileResponse response = pdfService.rotate(input, output, Collections.singletonList(1), 90);

        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("PDF删除页面 - 成功")
    void deletePages_Success() throws Exception {
        String input = getTestFilePath("test.pdf");
        String output = getTestFilePath("deleted.pdf");
        createSimplePdf(input);

        FileResponse response = pdfService.deletePages(input, output, Collections.emptyList());

        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("获取PDF信息 - 成功")
    void getPdfInfo_Success() throws Exception {
        String input = getTestFilePath("test.pdf");
        createSimplePdf(input);

        FileResponse response = pdfService.getPdfInfo(input);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("PDF处理 - 文件不存在")
    void pdfOperation_FileNotExists() {
        FileResponse response = pdfService.merge(
            Arrays.asList(getTestFilePath("non-existent.pdf")),
            getTestFilePath("output.pdf"),
            false
        );
        assertFalse(response.isSuccess());
    }

    private void createSimplePdf(String path) throws Exception {
        org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument();
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
        document.addPage(page);
        document.save(path);
        document.close();
    }
}
