# Phase 6: OCR 与文档转换

## 目标
集成 OCR 能力，实现图片文字识别、扫描件 PDF 转可搜索 PDF、PDF 与 Office 格式互转。

## 功能清单

### 1. 图片 OCR

```
POST /api/ocr/extract
Content-Type: multipart/form-data

参数:
- file: File (图片文件)
- language: String (eng, chi_sim, chi_tra, jpn)
- enhance: Boolean (是否图像增强)

响应:
{
  "success": true,
  "data": {
    "text": "识别出的文字内容...",
    "confidence": 0.95,
    "pages": [
      {
        "pageNum": 1,
        "text": "...",
        "blocks": [
          {
            "text": "标题",
            "confidence": 0.98,
            "bbox": {"x": 100, "y": 50, "width": 200, "height": 30}
          }
        ]
      }
    ],
    "language": "chi_sim",
    "processTime": 1250
  }
}
```

### 2. PDF OCR（扫描件转可搜索 PDF）

```
POST /api/ocr/pdf/convert

请求:
{
  "sourcePath": "/uploads/scan.pdf",
  "targetPath": "/outputs/searchable.pdf",
  "language": "chi_sim",
  "dpi": 300
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/searchable.pdf",
    "pagesProcessed": 10,
    "textLayerAdded": true,
    "ocrConfidence": 0.92
  }
}
```

### 3. PDF 转 Word

```
POST /api/convert/pdf-to-word

请求:
{
  "sourcePath": "/uploads/document.pdf",
  "targetPath": "/outputs/document.docx",
  "preserveFormatting": true,  // 保留格式
  "extractImages": true        // 提取图片
}
```

### 4. PDF 转 Excel

```
POST /api/convert/pdf-to-excel

请求:
{
  "sourcePath": "/uploads/table.pdf",
  "targetPath": "/outputs/table.xlsx",
  "detectTables": true,      // 自动检测表格
  "ocrEnabled": true         // 对扫描件启用 OCR
}
```

### 5. PDF 转 PPT

```
POST /api/convert/pdf-to-ppt

请求:
{
  "sourcePath": "/uploads/slides.pdf",
  "targetPath": "/outputs/slides.pptx",
  "slidePerPage": true,      // 每页一张幻灯片
  "extractAnimations": false
}
```

### 6. PPT 转 PDF

```
POST /api/convert/ppt-to-pdf

请求:
{
  "sourcePath": "/uploads/presentation.pptx",
  "targetPath": "/outputs/presentation.pdf",
  "quality": "high",         // low, medium, high
  "includeNotes": false      // 是否包含备注
}
```

### 7. Word 转 PDF

```
POST /api/convert/word-to-pdf

请求:
{
  "sourcePath": "/uploads/document.docx",
  "targetPath": "/outputs/document.pdf",
  "pdfStandard": "PDF/A-1b"  // PDF 标准
}
```

### 8. Excel 转 PDF

```
POST /api/convert/excel-to-pdf

请求:
{
  "sourcePath": "/uploads/sheet.xlsx",
  "targetPath": "/outputs/sheet.pdf",
  "sheetIndex": 0,
  "fitToPage": true,
  "orientation": "landscape"
}
```

## 技术选型

### OCR 引擎

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| Tesseract (本地) | 免费、离线 | 中文识别率一般 | 简单文档 |
| PaddleOCR (本地) | 中文识别率高、支持表格 | 模型较大 | 中文文档、表格 |
| 百度 OCR API | 识别率高、功能全 | 收费、依赖网络 | 高精度需求 |
| 腾讯 OCR API | 识别率高、速度快 | 收费 | 企业级应用 |
| Azure Computer Vision | 多语言支持好 | 国外服务 | 多语言文档 |

**推荐**: 本地使用 PaddleOCR，云端对接百度/腾讯 API

### 文档转换方案

```java
@Service
public class DocumentConversionService {
    
    /**
     * PDF 转 Word - 使用 Apache POI + PDFBox
     */
    public void pdfToWord(String sourcePath, String targetPath, 
                          ConversionOptions options) {
        try (PDDocument pdf = Loader.loadPDF(new File(sourcePath));
             XWPFDocument word = new XWPFDocument()) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String text = stripper.getText(pdf);
                
                XWPFParagraph para = word.createParagraph();
                XWPFRun run = para.createRun();
                run.setText(text);
                
                // 提取图片
                if (options.isExtractImages()) {
                    extractImages(pdf.getPage(i), word);
                }
            }
            
            word.write(new FileOutputStream(targetPath));
        }
    }
    
    /**
     * Word 转 PDF - 使用 LibreOffice 命令行
     */
    public void wordToPdf(String sourcePath, String targetPath) {
        String cmd = String.format(
            "libreoffice --headless --convert-to pdf --outdir %s %s",
            new File(targetPath).getParent(),
            sourcePath
        );
        executeCommand(cmd);
    }
    
    /**
     * PPT 转 PDF - 使用 Apache POI
     */
    public void pptToPdf(String sourcePath, String targetPath,
                         ConversionOptions options) {
        // 使用 XMLSlideShow 读取 PPTX
        // 使用 PDFBox 生成 PDF
        // 每页幻灯片转换为 PDF 一页
    }
}
```

## OCR 实现

```java
@Service
public class OcrService {
    
    @Autowired
    private OcrProperties ocrProperties;
    
    /**
     * 本地 OCR (PaddleOCR)
     */
    public OcrResult extractLocal(String imagePath, String language) {
        // 使用 PaddleOCR Java API
        PaddleOCR ocr = new PaddleOCR();
        
        List<OCRPredictResult> results = ocr.runOcr(imagePath);
        
        StringBuilder text = new StringBuilder();
        List<TextBlock> blocks = new ArrayList<>();
        
        for (OCRPredictResult result : results) {
            String lineText = result.getText();
            text.append(lineText).append("\n");
            
            blocks.add(TextBlock.builder()
                .text(lineText)
                .confidence(result.getScore())
                .bbox(convertBox(result.getBox()))
                .build());
        }
        
        return OcrResult.builder()
            .text(text.toString())
            .blocks(blocks)
            .language(language)
            .build();
    }
    
    /**
     * 百度 OCR API
     */
    public OcrResult extractBaidu(String imagePath, String language) {
        // 1. 获取 Access Token
        String token = baiduAuthService.getAccessToken();
        
        // 2. 调用通用文字识别 API
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
        
        // 3. 解析响应
        // 返回文字、位置、置信度
    }
    
    /**
     * PDF OCR - 转可搜索 PDF
     */
    public String convertToSearchablePdf(String sourcePath, String targetPath,
                                         String language) {
        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                // 1. 渲染页面为图片
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                
                // 2. OCR 识别文字
                String pageText = ocrImage(image, language);
                
                // 3. 添加文字层到 PDF
                PDPage page = document.getPage(i);
                addTextLayer(page, pageText);
            }
            
            document.save(targetPath);
        }
        
        return targetPath;
    }
}
```

## Dockerfile 增强（添加 OCR 支持）

```dockerfile
# Dockerfile.worker
FROM openjdk:21-jdk-slim

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    # LibreOffice for document conversion
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    # Tesseract OCR
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-chi-tra \
    tesseract-ocr-eng \
    # PaddleOCR 依赖
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    # FFmpeg
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 下载 PaddleOCR 模型
RUN mkdir -p /app/models \
    && cd /app/models \
    && wget https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.tar \
    && wget https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.tar \
    && tar -xvf ch_PP-OCRv4_det_infer.tar \
    && tar -xvf ch_PP-OCRv4_rec_infer.tar

# 复制应用
COPY target/file-processor-worker.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## API 端点汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/ocr/extract` | 图片 OCR |
| POST | `/api/ocr/pdf/convert` | PDF OCR 转可搜索 PDF |
| POST | `/api/convert/pdf-to-word` | PDF 转 Word |
| POST | `/api/convert/pdf-to-excel` | PDF 转 Excel |
| POST | `/api/convert/pdf-to-ppt` | PDF 转 PPT |
| POST | `/api/convert/ppt-to-pdf` | PPT 转 PDF |
| POST | `/api/convert/word-to-pdf` | Word 转 PDF |
| POST | `/api/convert/excel-to-pdf` | Excel 转 PDF |

## 配置

```yaml
ocr:
  # 默认 OCR 引擎: tesseract, paddle, baidu, tencent
  default-engine: paddle
  
  tesseract:
    data-path: /usr/share/tesseract-ocr/4.00/tessdata
    default-language: chi_sim+eng
    
  paddle:
    model-path: /app/models
    use-gpu: false
    
  baidu:
    app-id: ${BAIDU_OCR_APP_ID}
    api-key: ${BAIDU_OCR_API_KEY}
    secret-key: ${BAIDU_OCR_SECRET_KEY}
    
  tencent:
    secret-id: ${TENCENT_OCR_SECRET_ID}
    secret-key: ${TENCENT_OCR_SECRET_KEY}

conversion:
  libreoffice:
    path: /usr/lib/libreoffice
    timeout: 300  # seconds
```

## 验收标准

- [ ] 支持图片 OCR 文字提取（中文/英文/日文）
- [ ] 支持 PDF 扫描件转可搜索 PDF
- [ ] 支持 PDF 转 Word（保留基本格式）
- [ ] 支持 PDF 转 Excel（表格识别）
- [ ] 支持 PDF 转 PPT
- [ ] 支持 PPT 转 PDF
- [ ] 支持 Word 转 PDF
- [ ] 支持 Excel 转 PDF
- [ ] OCR 识别准确率 > 85%
- [ ] 支持批量 OCR 处理
