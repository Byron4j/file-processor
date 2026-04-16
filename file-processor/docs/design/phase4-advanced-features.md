# 第四阶段设计方案：高级特性

## 概述

**目标**：AI 集成、音视频处理、文档智能
**时间周期**：4-6 个月
**核心功能**：音视频处理、文档智能分类、敏感信息检测、AI 摘要、模板引擎

---

## 一、功能清单

### 1.1 音视频处理

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 视频信息提取 | 获取视频时长、分辨率、码率、编码格式 | P1 |
| 音频信息提取 | 获取音频时长、采样率、声道、比特率 | P1 |
| 视频封面提取 | 提取指定时间帧作为封面 | P1 |
| 视频转码 | 视频格式转换（MP4、AVI、MOV 等） | P2 |
| 音频转码 | 音频格式转换（MP3、WAV、AAC 等） | P2 |
| 音频转文字 | 语音识别转文字 | P2 |
| 视频压缩 | 视频压缩、分辨率调整 | P3 |

### 1.2 文档智能

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 文档分类 | 自动识别文档类型（合同、发票、报告） | P2 |
| 敏感信息检测 | 检测身份证号、手机号、银行卡号 | P2 |
| 关键词提取 | 提取文档关键词 | P2 |
| 实体识别 | 识别人名、地名、组织机构 | P3 |
| 情感分析 | 分析文档情感倾向 | P3 |
| 自动摘要 | 生成文档摘要 | P3 |

### 1.3 模板引擎

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| Word 模板填充 | 基于模板生成 Word 文档 | P1 |
| PDF 表单填充 | 填充 PDF 表单字段 | P1 |
| 批量生成 | 批量生成文档 | P2 |
| 动态表格 | 支持动态行数的表格填充 | P2 |

### 1.4 AI 集成

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 文档问答 | 基于文档内容的问答 | P3 |
| 智能标签 | 自动生成文档标签 | P3 |
| 文档对比 | 对比两份文档差异 | P3 |
| 翻译 | 文档自动翻译 | P3 |

---

## 二、技术架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          AI Layer                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │  LLM API     │ │  NLP Engine  │ │  ML Models   │            │
│  │  (Claude/    │ │  (HanLP/     │ │  (TensorFlow/│            │
│  │   OpenAI)    │ │   jieba)     │ │   ONNX)      │            │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘            │
└─────────┼────────────────┼────────────────┼────────────────────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │ Document    │  │ Media       │  │ Template    │
   │ Intelligence│  │ Processor   │  │ Engine      │
   │             │  │             │  │             │
   │ - 分类      │  │ - FFmpeg    │  │ - Word      │
   │ - 敏感检测  │  │ - JAVE2     │  │ - PDF       │
   │ - 摘要      │  │ - Whisper   │  │ - 批量生成  │
   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │ Core File   │  │ Task Queue  │  │ Metadata    │
   │ Service     │  │             │  │ Storage     │
   └─────────────┘  └─────────────┘  └─────────────┘
```

### 2.2 模块结构

```
com.fileprocessor
├── controller
│   ├── MediaController.java           # 音视频 API
│   ├── DocumentIntelligenceController.java  # 文档智能 API
│   ├── TemplateController.java        # 模板引擎 API
│   └── AiAssistantController.java     # AI 助手 API
├── service
│   ├── MediaService.java
│   ├── DocumentIntelligenceService.java
│   ├── TemplateService.java
│   └── AiAssistantService.java
├── media
│   ├── VideoProcessor.java            # 视频处理
│   ├── AudioProcessor.java            # 音频处理
│   ├── FFmpegExecutor.java            # FFmpeg 执行器
│   └── WhisperClient.java             # 语音识别客户端
├── intelligence
│   ├── DocumentClassifier.java        # 文档分类器
│   ├── SensitiveInfoDetector.java     # 敏感信息检测
│   ├── KeywordExtractor.java          # 关键词提取
│   ├── EntityRecognizer.java          # 实体识别
│   └── Summarizer.java                # 摘要生成
├── template
│   ├── WordTemplateEngine.java        # Word 模板引擎
│   ├── PdfTemplateEngine.java         # PDF 模板引擎
│   ├── TemplateParser.java            # 模板解析器
│   └── DataBinder.java                # 数据绑定
├── ai
│   ├── LlmClient.java                 # LLM 客户端
│   ├── PromptBuilder.java             # 提示词构建
│   └── AiResponseParser.java          # AI 响应解析
└── ml
    ├── ModelManager.java              # 模型管理
    └── FeatureExtractor.java          # 特征提取
```

### 2.3 依赖项

```xml
<!-- FFmpeg wrapper -->
<dependency>
    <groupId>ws.schild</groupId>
    <artifactId>jave-core</artifactId>
    <version>3.3.1</version>
</dependency>
<dependency>
    <groupId>ws.schild</groupId>
    <artifactId>jave-all-deps</artifactId>
    <version>3.3.1</version>
</dependency>

<!-- Whisper (via HTTP API or subprocess) -->
<!-- 使用系统安装的 Whisper 或 OpenAI API -->

<!-- HanLP 中文 NLP -->
<dependency>
    <groupId>com.hankcs</groupId>
    <artifactId>hanlp</artifactId>
    <version>portable-1.8.4</version>
</dependency>

<!-- OpenNLP (实体识别) -->
<dependency>
    <groupId>org.apache.opennlp</groupId>
    <artifactId>opennlp-tools</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- Anthropic Claude SDK -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- OpenAI SDK -->
<dependency>
    <groupId>com.openai</groupId>
    <artifactId>openai-java</artifactId>
    <version>0.18.2</version>
</dependency>

<!-- Velocity (模板引擎备选) -->
<dependency>
    <groupId>org.apache.velocity</groupId>
    <artifactId>velocity-engine-core</artifactId>
    <version>2.3</version>
</dependency>

<!-- DL4J (可选，用于本地 ML) -->
<dependency>
    <groupId>org.deeplearning4j</groupId>
    <artifactId>deeplearning4j-core</artifactId>
    <version>1.0.0-M2.1</version>
</dependency>
```

---

## 三、API 设计

### 3.1 音视频处理 API

#### POST /api/media/video/info
获取视频信息

```json
// Request
{
  "filePath": "/path/to/video.mp4"
}

// Response
{
  "success": true,
  "data": {
    "format": "mp4",
    "duration": 120.5,
    "width": 1920,
    "height": 1080,
    "videoCodec": "h264",
    "audioCodec": "aac",
    "videoBitrate": 5000000,
    "audioBitrate": 128000,
    "frameRate": 30.0,
    "fileSize": 15728640
  }
}
```

#### POST /api/media/video/thumbnail
提取视频封面

```json
// Request
{
  "sourcePath": "/path/to/video.mp4",
  "targetPath": "/path/to/thumbnail.jpg",
  "timestamp": 5.0,  // 第 5 秒
  "width": 320,
  "height": 180
}
```

#### POST /api/media/video/transcode
视频转码

```json
// Request
{
  "sourcePath": "/path/to/video.avi",
  "targetPath": "/path/to/output.mp4",
  "format": "mp4",
  "videoCodec": "h264",
  "audioCodec": "aac",
  "width": 1280,
  "height": 720,
  "videoBitrate": 2000000,
  "audioBitrate": 128000,
  "async": true
}
```

#### POST /api/media/audio/transcribe
音频/视频转文字

```json
// Request
{
  "sourcePath": "/path/to/audio.mp3",
  "language": "zh",
  "model": "whisper-1",  // 或 whisper-large-v3
  "async": true
}

// Response
{
  "success": true,
  "data": {
    "taskId": "t-xxx-xxx",
    "status": "PROCESSING"
  }
}

// Callback result
{
  "text": "识别出的完整文字内容...",
  "segments": [
    {
      "start": 0.0,
      "end": 5.5,
      "text": "第一段文字",
      "confidence": 0.95
    }
  ],
  "language": "zh",
  "duration": 120.5
}
```

### 3.2 文档智能 API

#### POST /api/intelligence/classify
文档自动分类

```json
// Request
{
  "filePath": "/path/to/document.pdf"
}

// Response
{
  "success": true,
  "data": {
    "primaryCategory": "合同",
    "confidence": 0.92,
    "categories": [
      {"name": "合同", "confidence": 0.92},
      {"name": "协议", "confidence": 0.85},
      {"name": "法律文件", "confidence": 0.78}
    ],
    "keywords": ["甲方", "乙方", "违约金", "期限"]
  }
}
```

#### POST /api/intelligence/sensitive-detect
敏感信息检测

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "types": ["ID_CARD", "PHONE", "BANK_CARD", "EMAIL"]
}

// Response
{
  "success": true,
  "data": {
    "hasSensitiveInfo": true,
    "totalFound": 5,
    "results": [
      {
        "type": "ID_CARD",
        "count": 2,
        "examples": ["110101********1234", "310101********5678"],
        "positions": [{"page": 1, "line": 10}, {"page": 2, "line": 5}]
      },
      {
        "type": "PHONE",
        "count": 3,
        "examples": ["138****1234", "159****5678"]
      }
    ]
  }
}
```

#### POST /api/intelligence/extract-keywords
关键词提取

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "topN": 10
}

// Response
{
  "success": true,
  "data": {
    "keywords": [
      {"word": "人工智能", "score": 0.95},
      {"word": "机器学习", "score": 0.88},
      {"word": "深度学习", "score": 0.82}
    ]
  }
}
```

#### POST /api/intelligence/summarize
文档摘要

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "maxLength": 500,  // 最大字数
  "style": "concise"  // concise, detailed, bullet_points
}

// Response
{
  "success": true,
  "data": {
    "summary": "本文介绍了人工智能的发展历程...",
    "keyPoints": [
      "人工智能起源于 1956 年",
      "深度学习推动了近年来的突破",
      "未来发展方向包括通用人工智能"
    ],
    "wordCount": 320
  }
}
```

#### POST /api/intelligence/ask
文档问答

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "question": "合同的有效期是多久？"
}

// Response
{
  "success": true,
  "data": {
    "answer": "合同有效期为 2024 年 1 月 1 日至 2025 年 12 月 31 日，共 2 年。",
    "confidence": 0.95,
    "sourcePages": [3],
    "sourceText": "本合同有效期自 2024 年 1 月 1 日起至 2025 年 12 月 31 日止。"
  }
}
```

### 3.3 模板引擎 API

#### POST /api/template/word/render
Word 模板渲染

```json
// Request
{
  "templatePath": "/templates/contract_template.docx",
  "targetPath": "/output/contract_001.docx",
  "data": {
    "contractNo": "CT-2024-001",
    "partyA": "北京科技有限公司",
    "partyB": "上海创新企业",
    "amount": "100,000",
    "signDate": "2024-01-15",
    "projects": [
      {"name": "软件开发", "price": 60000},
      {"name": "系统维护", "price": 40000}
    ]
  }
}
```

#### POST /api/template/pdf/fill
PDF 表单填充

```json
// Request
{
  "templatePath": "/templates/form_template.pdf",
  "targetPath": "/output/filled_form.pdf",
  "fields": {
    "name": "张三",
    "idCard": "110101********1234",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "agreement": true
  }
}
```

#### POST /api/template/batch-render
批量模板渲染

```json
// Request
{
  "templatePath": "/templates/certificate.docx",
  "outputDir": "/output/certificates/",
  "dataList": [
    {"name": "张三", "course": "Java 基础", "date": "2024-01-15"},
    {"name": "李四", "course": "Java 基础", "date": "2024-01-15"},
    {"name": "王五", "course": "Java 基础", "date": "2024-01-15"}
  ],
  "async": true
}
```

### 3.4 AI 助手 API

#### POST /api/ai/analyze
AI 文档分析

```json
// Request
{
  "filePath": "/path/to/report.pdf",
  "analysisType": "comprehensive",  // summary, sentiment, entities, all
  "language": "zh"
}

// Response
{
  "success": true,
  "data": {
    "summary": "这是一份财务报告...",
    "sentiment": {
      "overall": "positive",
      "score": 0.75
    },
    "entities": [
      {"type": "ORG", "name": "北京科技有限公司"},
      {"type": "DATE", "name": "2024年1月"}
    ],
    "recommendations": [
      "建议关注成本控制",
      "收入增长趋势良好"
    ]
  }
}
```

#### POST /api/ai/compare
文档对比

```json
// Request
{
  "sourcePath": "/path/to/contract_v1.pdf",
  "targetPath": "/path/to/contract_v2.pdf",
  "compareMode": "detailed"  // summary, detailed
}

// Response
{
  "success": true,
  "data": {
    "similarity": 0.85,
    "addedSections": [
      {"page": 3, "text": "新增条款内容..."}
    ],
    "removedSections": [
      {"page": 2, "text": "删除的条款..."}
    ],
    "modifiedSections": [
      {
        "page": 4,
        "oldText": "金额：10万元",
        "newText": "金额：15万元"
      }
    ]
  }
}
```

---

## 四、核心类设计

### 4.1 音视频处理

```java
@Service
public class MediaService {

    @Autowired
    private FFmpegExecutor ffmpegExecutor;

    /**
     * 获取视频信息
     */
    public VideoInfo getVideoInfo(String filePath);

    /**
     * 提取视频封面
     */
    public boolean extractThumbnail(String sourcePath, String targetPath,
                                    ThumbnailRequest request);

    /**
     * 视频转码
     */
    public TaskRecord transcodeVideo(String sourcePath, String targetPath,
                                     TranscodeConfig config);

    /**
     * 音频转文字
     */
    public TaskRecord transcribeAudio(String audioPath, TranscribeConfig config);
}

@Component
public class FFmpegExecutor {

    /**
     * 执行 FFmpeg 命令
     */
    public boolean execute(String command);

    /**
     * 获取多媒体信息
     */
    public MultimediaInfo getInfo(String filePath);

    /**
     * 转码
     */
    public boolean transcode(String sourcePath, String targetPath,
                             EncodingAttributes attrs);
}
```

### 4.2 文档智能

```java
@Service
public class DocumentIntelligenceService {

    @Autowired
    private DocumentClassifier classifier;

    @Autowired
    private SensitiveInfoDetector sensitiveDetector;

    @Autowired
    private Summarizer summarizer;

    @Autowired
    private LlmClient llmClient;

    /**
     * 文档分类
     */
    public ClassificationResult classify(String filePath);

    /**
     * 敏感信息检测
     */
    public SensitiveDetectionResult detectSensitiveInfo(
        String filePath, List<SensitiveType> types);

    /**
     * 关键词提取
     */
    public List<Keyword> extractKeywords(String filePath, int topN);

    /**
     * 生成摘要
     */
    public SummaryResult summarize(String filePath, SummarizeConfig config);

    /**
     * 文档问答
     */
    public AnswerResult ask(String filePath, String question);
}

@Component
public class SensitiveInfoDetector {

    private Map<SensitiveType, Pattern> patterns;

    /**
     * 检测敏感信息
     */
    public List<SensitiveInfo> detect(String text, List<SensitiveType> types);

    /**
     * 脱敏处理
     */
    public String mask(String text, SensitiveType type);
}

public enum SensitiveType {
    ID_CARD,      // 身份证号
    PHONE,        // 手机号
    BANK_CARD,    // 银行卡号
    EMAIL,        // 邮箱
    ADDRESS,      // 地址
    NAME          // 人名
}
```

### 4.3 模板引擎

```java
@Service
public class TemplateService {

    @Autowired
    private WordTemplateEngine wordEngine;

    @Autowired
    private PdfTemplateEngine pdfEngine;

    /**
     * Word 模板渲染
     */
    public boolean renderWordTemplate(String templatePath, String targetPath,
                                      Map<String, Object> data);

    /**
     * PDF 表单填充
     */
    public boolean fillPdfForm(String templatePath, String targetPath,
                               Map<String, String> fields);

    /**
     * 批量渲染
     */
    public TaskRecord batchRender(String templatePath, String outputDir,
                                  List<Map<String, Object>> dataList);
}

@Component
public class WordTemplateEngine {

    /**
     * 渲染模板
     */
    public boolean render(String templatePath, String targetPath,
                          TemplateData data);

    /**
     * 处理动态表格
     */
    private void processTable(XWPFTable table, List<Map<String, Object>> rows);

    /**
     * 替换占位符
     */
    private void replacePlaceholders(XWPFDocument doc, Map<String, Object> data);
}

@Component
public class PdfTemplateEngine {

    /**
     * 填充表单字段
     */
    public boolean fillFields(String templatePath, String targetPath,
                              Map<String, String> fields);

    /**
     * 获取表单字段列表
     */
    public List<String> getFieldNames(String templatePath);
}
```

### 4.4 AI 客户端

```java
@Component
public class LlmClient {

    @Value("${ai.provider:claude}")
    private String provider;

    private ClaudeClient claudeClient;
    private OpenAiClient openAiClient;

    /**
     * 发送对话请求
     */
    public LlmResponse chat(List<Message> messages, ChatConfig config);

    /**
     * 分析文档
     */
    public AnalysisResult analyzeDocument(String documentText,
                                          AnalysisType type);

    /**
     * 生成摘要
     */
    public String summarize(String text, int maxLength, String style);

    /**
     * 问答
     */
    public AnswerResult ask(String context, String question);
}

@Component
public class PromptBuilder {

    /**
     * 构建摘要提示词
     */
    public String buildSummarizePrompt(String text, int maxLength, String style);

    /**
     * 构建问答提示词
     */
    public String buildQaPrompt(String context, String question);

    /**
     * 构建文档分析提示词
     */
    public String buildAnalysisPrompt(String text, AnalysisType type);
}
```

---

## 五、实施步骤

### Week 1-4: 音视频基础

- [ ] 集成 FFmpeg/JAVE2
- [ ] 实现视频信息提取
- [ ] 实现音频信息提取
- [ ] 实现视频封面提取
- [ ] 创建 MediaController
- [ ] 集成测试

### Week 5-8: 音视频进阶

- [ ] 实现视频转码
- [ ] 实现音频转码
- [ ] 集成 Whisper/OpenAI API
- [ ] 实现音频转文字
- [ ] 性能优化
- [ ] 完整测试

### Week 9-12: 文档智能

- [ ] 设计文档分类模型/规则
- [ ] 实现文档分类
- [ ] 实现敏感信息检测
- [ ] 集成 HanLP/jieba
- [ ] 实现关键词提取
- [ ] 实现实体识别

### Week 13-16: AI 集成

- [ ] 集成 Claude/OpenAI API
- [ ] 实现文档摘要
- [ ] 实现文档问答
- [ ] 实现智能标签
- [ ] 实现文档对比
- [ ] 提示词优化

### Week 17-20: 模板引擎

- [ ] 设计 Word 模板语法
- [ ] 实现 Word 模板引擎
- [ ] 实现 PDF 表单填充
- [ ] 实现批量生成
- [ ] 创建 TemplateController
- [ ] 完整测试

### Week 21-24: 优化与集成

- [ ] 性能优化
- [ ] 缓存策略
- [ ] 错误处理完善
- [ ] 文档编写
- [ ] 压力测试

---

## 六、配置说明

### 6.1 FFmpeg 配置

```yaml
media:
  ffmpeg:
    path: /usr/local/bin/ffmpeg  # 系统 FFmpeg 路径
    timeout: 300  # 超时时间(秒)
    thread-count: 4
  whisper:
    mode: api  # api 或 local
    api-key: ${OPENAI_API_KEY}
    model: whisper-1
    language: zh
```

### 6.2 AI 配置

```yaml
ai:
  provider: claude  # claude 或 openai
  claude:
    api-key: ${CLAUDE_API_KEY}
    model: claude-3-sonnet-20240229
    max-tokens: 4096
    temperature: 0.5
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4-turbo-preview
    max-tokens: 4096
    temperature: 0.5
```

### 6.3 NLP 配置

```yaml
nlp:
  segmenter: hanlp  # hanlp 或 jieba
  keyword:
    algorithm: tfidf  # tfidf 或 textrank
    default-topn: 10
  sensitive:
    patterns:
      id-card: "\\d{17}[\\dXx]|\\d{15}"
      phone: "1[3-9]\\d{9}"
      email: "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
```

---

## 七、风险与对策

| 风险 | 影响 | 对策 |
|-----|------|------|
| FFmpeg 依赖系统 | 部署复杂 | 提供 Docker 镜像，预装 FFmpeg |
| LLM API 成本高 | 运营成本高 | 缓存结果，限制调用频率 |
| LLM API 延迟 | 响应慢 | 异步处理，流式输出 |
| 敏感信息检测误报 | 用户体验差 | 可调节敏感度，人工确认 |
| 大文档处理 | 超出 LLM 上下文 | 文档分块，摘要级联 |

---

## 八、验收标准

- [ ] 音视频：支持 MP4、AVI、MOV、MKV、MP3、WAV、AAC
- [ ] 音视频信息提取：响应时间 < 2s
- [ ] 语音识别：中文准确率 > 95%，处理速度 1x 实时
- [ ] 文档分类：准确率 > 85%
- [ ] 敏感信息检测：召回率 > 90%，准确率 > 85%
- [ ] 文档摘要：ROUGE 分数 > 0.4
- [ ] 模板渲染：1000 份文档 < 5 分钟
- [ ] LLM 响应：平均响应时间 < 5s
