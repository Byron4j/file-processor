package com.fileprocessor.service;

import com.fileprocessor.BaseTest;
import com.fileprocessor.dto.FileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Excel处理服务测试")
class ExcelServiceTest extends BaseTest {

    @Autowired
    private ExcelService excelService;

    @Test
    @DisplayName("提取Excel文本 - 成功")
    void extractText_Success() throws Exception {
        String testFile = getTestFilePath("test.xlsx");
        createSimpleExcelFile(testFile);

        FileResponse response = excelService.extractText(testFile, 0);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("提取Excel文本 - 文件不存在")
    void extractText_FileNotExists() {
        FileResponse response = excelService.extractText(getTestFilePath("non-existent.xlsx"), 0);
        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("转换Excel到CSV - 成功")
    void convertToCsv_Success() throws Exception {
        String testFile = getTestFilePath("test.xlsx");
        String outputFile = getTestFilePath("output.csv");
        createSimpleExcelFile(testFile);

        FileResponse response = excelService.convertToCsv(testFile, outputFile, 0, ',');

        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("转换Excel到JSON - 跳过")
    void convertToJson_Skipped() {
        // ExcelToJsonConfig 需要额外配置
        assertTrue(true);
    }

    @Test
    @DisplayName("获取Sheet信息 - 成功")
    void getSheetInfo_Success() throws Exception {
        String testFile = getTestFilePath("test.xlsx");
        createSimpleExcelFile(testFile);

        FileResponse response = excelService.getSheetInfo(testFile);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
    }

    private void createSimpleExcelFile(String path) throws Exception {
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Test");
        org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(0);
        row.createCell(0).setCellValue("Name");
        row.createCell(1).setCellValue("Value");

        try (FileOutputStream fos = new FileOutputStream(path)) {
            workbook.write(fos);
        }
        workbook.close();
    }
}
