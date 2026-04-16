# 文件处理服务发展规划 (Roadmap)

## 项目定位

基于 Java 21 + Spring Boot 3 的通用文件处理微服务，提供文档转换、内容提取、格式处理等能力。目标是成为企业级的文件处理基础设施。

---

## 发展阶段规划

### ✅ 第一阶段：核心能力完善（已完成）

**目标**：完善现有文档处理能力，建立稳定的处理框架
**状态**：已完成
**时间**：2024年1月

| 功能模块 | 具体功能 | 技术方案 | 优先级 |
|---------|---------|---------|-------|
| Excel 处理 | XLS/XLSX 文本提取 | Apache POI HSSF/XSSF | P0 |
| Excel 处理 | Excel 转 CSV/JSON | Apache POI | P0 |
| 图片处理 | 图片格式转换 | ImageIO / Thumbnailator | P0 |
| 图片处理 | 缩略图生成 | Thumbnailator | P0 |
| 文件校验 | MD5/SHA256 计算 | Java MessageDigest | P1 |
| 压缩包 | 7z/rar 支持 | SevenZipJBinding / junrar | P1 |

**关键产出**：
- 支持 Office 全家桶（Word、Excel、PPT、PDF）
- 支持主流图片格式（JPEG、PNG、GIF、WebP、BMP）
- 统一的处理结果返回格式

---

### 第二阶段：进阶功能（2-3 个月）

**目标**：增加文件编辑、合并、安全相关能力

| 功能模块 | 具体功能 | 技术方案 | 优先级 |
|---------|---------|---------|-------|
| 文件合并 | PDF 合并/拆分 | PDFBox / iText | P0 |
| 文件合并 | Office 文档合并 | Apache POI | P1 |
| 水印处理 | PDF 文字/图片水印 | PDFBox | P0 |
| 水印处理 | Office 文档水印 | Apache POI | P1 |
| 文件加密 | PDF 密码保护 | PDFBox | P1 |
| 文件解密 | 移除 PDF 密码 | PDFBox | P1 |
| OCR 识别 | 图片文字识别 | Tesseract / 百度 OCR API | P0 |
| 文件压缩 | 文件/文件夹压缩 ZIP | Java Zip API | P1 |

**关键产出**：
- 完整的 PDF 工具链（提取、合并、拆分、水印、加密）
- 图片文字识别能力
- 文件安全处理能力

---

### 第三阶段：企业级特性（3-4 个月）

**目标**：支持批量处理、异步任务、元数据管理

| 功能模块 | 具体功能 | 技术方案 | 优先级 |
|---------|---------|---------|-------|
| 异步任务 | 大文件异步处理 | Spring Async + 任务队列 | P0 |
| 任务管理 | 任务状态查询/取消 | Redis + 数据库 | P0 |
| 批量处理 | 多文件批量处理 | 线程池 + 批处理框架 | P0 |
| 文件存储 | 本地/MinIO/OSS 适配 | Spring Storage | P1 |
| 文件元数据 | 文件信息数据库管理 | MyBatis Plus + H2/MySQL | P1 |
| 文件预览 | Office 转 PDF 预览 | LibreOffice / OnlyOffice | P2 |
| 文件预览 | 图片预览服务 | ImageIO | P1 |

**关键产出**：
- 异步任务管理系统
- 文件元数据持久化
- 多存储后端支持
- 文件预览能力

---

### 第四阶段：高级特性（4-6 个月）

**目标**：AI 集成、音视频处理、文档智能

| 功能模块 | 具体功能 | 技术方案 | 优先级 |
|---------|---------|---------|-------|
| 音视频 | MP4/MP3 信息提取 | JAVE2 / FFmpeg | P1 |
| 音视频 | 视频缩略图/封面 | FFmpeg | P2 |
| 音视频 | 音频转文字 | Whisper / 讯飞 API | P2 |
| 文档智能 | 文档分类 | 机器学习 / 规则引擎 | P2 |
| 文档智能 | 敏感信息检测 | 正则 + NLP | P2 |
| 文档智能 | 自动摘要 | LLM API | P3 |
| 模板引擎 | Word 模板填充 | Apache POI + 模板引擎 | P1 |
| 模板引擎 | PDF 表单填充 | PDFBox | P1 |

**关键产出**：
- 音视频处理能力
- 文档智能分析
- 模板化文档生成

---

## 技术架构演进

### 当前架构
```
Controller → Service → Util (静态工具类)
```

### 目标架构
```
┌─────────────────────────────────────────────────────────────┐
│                         API Gateway                          │
│              (限流、认证、路由、统一响应)                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
   ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
   │ Sync API│    │Async API│    │ Webhook │
   │ 同步接口 │    │ 任务接口 │    │ 回调通知 │
   └────┬────┘    └────┬────┘    └─────────┘
        │               │
        └───────┬───────┘
                │
        ┌───────▼────────┐
        │  File Service  │
        │  文件处理服务   │
        └───────┬────────┘
                │
    ┌───────────┼───────────┐
    │           │           │
┌───▼───┐   ┌───▼───┐   ┌──▼────┐
│ Local │   │ MinIO │   │  OSS  │
│ 本地   │   │对象存储│   │ 阿里云 │
└───────┘   └───────┘   └───────┘
                │
        ┌───────▼────────┐
        │   Task Queue   │
        │   任务队列      │
        │ (Redis/RabbitMQ)│
        └────────────────┘
```

---

## 数据库设计（第三阶段）

### 文件元数据表
```sql
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) UNIQUE COMMENT '文件唯一标识',
    original_name VARCHAR(255) COMMENT '原始文件名',
    storage_path VARCHAR(500) COMMENT '存储路径',
    file_size BIGINT COMMENT '文件大小',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    extension VARCHAR(20) COMMENT '文件扩展名',
    md5_hash VARCHAR(32) COMMENT 'MD5哈希',
    sha256_hash VARCHAR(64) COMMENT 'SHA256哈希',
    source_type VARCHAR(50) COMMENT '来源类型(UPLOAD/CONVERT/EXTRACT)',
    status TINYINT DEFAULT 1 COMMENT '状态:0-删除,1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    INDEX idx_md5 (md5_hash),
    INDEX idx_created (created_at)
);
```

### 任务记录表
```sql
CREATE TABLE file_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) UNIQUE COMMENT '任务唯一标识',
    task_type VARCHAR(50) COMMENT '任务类型(CONVERT/EXTRACT/MERGE等)',
    source_files JSON COMMENT '源文件列表',
    target_files JSON COMMENT '目标文件列表',
    status VARCHAR(20) COMMENT '状态(PENDING/PROCESSING/SUCCESS/FAILED)',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
);
```

---

## 实施计划

### Q1 2026（当前季度）
**目标**：核心能力完善

- [ ] Week 1-2: Excel 处理模块（XLS/XLSX 提取、转 CSV/JSON）
- [ ] Week 3-4: 图片处理模块（格式转换、缩略图）
- [ ] Week 5-6: 文件校验模块（MD5/SHA256）
- [ ] Week 7-8: 7z/rar 支持 + 代码重构优化

### Q2 2026
**目标**：进阶功能

- [ ] Week 1-2: PDF 合并/拆分/水印
- [ ] Week 3-4: OCR 文字识别集成
- [ ] Week 5-6: Office 水印 + 文件加密
- [ ] Week 7-8: 文件压缩 + 性能优化

### Q3 2026
**目标**：企业级特性

- [ ] Week 1-2: 异步任务框架 + Redis 队列
- [ ] Week 3-4: 任务管理 API + 持久化
- [ ] Week 5-6: 多存储后端适配
- [ ] Week 7-8: 文件预览服务

### Q4 2026
**目标**：高级特性

- [ ] Week 1-2: 音视频基础处理
- [ ] Week 3-4: 文档智能（分类、敏感检测）
- [ ] Week 5-6: 模板引擎（Word/PDF 填充）
- [ ] Week 7-8: AI 集成（摘要、标签）

---

## 风险与对策

| 风险点 | 影响 | 对策 |
|-------|------|------|
| LibreOffice 依赖重 | 部署复杂 | 使用独立 Docker 容器，通过 HTTP API 调用 |
| 大文件处理 OOM | 服务崩溃 | 流式处理 + 异步任务 + 分块处理 |
| OCR 精度问题 | 提取质量差 | 多引擎支持（Tesseract+云端API），可配置 |
| 并发性能瓶颈 | 响应慢 | 线程池隔离 + 队列削峰 + 水平扩展 |
| 存储成本 | 费用高 | 支持对象存储 + 自动清理策略 |

---

## 成功指标

- **功能覆盖**：支持 20+ 种文件格式处理
- **性能指标**：
  - 单文件处理 < 100MB：响应时间 < 3s
  - 批量处理 100 个文件：完成时间 < 5min
  - 并发处理：支持 100 并发
- **稳定性**：服务可用性 > 99.9%
- **扩展性**：支持水平扩展，处理能力随节点线性增长

---

## 附录 A：详细设计文档

各阶段的详细设计方案位于 `docs/design/` 目录：

- [第一阶段：核心能力完善](./docs/design/phase1-core-capabilities.md)
- [第二阶段：进阶功能](./docs/design/phase2-advanced-features.md)
- [第三阶段：企业级特性](./docs/design/phase3-enterprise-features.md)
- [第四阶段：高级特性](./docs/design/phase4-advanced-features.md)

每个设计文档包含：功能清单、技术架构、API 设计、数据库设计、实施步骤、验收标准。

## 附录 B：待开发功能清单

### 高优先级（P0）
1. Excel 文本提取
2. Excel 转 CSV/JSON
3. 图片格式转换/缩略图
4. PDF 合并/拆分
5. OCR 文字识别
6. 异步任务框架

### 中优先级（P1）
7. 7z/rar 支持
8. 文件水印
9. 文件加密/解密
10. 文件压缩
11. 多存储后端
12. 模板填充

### 低优先级（P2/P3）
13. 音视频处理
14. 文档智能分类
15. 敏感信息检测
16. 自动摘要
17. 文件预览
18. 语音识别
