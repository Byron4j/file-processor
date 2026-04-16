# 第二阶段设计方案：进阶功能

## 概述

**目标**：增加文件编辑、合并、安全相关能力
**时间周期**：2-3 个月
**核心功能**：PDF 工具链、OCR 文字识别、水印处理、文件加密

---

## 一、功能清单

### 1.1 PDF 工具链

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| PDF 合并 | 多个 PDF 合并为一个文件 | P0 |
| PDF 拆分 | 按页码范围拆分 PDF | P0 |
| PDF 页面提取 | 提取指定页面为单独文件 | P1 |
| PDF 旋转 | 旋转指定页面 | P1 |
| PDF 删除页面 | 删除指定页面 | P1 |

### 1.2 水印处理

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| PDF 文字水印 | 添加文字水印（透明度、旋转、位置） | P0 |
| PDF 图片水印 | 添加图片水印 | P1 |
| Word 文字水印 | 添加文字水印 | P1 |
| PPT 水印 | 添加文字/图片水印 | P2 |

### 1.3 文件安全

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| PDF 密码保护 | 设置打开密码和权限密码 | P1 |
| PDF 解密 | 移除 PDF 密码 | P1 |
| PDF 权限设置 | 限制打印、复制、编辑 | P1 |

### 1.4 OCR 文字识别

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 图片 OCR | 从图片提取文字 | P0 |
| PDF OCR | 从扫描版 PDF 提取文字 | P0 |
| 多语言支持 | 中英文、日文、韩文 | P1 |
| 表格 OCR | 识别图片中的表格 | P2 |

---

## 二、技术架构

### 2.1 模块结构

```
com.fileprocessor
├── controller
│   ├── PdfController.java        # PDF 操作 API
│   ├── WatermarkController.java  # 水印 API
│   ├── SecurityController.java   # 加密/解密 API
│   └── OcrController.java        # OCR API
├── service
│   ├── PdfService.java
│   ├── WatermarkService.java
│   ├── SecurityService.java
│   └── OcrService.java
├── util
│   ├── PdfEditor.java            # PDF 编辑工具
│   ├── WatermarkUtil.java        # 水印工具
│   ├── PdfSecurityUtil.java      # PDF 安全工具
│   └── OcrEngine.java            # OCR 引擎封装
├── ocr
│   ├── TesseractOcr.java         # Tesseract 实现
│   ├── BaiduOcr.java             # 百度云 OCR
│   └── OcrProvider.java          # OCR 提供商接口
└── dto
    ├── PdfMergeRequest.java
    ├── PdfSplitRequest.java
    ├── WatermarkRequest.java
    └── OcrRequest.java
```

### 2.2 依赖项

```xml
<!-- PDFBox 扩展 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox-tools</artifactId>
    <version>3.0.1</version>
</dependency>

<!-- iText (用于高级 PDF 功能) -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext-core</artifactId>
    <version>8.0.2</version>
    <type>pom</type>
</dependency>

<!-- Tesseract OCR -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.11.0</version>
</dependency>

<!-- 百度云 OCR SDK -->
<dependency>
    <groupId>com.baidu.aip</groupId>
    <artifactId>java-sdk</artifactId>
    <version>4.16.17</version>
</dependency>

<!-- 图像预处理 (OCR 前处理) -->
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.9</version>
</dependency>
```

---

## 三、API 设计

### 3.1 PDF 操作 API

#### POST /api/pdf/merge
合并多个 PDF

```json
// Request
{
  "sourcePaths": [
    "/path/to/doc1.pdf",
    "/path/to/doc2.pdf",
    "/path/to/doc3.pdf"
  ],
  "targetPath": "/path/to/merged.pdf",
  "bookmarks": true
}

// Response
{
  "success": true,
  "message": "PDFs merged successfully",
  "filePath": "/path/to/merged.pdf",
  "totalPages": 50
}
```

#### POST /api/pdf/split
拆分 PDF

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "outputDir": "/path/to/output/",
  "mode": "RANGE",  // RANGE, EVERY_N_PAGES, EXTRACT
  "ranges": [
    {"start": 1, "end": 10, "name": "part1.pdf"},
    {"start": 11, "end": 20, "name": "part2.pdf"}
  ]
}

// Response
{
  "success": true,
  "message": "PDF split successfully",
  "files": [
    {"filePath": "/path/to/output/part1.pdf", "pages": 10},
    {"filePath": "/path/to/output/part2.pdf", "pages": 10}
  ]
}
```

#### POST /api/pdf/rotate
旋转页面

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/rotated.pdf",
  "pages": [1, 3, 5],
  "angle": 90  // 90, 180, 270
}
```

#### POST /api/pdf/delete-pages
删除页面

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/result.pdf",
  "pages": [2, 4, 6]
}
```

### 3.2 水印 API

#### POST /api/watermark/pdf/text
PDF 文字水印

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/watermarked.pdf",
  "watermark": {
    "text": "CONFIDENTIAL",
    "fontSize": 48,
    "color": "#FF0000",
    "opacity": 0.3,
    "rotation": 45,
    "position": "CENTER",  // CENTER, TOP_LEFT, TOP_RIGHT, etc.
    "pages": "ALL"  // ALL, ODD, EVEN, [1,2,3]
  }
}
```

#### POST /api/watermark/pdf/image
PDF 图片水印

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/watermarked.pdf",
  "watermark": {
    "imagePath": "/path/to/logo.png",
    "scale": 0.5,
    "opacity": 0.3,
    "position": "BOTTOM_RIGHT",
    "marginX": 50,
    "marginY": 50
  }
}
```

#### POST /api/watermark/word
Word 水印

```json
// Request
{
  "sourcePath": "/path/to/document.docx",
  "targetPath": "/path/to/watermarked.docx",
  "watermark": {
    "text": "DRAFT",
    "fontSize": 72,
    "color": "CCCCCC",
    "layout": "DIAGONAL"  // DIAGONAL, HORIZONTAL
  }
}
```

### 3.3 文件安全 API

#### POST /api/security/pdf/encrypt
加密 PDF

```json
// Request
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/encrypted.pdf",
  "userPassword": "open123",
  "ownerPassword": "owner456",
  "permissions": {
    "canPrint": true,
    "canModify": false,
    "canCopy": false,
    "canAnnotate": true,
    "encryptionLevel": "AES_256"
  }
}
```

#### POST /api/security/pdf/decrypt
解密 PDF

```json
// Request
{
  "sourcePath": "/path/to/encrypted.pdf",
  "targetPath": "/path/to/decrypted.pdf",
  "password": "open123"
}
```

#### POST /api/security/pdf/check-password
检查 PDF 是否加密

```json
// Request
{
  "filePath": "/path/to/document.pdf"
}

// Response
{
  "success": true,
  "data": {
    "encrypted": true,
    "hasUserPassword": true,
    "hasOwnerPassword": true,
    "permissions": {
      "canPrint": true,
      "canModify": false
    }
  }
}
```

### 3.4 OCR API

#### POST /api/ocr/image
图片 OCR

```json
// Request
{
  "sourcePath": "/path/to/image.png",
  "language": "chi_sim+eng",
  "preprocess": true,
  "targetPath": "/path/to/output.txt"  // 可选，不填则返回文本
}

// Response
{
  "success": true,
  "data": {
    "text": "识别的文字内容...",
    "confidence": 0.95,
    "wordCount": 150,
    "processingTime": 1200
  }
}
```

#### POST /api/ocr/pdf
PDF OCR（扫描版）

```json
// Request
{
  "sourcePath": "/path/to/scanned.pdf",
  "targetPath": "/path/to/output.pdf",  // 可搜索的 PDF
  "language": "chi_sim+eng",
  "pages": "ALL",
  "dpi": 300
}

// Response
{
  "success": true,
  "data": {
    "outputPath": "/path/to/output.pdf",
    "pagesProcessed": 10,
    "totalText": "提取的全部文字...",
    "processingTime": 5000
  }
}
```

#### POST /api/ocr/table
表格 OCR

```json
// Request
{
  "sourcePath": "/path/to/table_image.png",
  "targetPath": "/path/to/output.xlsx",
  "format": "EXCEL",  // EXCEL, CSV, JSON
  "language": "chi_sim"
}
```

---

## 四、核心类设计

### 4.1 PdfEditor

```java
public class PdfEditor {

    /**
     * 合并多个 PDF
     */
    public static boolean merge(List<String> sourcePaths, String targetPath,
                                boolean addBookmarks);

    /**
     * 拆分 PDF
     */
    public static List<String> split(String sourcePath, String outputDir,
                                     SplitConfig config);

    /**
     * 提取指定页面
     */
    public static boolean extractPages(String sourcePath, String targetPath,
                                       List<Integer> pages);

    /**
     * 旋转页面
     */
    public static boolean rotate(String sourcePath, String targetPath,
                                 List<Integer> pages, int angle);

    /**
     * 删除页面
     */
    public static boolean deletePages(String sourcePath, String targetPath,
                                      List<Integer> pages);

    /**
     * 获取 PDF 信息
     */
    public static PdfInfo getInfo(String filePath);
}

public class SplitConfig {
    private SplitMode mode;  // RANGE, EVERY_N_PAGES, EXTRACT
    private List<PageRange> ranges;
    private int everyNPages;
}
```

### 4.2 WatermarkUtil

```java
public class WatermarkUtil {

    /**
     * PDF 文字水印
     */
    public static boolean addTextWatermarkToPdf(String sourcePath,
                                                 String targetPath,
                                                 TextWatermarkConfig config);

    /**
     * PDF 图片水印
     */
    public static boolean addImageWatermarkToPdf(String sourcePath,
                                                  String targetPath,
                                                  ImageWatermarkConfig config);

    /**
     * Word 文字水印
     */
    public static boolean addTextWatermarkToWord(String sourcePath,
                                                  String targetPath,
                                                  TextWatermarkConfig config);

    /**
     * PPT 水印
     */
    public static boolean addWatermarkToPpt(String sourcePath,
                                             String targetPath,
                                             WatermarkConfig config);
}

public class TextWatermarkConfig {
    private String text;
    private int fontSize;
    private String color;
    private float opacity;
    private int rotation;
    private WatermarkPosition position;
    private PageRange pages;
}
```

### 4.3 PdfSecurityUtil

```java
public class PdfSecurityUtil {

    /**
     * 加密 PDF
     */
    public static boolean encrypt(String sourcePath, String targetPath,
                                  String userPassword, String ownerPassword,
                                  EncryptionConfig config);

    /**
     * 解密 PDF
     */
    public static boolean decrypt(String sourcePath, String targetPath,
                                  String password);

    /**
     * 检查是否加密
     */
    public static boolean isEncrypted(String filePath);

    /**
     * 获取安全信息
     */
    public static SecurityInfo getSecurityInfo(String filePath);

    /**
     * 修改权限
     */
    public static boolean changePermissions(String sourcePath,
                                            String targetPath,
                                            String ownerPassword,
                                            PermissionConfig config);
}
```

### 4.4 OCR 引擎

```java
public interface OcrProvider {

    /**
     * 识别图片
     */
    OcrResult recognize(String imagePath, OcrConfig config);

    /**
     * 识别 PDF
     */
    OcrResult recognizePdf(String pdfPath, OcrConfig config);

    /**
     * 支持的语言列表
     */
    List<String> getSupportedLanguages();

    /**
     * 是否可用
     */
    boolean isAvailable();
}

public class TesseractOcr implements OcrProvider {
    // Tesseract 实现
}

public class BaiduOcr implements OcrProvider {
    // 百度云 OCR 实现
}

public class OcrEngine {
    private List<OcrProvider> providers;

    /**
     * 自动选择最佳提供商
     */
    public OcrResult recognize(String imagePath, OcrConfig config);
}
```

---

## 五、实施步骤

### Week 1-2: PDF 合并/拆分

- [ ] 创建 PdfEditor 工具类
- [ ] 实现 PDF 合并功能
- [ ] 实现 PDF 拆分功能
- [ ] 添加书签支持
- [ ] 创建 PdfController
- [ ] 单元测试

### Week 3-4: PDF 页面操作

- [ ] 实现页面提取
- [ ] 实现页面旋转
- [ ] 实现页面删除
- [ ] PDF 信息获取
- [ ] 性能优化
- [ ] 集成测试

### Week 5-6: 水印处理

- [ ] 创建 WatermarkUtil
- [ ] PDF 文字水印
- [ ] PDF 图片水印
- [ ] Word 水印
- [ ] PPT 水印（可选）
- [ ] 创建 WatermarkController

### Week 7-8: PDF 安全

- [ ] 创建 PdfSecurityUtil
- [ ] PDF 加密功能
- [ ] PDF 解密功能
- [ ] 权限设置
- [ ] 创建 SecurityController
- [ ] 安全测试

### Week 9-10: OCR 基础

- [ ] 安装 Tesseract 引擎
- [ ] 创建 OcrProvider 接口
- [ ] 实现 TesseractOcr
- [ ] 图片 OCR 功能
- [ ] 创建 OcrController

### Week 11-12: OCR 进阶

- [ ] 集成百度云 OCR
- [ ] PDF OCR 功能
- [ ] 图像预处理优化
- [ ] 多语言支持
- [ ] 表格 OCR（可选）
- [ ] 完整测试

---

## 六、配置说明

### 6.1 Tesseract 配置

```yaml
ocr:
  tesseract:
    dataPath: /usr/local/share/tessdata  # 训练数据路径
    defaultLanguage: chi_sim+eng
    availableLanguages:
      - chi_sim
      - chi_tra
      - eng
      - jpn
      - kor
```

### 6.2 百度云 OCR 配置

```yaml
ocr:
  baidu:
    enabled: true
    appId: ${BAIDU_OCR_APP_ID}
    apiKey: ${BAIDU_OCR_API_KEY}
    secretKey: ${BAIDU_OCR_SECRET_KEY}
```

### 6.3 水印默认配置

```yaml
watermark:
  pdf:
    defaultFontSize: 48
    defaultOpacity: 0.3
    defaultRotation: 45
    defaultColor: "#FF0000"
```

---

## 七、风险与对策

| 风险 | 影响 | 对策 |
|-----|------|------|
| Tesseract 训练数据大 | 部署包体积大 | 按需下载语言包，提供精简版 |
| 扫描版 PDF OCR 慢 | 用户体验差 | 异步处理 + 进度通知 |
| 加密 PDF 处理 | 法律风险 | 明确使用场景，添加审计日志 |
| iText 商业许可 | 许可问题 | 使用 PDFBox 为主，iText 可选 |

---

## 八、验收标准

- [ ] PDF 合并：支持 100 个文件合并，总页数 1000+
- [ ] PDF 拆分：支持 1000 页文件拆分
- [ ] 水印：支持批量处理，100 文件 < 60s
- [ ] 加密：支持 AES-256 加密
- [ ] OCR：中文识别准确率 > 95%，英文 > 98%
- [ ] OCR：单页处理时间 < 3s
- [ ] 所有功能单元测试覆盖率 > 80%
