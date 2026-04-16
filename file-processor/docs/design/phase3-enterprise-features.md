# 第三阶段设计方案：企业级特性

## 概述

**目标**：支持批量处理、异步任务、元数据管理
**时间周期**：3-4 个月
**核心功能**：异步任务框架、批量处理、多存储后端、文件元数据管理

---

## 一、功能清单

### 1.1 异步任务框架

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 任务提交 | 提交异步处理任务 | P0 |
| 任务状态查询 | 查询任务进度和状态 | P0 |
| 任务取消 | 取消进行中的任务 | P1 |
| 任务回调 | Webhook 回调通知 | P1 |
| 任务重试 | 失败任务自动重试 | P1 |
| 任务队列 | 优先级队列支持 | P2 |

### 1.2 批量处理

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 批量转换 | 多文件批量格式转换 | P0 |
| 批量水印 | 批量添加水印 | P0 |
| 批量提取 | 批量文本提取 | P0 |
| 批量压缩 | 多文件打包 | P1 |
| 批量哈希 | 批量计算文件哈希 | P1 |

### 1.3 多存储后端

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 本地存储 | 本地文件系统存储 | P0 |
| MinIO | 对象存储支持 | P0 |
| 阿里云 OSS | 阿里云对象存储 | P1 |
| AWS S3 | AWS S3 存储 | P2 |
| 存储切换 | 运行时切换存储后端 | P1 |

### 1.4 文件元数据管理

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| 文件注册 | 文件信息入库 | P0 |
| 文件查询 | 按条件查询文件 | P0 |
| 文件分类 | 文件标签/分类管理 | P1 |
| 文件统计 | 文件数量/大小统计 | P1 |
| 文件清理 | 过期文件自动清理 | P2 |

### 1.5 文件预览

| 功能 | 描述 | 优先级 |
|-----|------|-------|
| Office 转 PDF | Office 文档转 PDF 预览 | P2 |
| 图片预览 | 图片在线预览 | P1 |
| PDF 预览 | PDF 在线预览 | P1 |
| 文本预览 | 文本文件预览 | P1 |

---

## 二、技术架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Layer                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │ Sync API     │ │ Async API    │ │ Webhook API  │            │
│  │ 同步接口      │ │ 任务接口      │ │ 回调接口      │            │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘            │
└─────────┼────────────────┼────────────────┼────────────────────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │ Sync        │  │ Async Task  │  │ Storage     │
   │ Service     │  │ Service     │  │ Service     │
   │             │  │             │  │             │
   │ - 快速处理   │  │ - 任务队列   │  │ - 本地       │
   │ - 小文件     │  │ - 进度跟踪   │  │ - MinIO     │
   │ - 即时响应   │  │ - 回调通知   │  │ - OSS       │
   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
          │                │                │
          └────────────────┼────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │ MySQL/H2    │  │ Redis       │  │ File        │
   │             │  │             │  │ Processors  │
   │ - 元数据     │  │ - 任务队列   │  │             │
   │ - 任务记录   │  │ - 进度缓存   │  │ - Office    │
   │ - 统计信息   │  │ - 分布式锁   │  │ - PDF       │
   └─────────────┘  └─────────────┘  │ - Image     │
                                     └─────────────┘
```

### 2.2 模块结构

```
com.fileprocessor
├── controller
│   ├── TaskController.java          # 任务管理 API
│   ├── BatchController.java         # 批量处理 API
│   ├── MetadataController.java      # 文件元数据 API
│   └── PreviewController.java       # 文件预览 API
├── service
│   ├── TaskService.java             # 任务管理服务
│   ├── BatchService.java            # 批量处理服务
│   ├── MetadataService.java         # 元数据服务
│   ├── PreviewService.java          # 预览服务
│   └── storage
│       ├── StorageService.java      # 存储接口
│       ├── LocalStorageService.java # 本地存储
│       ├── MinioStorageService.java # MinIO 存储
│       └── AliyunOssService.java    # 阿里云 OSS
├── task
│   ├── TaskExecutor.java            # 任务执行器
│   ├── TaskQueue.java               # 任务队列
│   ├── TaskWorker.java              # 任务工作线程
│   └── TaskCallback.java            # 任务回调
├── entity
│   ├── FileMetadata.java            # 文件元数据实体
│   ├── TaskRecord.java              # 任务记录实体
│   └── FileCategory.java            # 文件分类实体
├── mapper
│   ├── FileMetadataMapper.java
│   └── TaskRecordMapper.java
└── config
    ├── AsyncConfig.java             # 异步配置
    ├── RedisConfig.java             # Redis 配置
    └── StorageConfig.java           # 存储配置
```

### 2.3 依赖项

```xml
<!-- Spring Async -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- MinIO -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>

<!-- 阿里云 OSS -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>

<!-- Spring Boot Starter Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Jackson for JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- MyBatis Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- PageHelper for pagination -->
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## 三、数据库设计

### 3.1 文件元数据表 (file_metadata)

```sql
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) NOT NULL COMMENT '文件唯一标识 (UUID)',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_type VARCHAR(20) NOT NULL COMMENT '存储类型: LOCAL/MINIO/OSS/S3',
    storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    extension VARCHAR(20) COMMENT '文件扩展名',
    md5_hash VARCHAR(32) COMMENT 'MD5哈希值',
    sha256_hash VARCHAR(64) COMMENT 'SHA256哈希值',
    category_id BIGINT COMMENT '分类ID',
    tags JSON COMMENT '标签列表',
    description TEXT COMMENT '文件描述',
    metadata JSON COMMENT '扩展元数据 (宽高、页数等)',
    source_type VARCHAR(50) COMMENT '来源: UPLOAD/CONVERT/EXTRACT',
    reference_count INT DEFAULT 1 COMMENT '引用计数',
    expire_at TIMESTAMP NULL COMMENT '过期时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-删除, 1-正常, 2-过期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(64) COMMENT '创建者',
    
    UNIQUE KEY uk_file_id (file_id),
    INDEX idx_md5 (md5_hash),
    INDEX idx_storage (storage_type, storage_path),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';
```

### 3.2 任务记录表 (task_record)

```sql
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL COMMENT '任务唯一标识',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型: CONVERT/EXTRACT/MERGE/WATERMARK/OCR',
    task_name VARCHAR(255) COMMENT '任务名称',
    source_files JSON NOT NULL COMMENT '源文件列表 [{fileId, path}]',
    target_files JSON COMMENT '目标文件列表 [{fileId, path}]',
    parameters JSON COMMENT '任务参数',
    status VARCHAR(20) NOT NULL COMMENT '状态: PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度 0-100',
    current_step VARCHAR(100) COMMENT '当前步骤描述',
    total_items INT DEFAULT 1 COMMENT '总处理项数',
    processed_items INT DEFAULT 0 COMMENT '已处理项数',
    result JSON COMMENT '处理结果',
    error_message TEXT COMMENT '错误信息',
    error_stack TEXT COMMENT '错误堆栈',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry INT DEFAULT 3 COMMENT '最大重试次数',
    callback_url VARCHAR(500) COMMENT '回调URL',
    callback_status VARCHAR(20) COMMENT '回调状态: PENDING/SUCCESS/FAILED',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) COMMENT '创建者',
    
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_type (task_type),
    INDEX idx_created (created_at),
    INDEX idx_callback (callback_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务记录表';
```

### 3.3 文件分类表 (file_category)

```sql
CREATE TABLE file_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NOT NULL COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    description VARCHAR(255) COMMENT '描述',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_code (code),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分类表';

-- 初始化数据
INSERT INTO file_category (name, code, description) VALUES
('文档', 'DOCUMENT', 'Word、PDF、TXT 等文档'),
('表格', 'SPREADSHEET', 'Excel、CSV 等表格'),
('演示文稿', 'PRESENTATION', 'PPT、Keynote 等'),
('图片', 'IMAGE', 'JPG、PNG、GIF 等'),
('压缩包', 'ARCHIVE', 'ZIP、RAR、7z 等'),
('音视频', 'MEDIA', 'MP4、MP3 等'),
('其他', 'OTHER', '其他类型文件');
```

---

## 四、API 设计

### 4.1 任务管理 API

#### POST /api/tasks/submit
提交异步任务

```json
// Request
{
  "taskType": "CONVERT",
  "taskName": "批量转换任务",
  "sourceFiles": [
    {"fileId": "f-xxx-1", "path": "/uploads/doc1.doc"},
    {"fileId": "f-xxx-2", "path": "/uploads/doc2.doc"}
  ],
  "parameters": {
    "targetFormat": "PDF",
    "quality": "HIGH"
  },
  "callbackUrl": "https://example.com/callback",
  "priority": "NORMAL"
}

// Response
{
  "success": true,
  "data": {
    "taskId": "t-xxx-xxx",
    "status": "PENDING",
    "estimatedTime": 30000,
    "queryUrl": "/api/tasks/t-xxx-xxx/status"
  }
}
```

#### GET /api/tasks/{taskId}/status
查询任务状态

```json
// Response
{
  "success": true,
  "data": {
    "taskId": "t-xxx-xxx",
    "taskType": "CONVERT",
    "status": "PROCESSING",
    "progress": 45,
    "currentStep": "正在转换第 2/5 个文件",
    "processedItems": 2,
    "totalItems": 5,
    "startedAt": "2024-01-15T10:30:00",
    "estimatedCompleteAt": "2024-01-15T10:32:30"
  }
}
```

#### POST /api/tasks/{taskId}/cancel
取消任务

```json
// Response
{
  "success": true,
  "message": "Task cancelled successfully"
}
```

#### GET /api/tasks
任务列表查询

```
GET /api/tasks?status=PROCESSING&taskType=CONVERT&page=1&size=20
```

```json
// Response
{
  "success": true,
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "items": [
      {
        "taskId": "t-xxx-xxx",
        "taskName": "批量转换任务",
        "status": "PROCESSING",
        "progress": 45,
        "createdAt": "2024-01-15T10:30:00"
      }
    ]
  }
}
```

### 4.2 批量处理 API

#### POST /api/batch/convert
批量格式转换

```json
// Request
{
  "files": [
    "/path/to/doc1.doc",
    "/path/to/doc2.doc",
    "/path/to/doc3.doc"
  ],
  "targetFormat": "PDF",
  "outputDir": "/path/to/output/",
  "async": true,
  "callbackUrl": "https://example.com/callback"
}

// Response (async)
{
  "success": true,
  "data": {
    "taskId": "t-xxx-xxx",
    "status": "PENDING",
    "fileCount": 3
  }
}
```

#### POST /api/batch/watermark
批量添加水印

```json
// Request
{
  "files": [
    "/path/to/doc1.pdf",
    "/path/to/doc2.pdf"
  ],
  "watermark": {
    "text": "CONFIDENTIAL",
    "opacity": 0.3,
    "rotation": 45
  },
  "async": true
}
```

#### POST /api/batch/extract
批量文本提取

```json
// Request
{
  "files": [
    "/path/to/doc1.pdf",
    "/path/to/doc2.docx"
  ],
  "outputFormat": "JSON",
  "combineOutput": true,
  "targetPath": "/path/to/output/extracted.json"
}
```

### 4.3 文件元数据 API

#### POST /api/files/register
注册文件（手动添加元数据）

```json
// Request
{
  "originalName": "report.pdf",
  "storageType": "LOCAL",
  "storagePath": "/uploads/report.pdf",
  "fileSize": 1024000,
  "categoryId": 1,
  "tags": ["财务", "2024", "年报"],
  "description": "2024年度财务报告"
}

// Response
{
  "success": true,
  "data": {
    "fileId": "f-xxx-xxx",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### GET /api/files/{fileId}
获取文件信息

```json
// Response
{
  "success": true,
  "data": {
    "fileId": "f-xxx-xxx",
    "originalName": "report.pdf",
    "storageType": "LOCAL",
    "fileSize": 1024000,
    "mimeType": "application/pdf",
    "extension": "pdf",
    "md5Hash": "d41d8cd98f00b204e9800998ecf8427e",
    "category": {
      "id": 1,
      "name": "文档"
    },
    "tags": ["财务", "2024"],
    "metadata": {
      "pages": 10,
      "width": 595,
      "height": 842
    },
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### GET /api/files
文件列表查询

```
GET /api/files?categoryId=1&tag=财务&extension=pdf&page=1&size=20
```

```json
// Response
{
  "success": true,
  "data": {
    "total": 50,
    "page": 1,
    "size": 20,
    "items": [...]
  }
}
```

#### POST /api/files/{fileId}/tags
更新文件标签

```json
// Request
{
  "tags": ["财务", "2024", "已审核"]
}
```

### 4.4 文件预览 API

#### GET /api/preview/{fileId}
获取预览信息

```
GET /api/preview/f-xxx-xxx?type=PDF

// Response
{
  "success": true,
  "data": {
    "previewType": "PDF",
    "previewUrl": "/api/preview/f-xxx-xxx/content",
    "pages": 10,
    "fileSize": 512000
  }
}
```

#### GET /api/preview/{fileId}/content
预览文件内容（返回转换后的 PDF 或图片）

```
GET /api/preview/f-xxx-xxx/content?page=1

// 返回 PDF 流或图片流
```

---

## 五、核心类设计

### 5.1 任务执行框架

```java
public interface TaskHandler<T extends TaskParameters> {
    
    /**
     * 任务类型
     */
    TaskType getTaskType();
    
    /**
     * 执行任务
     */
    TaskResult execute(T parameters, TaskProgressListener listener);
    
    /**
     * 取消任务
     */
    void cancel(String taskId);
}

public class TaskExecutor {
    
    @Autowired
    private TaskQueue taskQueue;
    
    @Autowired
    private TaskRecordMapper taskRecordMapper;
    
    /**
     * 提交任务
     */
    public TaskRecord submit(TaskSubmitRequest request);
    
    /**
     * 执行任务（内部调用）
     */
    @Async("taskExecutor")
    public void doExecute(String taskId);
    
    /**
     * 取消任务
     */
    public boolean cancel(String taskId);
    
    /**
     * 查询任务状态
     */
    public TaskStatus getStatus(String taskId);
}

public interface TaskProgressListener {
    void onProgress(int progress, String message);
    void onComplete(TaskResult result);
    void onError(Exception e);
}
```

### 5.2 存储服务

```java
public interface StorageService {
    
    /**
     * 存储类型
     */
    StorageType getType();
    
    /**
     * 保存文件
     */
    String save(InputStream inputStream, String path, String filename);
    
    /**
     * 读取文件
     */
    InputStream read(String path);
    
    /**
     * 删除文件
     */
    boolean delete(String path);
    
    /**
     * 检查文件是否存在
     */
    boolean exists(String path);
    
    /**
     * 获取文件URL
     */
    String getUrl(String path);
    
    /**
     * 获取文件大小
     */
    long getSize(String path);
}

@Component
public class LocalStorageService implements StorageService {
    // 本地存储实现
}

@Component
public class MinioStorageService implements StorageService {
    // MinIO 实现
}

@Component
public class AliyunOssService implements StorageService {
    // 阿里云 OSS 实现
}

@Service
public class StorageManager {
    
    @Autowired
    private List<StorageService> storageServices;
    
    /**
     * 获取存储服务
     */
    public StorageService getService(StorageType type);
    
    /**
     * 自动选择存储（根据配置）
     */
    public StorageService getDefaultService();
}
```

### 5.3 批量处理

```java
@Service
public class BatchService {
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    /**
     * 批量转换
     */
    public TaskRecord batchConvert(BatchConvertRequest request);
    
    /**
     * 批量水印
     */
    public TaskRecord batchWatermark(BatchWatermarkRequest request);
    
    /**
     * 批量提取
     */
    public TaskRecord batchExtract(BatchExtractRequest request);
    
    /**
     * 批量哈希
     */
    public BatchHashResult batchHash(List<String> filePaths);
}
```

---

## 六、配置说明

### 6.1 异步任务配置

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 200
        keep-alive: 60s
      thread-name-prefix: task-

async:
  task:
    timeout: 300000  # 5分钟超时
    max-retry: 3
    callback:
      timeout: 10000
      retry-interval: 5000
```

### 6.2 Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

task:
  queue:
    prefix: "task:queue"
    processing-prefix: "task:processing"
    result-prefix: "task:result"
  lock:
    prefix: "task:lock"
    expire: 300
```

### 6.3 存储配置

```yaml
storage:
  default: local
  local:
    base-path: ./uploads
    url-prefix: /uploads
  minio:
    enabled: false
    endpoint: http://localhost:9000
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    bucket: file-processor
  aliyun-oss:
    enabled: false
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    bucket: file-processor
```

---

## 七、实施步骤

### Week 1-2: 数据库与实体

- [ ] 设计数据库表结构
- [ ] 创建 MyBatis Mapper
- [ ] 实现实体类
- [ ] 配置数据库连接
- [ ] 单元测试

### Week 3-4: 异步任务框架

- [ ] 设计任务执行框架
- [ ] 实现任务队列（Redis）
- [ ] 实现任务执行器
- [ ] 任务状态管理
- [ ] 任务回调机制

### Week 5-6: 任务管理 API

- [ ] 创建 TaskController
- [ ] 实现任务提交
- [ ] 实现任务查询
- [ ] 实现任务取消
- [ ] 集成测试

### Week 7-8: 多存储后端

- [ ] 设计 Storage 接口
- [ ] 实现本地存储
- [ ] 实现 MinIO 存储
- [ ] 实现阿里云 OSS
- [ ] 存储管理器

### Week 9-10: 批量处理

- [ ] 批量转换实现
- [ ] 批量水印实现
- [ ] 批量提取实现
- [ ] 批量任务管理
- [ ] 性能优化

### Week 11-12: 文件元数据

- [ ] 文件注册功能
- [ ] 文件查询功能
- [ ] 分类管理
- [ ] 标签管理
- [ ] 统计功能

### Week 13-14: 文件预览

- [ ] Office 转 PDF
- [ ] 图片预览
- [ ] PDF 预览
- [ ] 预览缓存
- [ ] 性能优化

---

## 八、风险与对策

| 风险 | 影响 | 对策 |
|-----|------|------|
| Redis 故障 | 任务队列不可用 | 降级为内存队列，定期持久化 |
| 存储后端切换 | 文件访问失败 | 平滑迁移，支持多存储共存 |
| 任务堆积 | 内存溢出 | 限制队列长度，拒绝新任务 |
| 回调失败 | 状态不同步 | 重试机制 + 补偿任务 |

---

## 九、验收标准

- [ ] 异步任务：支持 1000 并发任务提交
- [ ] 任务队列：支持 10 万任务堆积
- [ ] 批量处理：1000 文件批量处理 < 10 分钟
- [ ] 存储切换：切换耗时 < 1s
- [ ] 元数据查询：响应时间 < 100ms
- [ ] 文件预览：Office 转 PDF < 5s
- [ ] 系统可用性：> 99.9%
