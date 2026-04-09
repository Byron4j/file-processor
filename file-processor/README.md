# File Processor 文件处理服务

基于 Java 21 + Spring Boot 3 + MyBatis Plus 的文件处理转换项目。

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.0
- **MyBatis Plus**: 3.5.5
- **Apache POI**: 5.2.5 (Office文档处理)
- **Apache PDFBox**: 3.0.1 (PDF处理)
- **Jsoup**: 1.17.1 (HTML解析)
- **Hutool**: 5.8.23 (工具库)

## 已实现功能

### 1. DOC 转 DOCX
将旧版 Word 文档 (.doc) 转换为新版 Word 文档 (.docx) 格式。

**API**: `POST /api/file/convert/doc-to-docx`

```json
{
  "sourcePath": "/path/to/input.doc",
  "targetPath": "/path/to/output.docx"
}
```

### 2. ZIP 提取 index.html
解压 ZIP 文件，查找其中的 index.html 文件，提取其文本内容并保存。

**API**: `POST /api/file/extract/zip-index-html`

```json
{
  "sourcePath": "/path/to/archive.zip",
  "targetPath": "/path/to/output.txt"
}
```

提取的文本会：
- 移除 script、style 标签
- 保留标题、段落、列表的结构
- 将 HTML 转换为纯文本格式

### 3. 文档文本提取
从 DOC、DOCX、PDF、PPT、PPTX 文件中提取纯文本内容，保存为 txt 文件。

**API**: `POST /api/file/extract/text`

```json
{
  "sourcePath": "/path/to/document.pdf",
  "targetPath": "/path/to/output.txt"
}
```

支持格式：
- Word: .doc, .docx
- PDF: .pdf
- PowerPoint: .ppt, .pptx

提取内容：
- 保留段落结构
- PPT/PPTX 会标注页码（"=== Slide N ==="）
- Word 文档保留表格内容

## 快速开始

### 1. 编译运行

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn package

# 运行
java -jar target/file-processor-1.0.0.jar
```

### 2. 测试 API

```bash
# 健康检查
curl http://localhost:8080/api/file/health

# DOC 转 DOCX
curl -X POST http://localhost:8080/api/file/convert/doc-to-docx \
  -H "Content-Type: application/json" \
  -d '{"sourcePath":"./test.doc","targetPath":"./test.docx"}'

# ZIP 提取 index.html
curl -X POST http://localhost:8080/api/file/extract/zip-index-html \
  -H "Content-Type: application/json" \
  -d '{"sourcePath":"./test.zip","targetPath":"./output.txt"}'

# 提取文档文本 (DOC/DOCX/PDF/PPT/PPTX)
curl -X POST http://localhost:8080/api/file/extract/text \
  -H "Content-Type: application/json" \
  -d '{"sourcePath":"./document.pdf","targetPath":"./output.txt"}'
```

## 项目结构

```
file-processor/
├── src/
│   ├── main/java/com/fileprocessor/
│   │   ├── controller/      # REST API 控制器
│   │   ├── service/         # 业务逻辑层
│   │   ├── util/            # 工具类
│   │   │   ├── DocConverter.java    # DOC转DOCX
│   │   │   ├── ZipProcessor.java    # ZIP处理
│   │   │   └── TextExtractor.java   # 文本提取(DOC/DOCX/PDF/PPT/PPTX)
│   │   ├── dto/             # 数据传输对象
│   │   └── exception/       # 异常处理
│   ├── main/resources/
│   │   └── application.yml  # 配置文件
│   └── test/                # 测试代码
├── pom.xml                  # Maven配置
└── README.md
```

## 配置

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

## 后续扩展

可扩展支持更多文件格式：
- PDF 转 Word/图片
- 图片格式转换
- MP4/MP3 处理
- 文本文件编码转换
