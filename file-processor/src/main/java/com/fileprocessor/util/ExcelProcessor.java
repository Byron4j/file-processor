package com.fileprocessor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Excel processing utility for text extraction and format conversion
 */
public class ExcelProcessor {

    private static final Logger log = LoggerFactory.getLogger(ExcelProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Excel content holder
     */
    public static class ExcelContent {
        private String sheetName;
        private int rowCount;
        private int columnCount;
        private List<List<String>> content;

        public ExcelContent() {
            this.content = new ArrayList<>();
        }

        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        public int getColumnCount() { return columnCount; }
        public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
        public List<List<String>> getContent() { return content; }
        public void setContent(List<List<String>> content) { this.content = content; }
    }

    /**
     * Sheet information
     */
    public static class SheetInfo {
        private int index;
        private String name;
        private int rowCount;
        private int columnCount;

        public SheetInfo(int index, String name, int rowCount, int columnCount) {
            this.index = index;
            this.name = name;
            this.rowCount = rowCount;
            this.columnCount = columnCount;
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public int getRowCount() { return rowCount; }
        public int getColumnCount() { return columnCount; }
    }

    /**
     * Extract text content from Excel file
     *
     * @param filePath Path to Excel file
     * @param sheetIndex Sheet index (0-based)
     * @return ExcelContent object containing extracted data
     */
    public static ExcelContent extractText(String filePath, int sheetIndex) {
        log.info("Extracting text from Excel: {}, sheet: {}", filePath, sheetIndex);

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(is, filePath)) {

            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                throw new IllegalArgumentException("Invalid sheet index: " + sheetIndex);
            }

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            ExcelContent content = new ExcelContent();
            content.setSheetName(sheet.getSheetName());

            int maxColumns = 0;
            List<List<String>> rows = new ArrayList<>();

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(getCellValueAsString(cell));
                }
                rows.add(rowData);
                maxColumns = Math.max(maxColumns, rowData.size());
            }

            content.setContent(rows);
            content.setRowCount(rows.size());
            content.setColumnCount(maxColumns);

            log.info("Extracted {} rows, {} columns from sheet '{}'",
                    rows.size(), maxColumns, sheet.getSheetName());
            return content;

        } catch (Exception e) {
            log.error("Failed to extract text from Excel: {}", filePath, e);
            return null;
        }
    }

    /**
     * Extract text from all sheets
     */
    public static Map<String, ExcelContent> extractAllSheets(String filePath) {
        log.info("Extracting all sheets from Excel: {}", filePath);

        Map<String, ExcelContent> result = new LinkedHashMap<>();

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(is, filePath)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                ExcelContent content = extractText(filePath, i);
                if (content != null) {
                    result.put(content.getSheetName(), content);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to extract sheets from Excel: {}", filePath, e);
            return null;
        }
    }

    /**
     * Convert Excel to CSV format
     *
     * @param sourcePath Source Excel file path
     * @param targetPath Target CSV file path
     * @param sheetIndex Sheet index to convert
     * @param delimiter CSV delimiter character
     * @return true if conversion successful
     */
    public static boolean convertToCsv(String sourcePath, String targetPath,
                                       int sheetIndex, char delimiter) {
        log.info("Converting Excel to CSV: {} -> {}", sourcePath, targetPath);

        try {
            ExcelContent content = extractText(sourcePath, sheetIndex);
            if (content == null) {
                return false;
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .build();

            try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, format)) {

                for (List<String> row : content.getContent()) {
                    printer.printRecord(row);
                }
            }

            log.info("Successfully converted to CSV: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to convert Excel to CSV: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Excel to JSON conversion configuration
     */
    public static class ExcelToJsonConfig {
        private int sheetIndex = 0;
        private int headerRow = 0;
        private int dataStartRow = 1;
        private Map<String, String> columnMapping; // A->name, B->age, etc.
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

    /**
     * Convert Excel to JSON format
     *
     * @param sourcePath Source Excel file path
     * @param targetPath Target JSON file path
     * @param config Conversion configuration
     * @return true if conversion successful
     */
    public static boolean convertToJson(String sourcePath, String targetPath,
                                        ExcelToJsonConfig config) {
        log.info("Converting Excel to JSON: {} -> {}", sourcePath, targetPath);

        try (InputStream is = Files.newInputStream(Paths.get(sourcePath));
             Workbook workbook = createWorkbook(is, sourcePath)) {

            if (config.getSheetIndex() < 0 || config.getSheetIndex() >= workbook.getNumberOfSheets()) {
                throw new IllegalArgumentException("Invalid sheet index: " + config.getSheetIndex());
            }

            Sheet sheet = workbook.getSheetAt(config.getSheetIndex());
            List<Map<String, Object>> data = new ArrayList<>();

            // Get headers
            List<String> headers = new ArrayList<>();
            Row headerRow = sheet.getRow(config.getHeaderRow());
            if (headerRow != null && config.isUseHeaderAsKey()) {
                for (Cell cell : headerRow) {
                    headers.add(getCellValueAsString(cell));
                }
            }

            // Process data rows
            for (int i = config.getDataStartRow(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();

                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String key = getColumnKey(j, headers, config.getColumnMapping());
                    Object value = getCellValue(cell);
                    rowData.put(key, value);
                }

                if (!rowData.isEmpty()) {
                    data.add(rowData);
                }
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write JSON
            objectMapper.writeValue(targetFile, data);

            log.info("Successfully converted to JSON: {} ({} records)", targetPath, data.size());
            return true;

        } catch (Exception e) {
            log.error("Failed to convert Excel to JSON: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Get sheet information from Excel file
     *
     * @param filePath Path to Excel file
     * @return List of SheetInfo objects
     */
    public static List<SheetInfo> getSheetInfo(String filePath) {
        log.info("Getting sheet info from: {}", filePath);

        List<SheetInfo> infoList = new ArrayList<>();

        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = createWorkbook(is, filePath)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                int rowCount = sheet.getPhysicalNumberOfRows();
                int maxColumns = 0;

                for (Row row : sheet) {
                    maxColumns = Math.max(maxColumns, row.getLastCellNum());
                }

                infoList.add(new SheetInfo(i, sheet.getSheetName(), rowCount, maxColumns));
            }

            return infoList;

        } catch (Exception e) {
            log.error("Failed to get sheet info: {}", filePath, e);
            return null;
        }
    }

    // ==================== Helper Methods ====================

    private static Workbook createWorkbook(InputStream is, String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(is);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(is);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + filePath);
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        return cell.getStringCellValue();
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toInstant().toString();
                    }
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return (long) numericValue;
                    }
                    return numericValue;
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        return cell.getStringCellValue();
                    }
                case BLANK:
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String getColumnKey(int columnIndex, List<String> headers,
                                       Map<String, String> columnMapping) {
        // Check for column mapping first (A, B, C...)
        if (columnMapping != null) {
            String colLetter = getColumnLetter(columnIndex);
            if (columnMapping.containsKey(colLetter)) {
                return columnMapping.get(colLetter);
            }
        }

        // Use header if available
        if (columnIndex < headers.size() && !headers.get(columnIndex).isEmpty()) {
            return headers.get(columnIndex);
        }

        // Fallback to column letter
        return getColumnLetter(columnIndex);
    }

    private static String getColumnLetter(int columnIndex) {
        StringBuilder result = new StringBuilder();
        int index = columnIndex;
        while (index >= 0) {
            result.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return result.toString();
    }

    /**
     * Check if file is supported Excel format
     */
    public static boolean isSupportedFormat(String filePath) {
        if (filePath == null) return false;
        String lowerPath = filePath.toLowerCase();
        return lowerPath.endsWith(".xlsx") || lowerPath.endsWith(".xls");
    }
}
