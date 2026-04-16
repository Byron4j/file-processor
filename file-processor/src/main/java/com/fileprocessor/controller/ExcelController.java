package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.ExcelService;
import com.fileprocessor.util.ExcelProcessor;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Excel processing REST API controller
 */
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private static final Logger log = LoggerFactory.getLogger(ExcelController.class);

    @Autowired
    private ExcelService excelService;

    /**
     * Extract text from Excel file
     */
    @PostMapping("/extract/text")
    public ResponseEntity<FileResponse> extractText(
            @RequestBody @Valid ExcelExtractRequest request) {
        log.info("REST request to extract Excel text: {}, sheet: {}",
                request.getSourcePath(), request.getSheetIndex());

        FileResponse response = excelService.extractText(
                request.getSourcePath(),
                request.getSheetIndex()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert Excel to CSV
     */
    @PostMapping("/convert/csv")
    public ResponseEntity<FileResponse> convertToCsv(
            @RequestBody @Valid ExcelToCsvRequest request) {
        log.info("REST request to convert Excel to CSV: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        FileResponse response = excelService.convertToCsv(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getSheetIndex(),
                request.getDelimiter()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Convert Excel to JSON
     */
    @PostMapping("/convert/json")
    public ResponseEntity<FileResponse> convertToJson(
            @RequestBody @Valid ExcelToJsonRequest request) {
        log.info("REST request to convert Excel to JSON: {} -> {}",
                request.getSourcePath(), request.getTargetPath());

        ExcelProcessor.ExcelToJsonConfig config = new ExcelProcessor.ExcelToJsonConfig();
        config.setSheetIndex(request.getSheetIndex());
        config.setHeaderRow(request.getHeaderRow());
        config.setDataStartRow(request.getDataStartRow());
        config.setColumnMapping(request.getColumnMapping());
        config.setUseHeaderAsKey(request.isUseHeaderAsKey());

        FileResponse response = excelService.convertToJson(
                request.getSourcePath(),
                request.getTargetPath(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get Excel sheet information
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getSheetInfo(
            @RequestParam String path) {
        log.info("REST request to get Excel sheet info: {}", path);

        FileResponse response = excelService.getSheetInfo(path);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class ExcelExtractRequest extends FileConvertRequest {
        private int sheetIndex = 0;

        public int getSheetIndex() { return sheetIndex; }
        public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }
    }

    public static class ExcelToCsvRequest extends FileConvertRequest {
        private int sheetIndex = 0;
        private char delimiter = ',';

        public int getSheetIndex() { return sheetIndex; }
        public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }
        public char getDelimiter() { return delimiter; }
        public void setDelimiter(char delimiter) { this.delimiter = delimiter; }
    }

    public static class ExcelToJsonRequest extends FileConvertRequest {
        private int sheetIndex = 0;
        private int headerRow = 0;
        private int dataStartRow = 1;
        private Map<String, String> columnMapping;
        private boolean useHeaderAsKey = true;

        public int getSheetIndex() { return sheetIndex; }
        public void setSheetIndex(int sheetIndex) { this.sheetIndex = sheetIndex; }
        public int getHeaderRow() { return headerRow; }
        public void setHeaderRow(int headerRow) { this.headerRow = headerRow; }
        public int getDataStartRow() { return dataStartRow; }
        public void setDataStartRow(int dataStartRow) { this.dataStartRow = dataStartRow; }
        public Map<String, String> getColumnMapping() { return columnMapping; }
        public void setColumnMapping(Map<String, String> columnMapping) { this.columnMapping = columnMapping; }
        public boolean isUseHeaderAsKey() { return useHeaderAsKey; }
        public void setUseHeaderAsKey(boolean useHeaderAsKey) { this.useHeaderAsKey = useHeaderAsKey; }
    }
}
