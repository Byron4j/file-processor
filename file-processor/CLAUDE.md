# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=FileConvertTest

# Package into JAR
mvn package

# Run the application
java -jar target/file-processor-1.0.0.jar

# Run with Maven Spring Boot plugin
mvn spring-boot:run
```

## Architecture Overview

This is a Spring Boot 3 file processing REST API service using Java 21.

### Layer Structure

- **Controller**: REST endpoints, input validation using Jakarta Validation, delegates to service layer. Request DTOs are often nested static classes within controllers.
- **Service**: Business logic, file validation, orchestrates utility classes. Returns `FileResponse` objects.
- **Util**: Static utility classes containing the actual file processing logic using third-party libraries.

### Key Design Patterns

- **Builder pattern**: `FileResponse` uses a builder for construction with `data` field for flexible payloads
- **Static utility classes**: File processing logic is in stateless static methods (e.g., `PdfEditor.merge()`, `TextExtractor.extractText()`)
- **Configuration classes**: Nested static classes for complex operations (e.g., `SplitConfig`, `TextWatermarkConfig`, `EncryptionConfig`)
- **Validation**: Jakarta Validation annotations on request DTOs with `@Valid` in controllers
- **Global exception handling**: `GlobalExceptionHandler` uses `@RestControllerAdvice` to map exceptions to `FileResponse`

### Dependencies

- **Apache POI** (5.2.5): Office document processing - `HWPFDocument` for .doc, `XWPFDocument` for .docx, `HSLFSlideShow` for .ppt, `XMLSlideShow` for .pptx
- **Apache PDFBox** (3.0.1): PDF processing using `Loader.loadPDF()` API (not the deprecated `PDDocument.load()`), `PDFTextStripper` for text extraction
- **Thumbnailator** (0.4.20): Image thumbnail generation and format conversion
- **Apache Commons CSV** (1.10.0): Excel to CSV conversion
- **SevenZipJBinding** (16.02): 7z archive extraction
- **JunRar** (7.5.5): RAR archive extraction
- **Jsoup** (1.17.1): HTML parsing for extracting text from HTML content
- **Hutool** (5.8.23): Java utility library
- **JAVE2** (3.3.1): FFmpeg wrapper for audio/video processing (note: uses direct command execution via `Runtime.exec()` due to API compatibility)
- **HanLP** (1.8.4): Chinese NLP for keyword extraction and text segmentation
- **OpenNLP** (2.3.0): Named entity recognition for document classification
- **OpenAI SDK** (0.18.2): OpenAI API integration for AI features
- **Anthropic SDK** (0.1.0): Claude API integration for AI features
- **Velocity** (2.3): Template engine for document generation
- **Alipay SDK** (4.38.10.ALL): Alipay payment integration
- **WeChat Pay SDK** (0.2.12): WeChat Pay integration

## API Endpoints

### Document Processing
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/file/convert/doc-to-docx` | Convert .doc to .docx format |
| POST | `/api/file/extract/zip-index-html` | Extract index.html from ZIP to plain text |
| POST | `/api/file/extract/text` | Extract text from DOC/DOCX/PDF/PPT/PPTX |

### Excel Processing (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/excel/extract/text` | Extract text from Excel |
| POST | `/api/excel/convert/csv` | Convert Excel to CSV |
| POST | `/api/excel/convert/json` | Convert Excel to JSON |
| GET | `/api/excel/info` | Get Excel sheet information |

### Image Processing (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/image/convert` | Convert image format (JPEG, PNG, GIF, BMP, WebP, TIFF) |
| POST | `/api/image/thumbnail` | Generate thumbnail (FIT/FILL/SCALE modes) |
| POST | `/api/image/compress` | Compress image with quality setting |
| GET | `/api/image/info` | Get image metadata |

### File Verification (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/file/hash` | Calculate file hash (MD5, SHA-1, SHA-256, SHA-384, SHA-512) |
| POST | `/api/file/hashes` | Calculate multiple hashes in single pass |
| POST | `/api/file/hash/verify` | Verify file hash against expected value |
| GET | `/api/file/hash/algorithms` | List supported hash algorithms |

### Archive Processing (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/archive/extract/7z` | Extract 7z archive (password support) |
| POST | `/api/archive/extract/rar` | Extract RAR archive |
| POST | `/api/archive/extract` | Auto-detect format and extract |
| GET | `/api/archive/info` | Get archive information |

### PDF Processing (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/pdf/merge` | Merge multiple PDFs with optional bookmarks |
| POST | `/api/pdf/split` | Split PDF by ranges, every N pages, or extract mode |
| POST | `/api/pdf/extract` | Extract specific pages to a new PDF |
| POST | `/api/pdf/rotate` | Rotate pages by 90/180/270 degrees |
| POST | `/api/pdf/delete-pages` | Delete specific pages from PDF |
| GET | `/api/pdf/info` | Get PDF metadata (page count, author, encryption status) |

### Watermark (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/watermark/pdf/text` | Add text watermark to PDF with rotation/opacity |
| POST | `/api/watermark/pdf/image` | Add image watermark to PDF |
| POST | `/api/watermark/word/text` | Add text watermark to Word document |

### PDF Security (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/security/pdf/encrypt` | Encrypt PDF with user/owner passwords and permissions |
| POST | `/api/security/pdf/decrypt` | Decrypt PDF with password |
| GET | `/api/security/pdf/check` | Check PDF encryption status and permissions |

### Task Management (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks/submit` | Submit async task |
| GET | `/api/tasks/{taskId}/status` | Get task status and progress |
| POST | `/api/tasks/{taskId}/cancel` | Cancel pending/processing task |
| GET | `/api/tasks` | List tasks with filters |

### Batch Processing (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/batch/convert` | Batch format conversion |
| POST | `/api/batch/watermark` | Batch add watermark |
| POST | `/api/batch/extract` | Batch text extraction |
| POST | `/api/batch/hash` | Batch calculate file hashes (sync) |

### File Metadata (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/register` | Register file metadata |
| GET | `/api/files/{fileId}` | Get file metadata |
| GET | `/api/files` | List files with filters |
| POST | `/api/files/{fileId}/tags` | Update file tags |
| DELETE | `/api/files/{fileId}` | Delete file (soft) |
| GET | `/api/files/categories` | Get file categories |
| GET | `/api/files/statistics` | Get file statistics |

### File Preview (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/preview/{fileId}` | Get preview info |
| GET | `/api/preview/{fileId}/content` | Get preview content |

### Media Processing (Phase 4)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/media/video/info` | Get video metadata (duration, resolution, codec) |
| GET | `/api/media/audio/info` | Get audio metadata (duration, bitrate, sample rate) |
| POST | `/api/media/video/thumbnail` | Extract thumbnail at timestamp |
| POST | `/api/media/video/transcode` | Transcode video with custom settings |
| POST | `/api/media/audio/transcode` | Transcode audio with custom settings |

### Document Intelligence (Phase 4)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/intelligence/classify` | Classify document type (CONTRACT, INVOICE, etc.) |
| POST | `/api/intelligence/sensitive-info` | Detect sensitive information (ID, phone, email, bank) |
| POST | `/api/intelligence/keywords` | Extract keywords using HanLP |
| POST | `/api/intelligence/summary` | Generate document summary |

### AI Integration (Phase 4)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/summary` | AI-powered document summary (Claude/OpenAI) |
| POST | `/api/ai/tags` | AI-generated tags for document |
| POST | `/api/ai/ask` | Document Q&A with AI |

### Template Engine (Phase 4)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/template/word/render` | Render Word template with data |
| POST | `/api/template/placeholders` | Get template placeholder variables |
| POST | `/api/template/batch-render` | Batch render templates (async) |

### Payment & Subscription (Commercial)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payment/create` | Create payment order (Alipay/WeChat) |
| POST | `/api/payment/alipay/notify` | Alipay callback notification |
| POST | `/api/payment/wechat/notify` | WeChat Pay callback notification |
| GET | `/api/payment/status/{orderNo}` | Query payment order status |
| GET | `/api/subscription/plans` | List all subscription plans |
| GET | `/api/subscription/plans/{planId}` | Get subscription plan detail |
| GET | `/api/subscription/my` | Get current user subscription |
| GET | `/api/subscription/history` | Get subscription history |

### File Fingerprint (Instant Transfer)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/fingerprint/check` | Check if file exists for instant transfer |
| POST | `/api/fingerprint/instant-transfer` | Perform instant file transfer |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/file/info?path={path}` | Get file metadata |
| GET | `/api/file/health` | Health check |

All endpoints return `FileResponse` JSON with `success`, `message`, `filePath`, `fileSize`, and optional `data` fields.

## File Processing Details

### DOC to DOCX Conversion
- Direct POI API conversion preserving formatting (bold, italic, underline, font size, color)
- Handles paragraph and character run-level formatting

### ZIP HTML Extraction
- Case-insensitive search for "index.html" anywhere in the archive
- Strips script/style tags using Jsoup, converts headers/paragraphs/lists to formatted text
- Outputs UTF-8 text file

### Document Text Extraction (TextExtractor)
- Supports DOC, DOCX, PDF, PPT, PPTX via `isSupportedFormat()`
- DOC: `WordExtractor` from poi-scratchpad
- DOCX: `XWPFWordExtractor`, preserves table content with cell separators
- PDF: `Loader.loadPDF()` + `PDFTextStripper`
- PPT: `HSLFSlideShow` + `SlideShowExtractor`
- PPTX: `XMLSlideShow` + `SlideShowExtractor`
- PPT/PPTX extracts text per slide with "=== Slide N ===" markers

### Excel Processing (ExcelProcessor)
- Supports XLS and XLSX formats
- Text extraction returns row/column data structure via `ExcelContent`
- CSV conversion: Apache Commons CSV with configurable delimiter
- JSON conversion: supports header mapping, column mapping (A→name, B→age)

### Image Processing (ImageProcessor)
- Format conversion via Thumbnailator: JPEG, PNG, GIF, BMP, WebP, TIFF
- Thumbnail generation: FIT (fit within), FILL (crop to fill), SCALE (resize)
- Compression: configurable quality (0.0-1.0) and max dimensions

### Archive Processing (ArchiveUtil)
- 7z: SevenZipJBinding with `ISimpleInArchive` interface
- RAR: JunRar library
- Password support for encrypted archives
- Info extraction: total files, compressed/uncompressed size, entry list

### PDF Processing (PdfEditor)
- Merge: `PDFMergerUtility` from pdfbox-tools
- Split: by page ranges, every N pages, or extract specific pages
- Rotate: modifies page rotation metadata (90/180/270 degrees)
- Delete: removes pages in reverse order to avoid index shifting
- Info: extracts document metadata and page dimensions

### Watermark (WatermarkUtil)
- PDF text: uses `PDPageContentStream` with transparency via `PDExtendedGraphicsState`
- PDF image: `PDImageXObject.createFromFile()` with scale/opacity/position
- Word: adds text to header using `XWPFHeader` with `HeaderFooterType.DEFAULT`

### PDF Security (PdfSecurityUtil)
- Encryption: `StandardProtectionPolicy` with AES-128/256
- Permissions: print, modify, copy, annotate
- Decryption: `Loader.loadPDF(file, password)` then `setAllSecurityToBeRemoved(true)`

### Async Task Framework (Phase 3)
- Task submission: `TaskService.submitTask()` returns task ID immediately
- Task execution: `@Async("taskExecutor")` runs tasks in thread pool
- Progress tracking: `DefaultProgressListener` updates database progress
- Task status: PENDING → PROCESSING → SUCCESS/FAILED/CANCELLED

### Batch Processing (Phase 3)
- Batch convert: async task with file list, target format, output directory
- Batch watermark: applies text/image watermark to multiple files
- Batch extract: extracts text from multiple documents
- Batch hash: synchronous operation for integrity verification

### Storage Service (Phase 3)
- `StorageService` interface with `StorageType` (LOCAL, MINIO, ALIYUN_OSS)
- `LocalStorageService`: file system storage with base path configuration
- `MinioStorageService`: MinIO object storage with bucket management
- `AliyunOssService`: Alibaba Cloud OSS with presigned URLs
- `StorageManager`: auto-detects and routes to configured storage

### File Metadata Management (Phase 3)
- `FileMetadata` entity with JSON fields for tags and metadata
- `MetadataService`: register, query, categorize, tag files
- Categories: DOCUMENT, SPREADSHEET, PRESENTATION, IMAGE, ARCHIVE, MEDIA, OTHER
- Soft delete with reference counting for deduplication

### File Preview (Phase 3)
- Preview types: PDF, IMAGE, TEXT based on file extension
- `PreviewService.getPreviewInfo()`: returns preview type and URL
- `PreviewService.getPreviewContent()`: streams file content

### Media Processing (Phase 4) - `MediaProcessor`
- Uses direct FFmpeg command execution via `Runtime.exec()` (JAVE2 wrapper had API compatibility issues)
- `getVideoInfo()`: ffprobe to extract duration, resolution, codec, bitrate
- `getAudioInfo()`: ffprobe to extract duration, sample rate, channels
- `extractThumbnail()`: ffmpeg -ss timestamp -vframes 1 with optional scale
- `transcodeVideo()`: ffmpeg with configurable codec, resolution, bitrate
- `transcodeAudio()`: ffmpeg with configurable codec, sample rate, channels

### Document Intelligence (Phase 4)
- **Document Classification** (`DocumentClassifier`): rule-based classification using keyword detection for CONTRACT, INVOICE, REPORT, RESUME, etc.
- **Sensitive Info Detection** (`SensitiveInfoDetector`): regex patterns for ID_CARD (15/18 digits), PHONE (11 digits), EMAIL, BANK_CARD
- **Keyword Extraction** (`KeywordExtractor`): HanLP TextRank algorithm with word segmentation
- **Text Summarization** (`TextSummarizer`): extractive summarization using TF-IDF sentence scoring

### AI Integration (Phase 4) - `LlmClient`
- Supports Claude (Anthropic) and OpenAI APIs via configuration (`ai.provider`)
- `chat()`: general conversation with system prompt and user message
- `summarize()`: document summarization with configurable max tokens
- `askQuestion()`: document Q&A with context from extracted text
- `generateTags()`: AI-generated tags based on document content
- Response parsing extracts answer, confidence score, and source references

### Template Engine (Phase 4) - `WordTemplateEngine`
- Placeholder syntax: `${variableName}` in Word documents
- Supports text replacement in paragraphs and tables
- Dynamic table rows: `${tableField}` in table cells creates row per data item
- Data context: Map<String, Object> passed to render() method
- Placeholder extraction: scans document for all `${...}` patterns

### Payment Integration (Commercial) - `PaymentService`
生产级支付服务，支持支付宝和微信支付，包含完整的幂等性控制、事务管理和回调处理。

**Alipay Integration**:
- `createAlipayPayment()`: 创建支付宝网页支付，返回支付URL
  - 支持设置notifyUrl和returnUrl
  - 订单超时时间为30分钟
  - 使用RSA2签名算法
- `handleAlipayCallback()`: 处理支付宝异步通知
  - 幂等性检查：使用Redis防止重复处理
  - 订单锁：使用分布式锁防止并发处理
  - 金额校验：验证回调金额与订单金额一致
  - 自动激活订阅
- `queryAlipayOrderStatus()`: 主动查询订单支付状态
- `alipayRefund()`: 支付宝退款（支持部分退款）

**WeChat Pay Integration**:
- `createWechatPayment()`: 创建微信Native支付，返回支付二维码URL
  - 金额转换：元转分（乘以100）
  - 设置超时时间
  - 必须设置sceneInfo包含用户IP
- `handleWechatCallback()`: 处理微信支付回调
  - 完整的签名验证（使用微信支付SDK的NotificationParser）
  - APIv3解密resource字段
  - 幂等性控制和订单锁
  - 金额校验
- `queryWechatOrderStatus()`: 主动查询微信支付订单状态
- `wechatRefund()`: 微信退款（TODO: 需完善实现）

**Order Management**:
- `PaymentOrder` 实体记录订单生命周期：PENDING → PAID/CLOSED/FAILED/REFUNDED
- `createPaymentOrder()`: 创建订单（带幂等性检查）
- `queryOrderStatus()`: 查询订单状态，自动处理超时订单
- `closeExpiredOrders()`: 定时任务关闭超时订单（每5分钟执行）
- `applyRefund()`: 退款申请，支持全额和部分退款

**幂等性机制**:
- 订单创建：使用Redis setIfAbsent防止重复创建
- 回调处理：使用Redis记录已处理的回调通知ID
- 订单锁：使用Redis分布式锁防止并发更新订单状态
- 订阅激活：数据库唯一约束防止重复激活

**退款管理**:
- `RefundRecord` 实体记录退款流水
- 支持全额退款和部分退款
- 退款后自动停用订阅

**配置示例**:
```yaml
payment:
  alipay:
    app-id: your_app_id
    private-key: your_private_key
    public-key: alipay_public_key
    notify-url: https://your-domain.com/api/payment/alipay/notify
    return-url: https://your-domain.com/payment/success
  wechat:
    mch-id: your_mch_id
    app-id: your_app_id
    api-v3-key: your_api_v3_key
    mch-serial-no: your_mch_serial_no
    private-key-path: /path/to/apiclient_key.pem
    notify-url: https://your-domain.com/api/payment/wechat/notify
```

### File Fingerprint (Instant Transfer) - `FileFingerprintService`
- **Fingerprint Calculation**: MD5 and SHA256 hash computation for file deduplication
- **Instant Transfer Check**: `checkFileExists()` verifies if file already exists by MD5 + size
- **Reference Counting**: Tracks how many users reference the same physical file
- **Storage Optimization**: Physical file deleted only when reference count reaches zero
- **Integration**: Works with `FileMetadata` to link user files to fingerprints

## Testing

Tests use `@SpringBootTest` for integration testing with the full Spring context. Tests create temporary files in `./test-files/` and clean up after execution.

## Development Roadmap

See [ROADMAP.md](ROADMAP.md) for the project's development plan.

Detailed design documents for each phase:
- ✅ [Phase 1: Core capabilities](docs/design/phase1-core-capabilities.md) - Excel, image processing, file verification, 7z/rar (COMPLETED)
- ✅ [Phase 2: Advanced features](docs/design/phase2-advanced-features.md) - PDF tools, watermarks, encryption (COMPLETED)
- ✅ [Phase 3: Enterprise features](docs/design/phase3-enterprise-features.md) - Async tasks, batch processing, multi-storage, file metadata, preview (COMPLETED)
- ✅ [Phase 4: AI integration](docs/design/phase4-advanced-features.md) - Audio/video processing, document intelligence, AI summary/tags, template engine (COMPLETED)

When implementing features from a specific phase, refer to the corresponding design document for architecture, API specifications, database schema, implementation steps, and acceptance criteria.
