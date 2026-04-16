# 第一阶段设计方案：核心能力完善

## 概述

**目标**：完善现有文档处理能力，建立稳定的处理框架
**时间周期**：1-2 个月
**核心功能**：Excel 处理、图片处理、文件校验、压缩包扩展支持

---

## 一、功能清单

### 1.1 Excel 处理模块

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| XLS/XLSX 文本提取 | 从 Excel 文件提取所有单元格文本内容 | P0 |
| Excel 转 CSV | 将 Excel 转换为 CSV 格式 | P0 |
| Excel 转 JSON | 将 Excel 转换为 JSON 格式（支持多级对象） | P0 |
| Excel 信息获取 | 获取工作表列表、行列数、单元格统计 | P1 |

### 1.2 图片处理模块

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 图片格式转换 | JPEG/PNG/GIF/BMP/WebP/TIFF 互转 | P0 |
| 缩略图生成 | 按比例或固定尺寸生成缩略图 | P0 |
| 图片信息获取 | 尺寸、格式、颜色模式、DPI 等元数据 | P1 |
| 图片压缩 | 质量压缩、尺寸压缩 | P1 |

### 1.3 文件校验模块

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| MD5 计算 | 计算文件 MD5 哈希值 | P0 |
| SHA-256 计算 | 计算文件 SHA-256 哈希值 | P0 |
| 批量校验 | 同时计算多种哈希算法 | P1 |

### 1.4 压缩包扩展支持

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 7z 解压/压缩 | 7z 格式支持 | P1 |
| rar 解压 | rar 格式支持（只读） | P1 |
| 压缩包内容预览 | 不解压查看文件列表 | P1 |

---

## 二、技术架构

### 2.1 模块结构

```
com.fileprocessor
├── controller
│   ├── ExcelController.java      # Excel 处理 API
│   ├── ImageController.java      # 图片处理 API
│   ├── FileVerifyController.java # 文件校验 API
│   └── ArchiveController.java    # 压缩包扩展 API
├── service
│   ├── ExcelService.java
│   ├── ImageService.java
│   ├── FileVerifyService.java
│   └── ArchiveService.java
├── util
│   ├── ExcelProcessor.java       # Excel 处理工具
│   ├── ImageProcessor.java       # 图片处理工具
│   ├── FileHashCalculator.java   # 哈希计算工具
│   └── ArchiveUtil.java          # 压缩包工具（增强）
├── dto
│   ├── ExcelConvertRequest.java
│   ├── ImageProcessRequest.java
│   └── FileHashResponse.java
└── model
    └── ImageFormat.java          # 图片格式枚举
```

### 2.2 依赖项

```xml
<!-- Thumbnailator - 图片处理 -->
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>

<!-- WebP 支持 -->
<dependency>
    <groupId>org.sejda.imageio</groupId>
    <artifactId>webp-imageio</artifactId>
    <version>0.1.6</version>
</dependency>

<!-- 7z 支持 -->
<dependency>
    <groupId>net.sf.sevenzipjbinding</groupId>
    <artifactId>sevenzipjbinding</artifactId>
    <version>16.02-2.01</version>
</dependency>
<dependency>
    <groupId>net.sf.sevenzipjbinding</groupId>
    <artifactId>sevenzipjbinding-all-platforms</artifactId>
    <version>16.02-2.01</version>
</dependency>

<!-- RAR 支持 -->
<dependency>
    <groupId>com.github.junrar</groupId>
    <artifactId>junrar</artifactId>
    <version>7.5.5</version>
</dependency>

<!-- Apache Commons CSV -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>
```

---

## 三、API 设计

### 3.1 Excel API

#### POST /api/excel/extract/text
提取 Excel 文本内容

```json
// Request
{
  "sourcePath": "/path/to/data.xlsx",
  "sheetIndex": 0,
  "includeHeader": true
}

// Response
{
  "success": true,
  "message": "Text extracted successfully",
  "data": {
    "sheetName": "Sheet1",
    "rowCount": 100,
    "content": [
      ["姓名", "年龄", "城市"],
      ["张三", "25", "北京"],
      ["李四", "30", "上海"]
    ]
  }
}
```

#### POST /api/excel/convert/csv
Excel 转 CSV

```json
// Request
{
  "sourcePath": "/path/to/data.xlsx",
  "targetPath": "/path/to/output.csv",
  "sheetIndex": 0,
  "delimiter": ","
}

// Response
{
  "success": true,
  "message": "Conversion successful",
  "filePath": "/path/to/output.csv",
  "fileSize": 1024
}
```

#### POST /api/excel/convert/json
Excel 转 JSON

```json
// Request
{
  "sourcePath": "/path/to/data.xlsx",
  "targetPath": "/path/to/output.json",
  "sheetIndex": 0,
  "headerRow": 0,
  "dataStartRow": 1,
  "mapping": {
    "A": "name",
    "B": "age",
    "C": "city"
  }
}

// Response
{
  "success": true,
  "message": "Conversion successful",
  "filePath": "/path/to/output.json",
  "recordCount": 99
}
```

### 3.2 图片处理 API

#### POST /api/image/convert
图片格式转换

```json
// Request
{
  "sourcePath": "/path/to/photo.jpg",
  "targetPath": "/path/to/photo.png",
  "targetFormat": "PNG",
  "quality": 0.9
}

// Response
{
  "success": true,
  "message": "Image converted successfully",
  "filePath": "/path/to/photo.png",
  "fileSize": 204800,
  "width": 1920,
  "height": 1080
}
```

#### POST /api/image/thumbnail
生成缩略图

```json
// Request
{
  "sourcePath": "/path/to/photo.jpg",
  "targetPath": "/path/to/thumb.jpg",
  "width": 200,
  "height": 200,
  "mode": "FIT",  // FIT, FILL, SCALE
  "keepAspectRatio": true
}

// Response
{
  "success": true,
  "message": "Thumbnail created",
  "filePath": "/path/to/thumb.jpg",
  "width": 200,
  "height": 113
}
```

#### GET /api/image/info
获取图片信息

```json
// Request
GET /api/image/info?path=/path/to/photo.jpg

// Response
{
  "success": true,
  "data": {
    "format": "JPEG",
    "width": 1920,
    "height": 1080,
    "colorType": "RGB",
    "dpi": 72,
    "fileSize": 1048576
  }
}
```

### 3.3 文件校验 API

#### POST /api/file/hash
计算文件哈希

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "algorithms": ["MD5", "SHA-256"]
}

// Response
{
  "success": true,
  "data": {
    "md5": "d41d8cd98f00b204e9800998ecf8427e",
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
}
```

#### POST /api/file/verify
校验文件哈希

```json
// Request
{
  "filePath": "/path/to/document.pdf",
  "algorithm": "MD5",
  "expectedHash": "d41d8cd98f00b204e9800998ecf8427e"
}

// Response
{
  "success": true,
  "data": {
    "matched": true,
    "actualHash": "d41d8cd98f00b204e9800998ecf8427e"
  }
}
```

### 3.4 压缩包扩展 API

#### POST /api/archive/extract/7z
解压 7z 文件

```json
// Request
{
  "sourcePath": "/path/to/archive.7z",
  "targetPath": "/path/to/output/",
  "password": "optional_password"
}
```

#### POST /api/archive/extract/rar
解压 rar 文件

```json
// Request
{
  "sourcePath": "/path/to/archive.rar",
  "targetPath": "/path/to/output/",
  "password": "optional_password"
}
```

#### POST /api/archive/list
列出压缩包内容

```json
// Request
{
  "archivePath": "/path/to/archive.7z"
}

// Response
{
  "success": true,
  "data": {
    "totalFiles": 100,
    "totalSize": 10485760,
    "entries": [
      {
        "name": "document.pdf",
        "size": 1024000,
        "isDirectory": false,
        "lastModified": "2024-01-15T10:30:00"
      }
    ]
  }
}
```

---

## 四、核心类设计

### 4.1 ExcelProcessor

```java
public class ExcelProcessor {

    /**
     * 提取 Excel 文本内容
     */
    public static ExcelContent extractText(String filePath, int sheetIndex);

    /**
     * 转换为 CSV
     */
    public static boolean convertToCsv(String sourcePath, String targetPath,
                                       int sheetIndex, char delimiter);

    /**
     * 转换为 JSON
     */
    public static boolean convertToJson(String sourcePath, String targetPath,
                                        ExcelToJsonConfig config);

    /**
     * 获取工作表信息
     */
    public static List<SheetInfo> getSheetInfo(String filePath);
}

public class ExcelContent {
    private String sheetName;
    private int rowCount;
    private int columnCount;
    private List<List<String>> content;
}
```

### 4.2 ImageProcessor

```java
public class ImageProcessor {

    /**
     * 图片格式转换
     */
    public static boolean convertFormat(String sourcePath, String targetPath,
                                        ImageFormat targetFormat, float quality);

    /**
     * 生成缩略图
     */
    public static boolean createThumbnail(String sourcePath, String targetPath,
                                          ThumbnailConfig config);

    /**
     * 获取图片信息
     */
    public static ImageInfo getImageInfo(String filePath);

    /**
     * 压缩图片
     */
    public static boolean compress(String sourcePath, String targetPath,
                                   CompressionConfig config);
}

public enum ImageFormat {
    JPEG, PNG, GIF, BMP, WEBP, TIFF
}

public class ThumbnailConfig {
    private int width;
    private int height;
    private ThumbnailMode mode;  // FIT, FILL, SCALE
    private boolean keepAspectRatio;
}
```

### 4.3 FileHashCalculator

```java
public class FileHashCalculator {

    /**
     * 计算单个哈希值
     */
    public static String calculateHash(String filePath, HashAlgorithm algorithm);

    /**
     * 批量计算哈希值
     */
    public static Map<HashAlgorithm, String> calculateHashes(
        String filePath, List<HashAlgorithm> algorithms);

    /**
     * 校验文件哈希
     */
    public static boolean verifyHash(String filePath,
                                     HashAlgorithm algorithm,
                                     String expectedHash);
}

public enum HashAlgorithm {
    MD5, SHA_1, SHA_256, SHA_512
}
```

---

## 五、实施步骤

### Week 1-2: Excel 处理模块

**Week 1**: 基础框架与文本提取
- [ ] 创建 ExcelProcessor 工具类
- [ ] 实现 XLS/XLSX 文本提取
- [ ] 添加工作表信息获取
- [ ] 创建 ExcelController
- [ ] 编写单元测试

**Week 2**: 格式转换
- [ ] 实现 Excel 转 CSV
- [ ] 实现 Excel 转 JSON（简单映射）
- [ ] 支持列映射配置
- [ ] 性能优化（大文件流式处理）
- [ ] 集成测试

### Week 3-4: 图片处理模块

**Week 3**: 格式转换与信息获取
- [ ] 添加图片处理依赖
- [ ] 创建 ImageProcessor
- [ ] 实现图片格式转换
- [ ] 实现图片信息获取
- [ ] 创建 ImageController

**Week 4**: 缩略图与压缩
- [ ] 实现缩略图生成
- [ ] 实现图片压缩
- [ ] 添加 WebP 支持
- [ ] 性能测试与优化

### Week 5-6: 文件校验模块

**Week 5**: 哈希计算
- [ ] 创建 FileHashCalculator
- [ ] 实现 MD5/SHA-256 计算
- [ ] 支持大文件分块计算
- [ ] 创建 FileVerifyController

**Week 6**: 校验与集成
- [ ] 实现哈希校验功能
- [ ] 批量哈希计算
- [ ] 性能优化
- [ ] 单元测试

### Week 7-8: 压缩包扩展

**Week 7**: 7z 支持
- [ ] 添加 sevenzipjbinding 依赖
- [ ] 实现 7z 解压
- [ ] 实现 7z 压缩
- [ ] 添加密码支持

**Week 8**: RAR 与内容预览
- [ ] 添加 junrar 依赖
- [ ] 实现 RAR 解压
- [ ] 实现压缩包内容预览
- [ ] 统一错误处理
- [ ] 完整测试

---

## 六、风险与对策

| 风险 | 影响 | 对策 |
|-----|------|------|
| sevenzipjbinding 平台兼容性 | 部署失败 | 使用 all-platforms 依赖，提供平台检测 |
| 大 Excel 文件内存溢出 | 服务崩溃 | 使用流式 API（SXSSFWorkbook） |
| 图片处理慢 | 响应时间长 | 使用 Thumbnailator 的异步处理，添加超时控制 |
| RAR 加密支持有限 | 部分文件无法解压 | 明确文档说明限制，支持常见加密方式 |

---

## 七、验收标准

- [ ] Excel：支持 10 万行数据提取，响应时间 < 5s
- [ ] Excel 转 CSV/JSON：支持 100MB 文件
- [ ] 图片：支持 20MB 图片处理，响应时间 < 3s
- [ ] 缩略图：支持批量生成，100 张 < 30s
- [ ] 哈希计算：1GB 文件计算时间 < 10s
- [ ] 7z/rar：支持 1GB 压缩包解压
- [ ] 所有功能单元测试覆盖率 > 80%
