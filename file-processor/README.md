# FileMaster Pro - 企业级文件处理平台

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg" alt="Spring Boot 3.2.0">
  <img src="https://img.shields.io/badge/MyBatis%20Plus-3.5.5-orange.svg" alt="MyBatis Plus">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License MIT">
</p>

FileMaster Pro 是一个企业级文件处理与转换平台，支持 100+ 种文件格式的转换、处理、分析和智能识别。基于 Java 21 + Spring Boot 3 构建，提供高可用、高性能的文件处理能力。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [使用方式](#使用方式)
- [配置说明](#配置说明)
- [运维部署手册](#运维部署手册)
- [项目结构](#项目结构)
- [API 文档](#api-文档)
- [开发文档](#开发文档)

---

## 功能特性

### 一、文档处理

| 功能 | 描述 | 状态 |
|------|------|------|
| **DOC 转 DOCX** | 旧版 Word 文档转换，保留格式 | ✅ |
| **文档文本提取** | 从 DOC/DOCX/PDF/PPT/PPTX 提取文本 | ✅ |
| **PDF 合并/拆分** | 支持书签、页面范围、奇偶页拆分 | ✅ |
| **PDF 旋转/删除** | 页面旋转（90/180/270度）和删除 | ✅ |
| **PDF 加密解密** | AES-256 加密、权限控制 | ✅ |
| **PDF 水印** | 文字/图片水印、透明度、旋转 | ✅ |
| **Word 水印** | 页眉文字水印 | ✅ |
| **Office 预览** | Office 转 PDF 在线预览 | ✅ |
| **模板引擎** | Word 模板渲染、占位符替换 | ✅ |

### 二、Excel 处理

| 功能 | 描述 | 状态 |
|------|------|------|
| **文本提取** | 提取单元格数据和公式 | ✅ |
| **转 CSV** | 自定义分隔符、编码 | ✅ |
| **转 JSON** | 支持表头映射、数据格式化 | ✅ |
| **Sheet 信息** | 获取工作表元数据 | ✅ |

### 三、图片处理

| 功能 | 描述 | 状态 |
|------|------|------|
| **格式转换** | JPEG/PNG/GIF/BMP/WebP/TIFF 互转 | ✅ |
| **缩略图生成** | FIT/FILL/SCALE 三种模式 | ✅ |
| **图片压缩** | 质量调节、尺寸限制 | ✅ |
| **图片信息** | EXIF 元数据读取 | ✅ |

### 四、压缩包处理

| 功能 | 描述 | 状态 |
|------|------|------|
| **7z 解压** | 支持密码保护 | ✅ |
| **RAR 解压** | RAR/RAR5 格式支持 | ✅ |
| **ZIP 提取** | 智能查找 index.html | ✅ |
| **压缩包信息** | 文件列表、压缩率统计 | ✅ |

### 五、PDF 高级处理

| 功能 | 描述 | 状态 |
|------|------|------|
| **PDF 合并** | 多文件合并、书签保留 | ✅ |
| **PDF 拆分** | 按范围/每N页/指定页提取 | ✅ |
| **PDF 旋转** | 指定页面旋转 | ✅ |
| **PDF 删除页面** | 批量删除指定页 | ✅ |
| **PDF 信息提取** | 页数、作者、加密状态 | ✅ |

### 六、音视频处理（FFmpeg）

| 功能 | 描述 | 状态 |
|------|------|------|
| **视频信息** | 时长、分辨率、码率、编码格式 | ✅ |
| **音频信息** | 采样率、声道、比特率 | ✅ |
| **视频缩略图** | 指定时间点截图 | ✅ |
| **视频转码** | H.264/H.265/VP9 编码转换 | ✅ |
| **音频转码** | MP3/AAC/FLAC/OGG 转换 | ✅ |
| **视频合并** | 多视频拼接 | ✅ |
| **视频剪辑** | 时间段截取 | ✅ |
| **GIF 生成** | 视频转 GIF、帧率控制 | ✅ |
| **字幕烧录** | SRT/VTT 字幕嵌入 | ✅ |
| **视频水印** | 文字/图片水印 | ✅ |
| **音频转录** | Whisper API 语音识别 | ✅ |
| **字幕生成** | 自动生成 SRT/VTT | ✅ |

### 七、OCR 文字识别

| 功能 | 描述 | 状态 |
|------|------|------|
| **Tesseract OCR** | 多语言识别（中文/英文/日文等） | ✅ |
| **PaddleOCR** | 百度 PaddleOCR 集成 | ✅ |
| **PDF OCR** | 扫描件文字提取 | ✅ |
| **图片 OCR** | 图片文字识别 | ✅ |

### 八、文档智能

| 功能 | 描述 | 状态 |
|------|------|------|
| **文档分类** | 合同/发票/简历/报告自动分类 | ✅ |
| **敏感信息检测** | 身份证号/手机号/银行卡/邮箱检测 | ✅ |
| **关键词提取** | HanLP TextRank 算法 | ✅ |
| **文本摘要** | 自动提取文章摘要 | ✅ |

### 九、AI 集成

| 功能 | 描述 | 状态 |
|------|------|------|
| **AI 文档摘要** | Claude/OpenAI 智能摘要 | ✅ |
| **AI 标签生成** | 自动提取文档标签 | ✅ |
| **AI 问答** | 基于文档内容的智能问答 | ✅ |

### 十、企业级特性

| 功能 | 描述 | 状态 |
|------|------|------|
| **JWT 认证** | Access/Refresh Token 双令牌机制 | ✅ |
| **RBAC 权限** | 角色权限控制（SUPER_ADMIN/ADMIN/USER/GUEST） | ✅ |
| **API 限流** | 基于 Redis 的令牌桶限流 | ✅ |
| **审计日志** | 操作日志记录与查询 | ✅ |
| **文件分享** | 密码保护、过期时间、下载次数限制 | ✅ |
| **病毒扫描** | ClamAV 集成 | ✅ |
| **文件加密** | AES-256-GCM 文件级加密 | ✅ |
| **回收站** | 软删除、自动清理 | ✅ |
| **用户配额** | 存储空间/日上传量限制 | ✅ |
| **异步任务** | 任务队列、进度跟踪、回调通知 | ✅ |
| **批量处理** | 批量转换/水印/提取/哈希 | ✅ |
| **文件版本** | 多版本管理、版本回滚 | ✅ |
| **文件分片** | 大文件分片上传/下载 | ✅ |
| **断点续传** | 上传/下载断点续传 | ✅ |
| **秒传** | 基于哈希的文件秒传 | ✅ |

---

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Security | 6.2 | 安全认证 |
| Spring Data Redis | 3.2 | 缓存 |
| Spring WebSocket | 3.2 | 实时通信 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| JWT | 0.12.3 | Token 认证 |
| H2/MySQL | 8.0 | 数据库 |

### 文件处理库

| 库 | 版本 | 用途 |
|-----|------|------|
| Apache POI | 5.2.5 | Office 文档处理 |
| Apache PDFBox | 3.0.1 | PDF 处理 |
| Apache Commons CSV | 1.10.0 | CSV 处理 |
| Thumbnailator | 0.4.20 | 图片处理 |
| SevenZipJBinding | 16.02 | 7z 解压 |
| JunRar | 7.5.5 | RAR 解压 |
| Tesseract | 5.11.0 | OCR 识别 |
| FFmpeg | 6.0 | 音视频处理 |

### AI & NLP

| 库 | 版本 | 用途 |
|-----|------|------|
| HanLP | 1.8.4 | 中文 NLP |
| OpenNLP | 2.3.0 | 命名实体识别 |
| OpenAI SDK | 0.18.2 | OpenAI API |
| Anthropic SDK | 0.1.0 | Claude API |

### 基础设施

| 技术 | 用途 |
|------|------|
| Redis | 缓存、限流、会话 |
| RabbitMQ | 异步任务队列 |
| MinIO | 对象存储 |
| Meilisearch | 全文搜索 |
| ClamAV | 病毒扫描 |
| Prometheus | 监控指标 |
| Grafana | 监控仪表盘 |
| ELK Stack | 日志收集分析 |

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0（可选，默认使用 H2）
- Redis 7.0（可选，用于高级功能）
- FFmpeg 6.0+（可选，用于音视频处理）
- Tesseract 5.0+（可选，用于 OCR）

### 本地开发

```bash
# 1. 克隆项目
git clone https://github.com/your-org/filemaster-pro.git
cd filemaster-pro

# 2. 编译
mvn clean compile

# 3. 运行测试
mvn test

# 4. 启动应用
mvn spring-boot:run

# 5. 验证启动
curl http://localhost:8080/api/file/health
```

### 默认访问信息

| 服务 | URL | 默认账号 |
|------|-----|----------|
| API 服务 | http://localhost:8080 | - |
| H2 控制台 | http://localhost:8080/h2-console | sa/空密码 |
| 管理员账号 | - | admin/admin123 |

---

## 使用方式

### 1. 认证

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 响应
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400,
    "tokenType": "Bearer",
    "username": "admin",
    "roles": ["SUPER_ADMIN"]
  }
}
```

### 2. 文件上传

```bash
# 分片上传初始化
curl -X POST http://localhost:8080/api/file/transfer/upload/init \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "large-video.mp4",
    "fileSize": 1073741824,
    "chunkSize": 10485760
  }'

# 上传分片
curl -X POST http://localhost:8080/api/file/transfer/upload/chunk \
  -H "Authorization: Bearer <token>" \
  -F "uploadId=<uploadId>" \
  -F "chunkNumber=1" \
  -F "file=@/path/to/chunk1"

# 合并分片
curl -X POST http://localhost:8080/api/file/transfer/upload/complete \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "uploadId": "<uploadId>",
    "md5Hash": "d41d8cd98f00b204e9800998ecf8427e"
  }'
```

### 3. 文档转换

```bash
# DOC 转 DOCX
curl -X POST http://localhost:8080/api/file/convert/doc-to-docx \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/document.doc",
    "targetPath": "/outputs/document.docx"
  }'

# Excel 转 CSV
curl -X POST http://localhost:8080/api/excel/convert/csv \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/data.xlsx",
    "targetPath": "/outputs/data.csv",
    "delimiter": ","
  }'

# 图片格式转换
curl -X POST http://localhost:8080/api/image/convert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/photo.jpg",
    "targetPath": "/outputs/photo.png",
    "targetFormat": "PNG"
  }'
```

### 4. PDF 处理

```bash
# PDF 合并
curl -X POST http://localhost:8080/api/pdf/merge \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePaths": ["/uploads/doc1.pdf", "/uploads/doc2.pdf"],
    "targetPath": "/outputs/merged.pdf",
    "bookmarks": true
  }'

# PDF 添加水印
curl -X POST http://localhost:8080/api/watermark/pdf/text \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/document.pdf",
    "targetPath": "/outputs/watermarked.pdf",
    "text": "CONFIDENTIAL",
    "opacity": 0.3,
    "rotation": 45,
    "fontSize": 48
  }'

# PDF 加密
curl -X POST http://localhost:8080/api/security/pdf/encrypt \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/document.pdf",
    "targetPath": "/outputs/encrypted.pdf",
    "userPassword": "open123",
    "ownerPassword": "owner456",
    "canPrint": true,
    "canCopy": false
  }'
```

### 5. 音视频处理

```bash
# 获取视频信息
curl "http://localhost:8080/api/media/video/info?path=/uploads/video.mp4" \
  -H "Authorization: Bearer <token>"

# 视频转码
curl -X POST http://localhost:8080/api/media/video/transcode \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/video.mp4",
    "targetPath": "/outputs/video_720p.mp4",
    "videoCodec": "h264",
    "width": 1280,
    "height": 720,
    "videoBitrate": 2000000
  }'

# 视频剪辑
curl -X POST http://localhost:8080/api/media/video/trim \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/video.mp4",
    "targetPath": "/outputs/clip.mp4",
    "startTime": 10.5,
    "endTime": 45.0
  }'

# 音频转录（Whisper）
curl -X POST http://localhost:8080/api/media/audio/transcribe \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourcePath": "/uploads/audio.mp3",
    "language": "zh"
  }'
```

### 6. AI 功能

```bash
# AI 文档摘要
curl -X POST http://localhost:8080/api/ai/summary \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/uploads/document.pdf",
    "maxTokens": 1000
  }'

# AI 问答
curl -X POST http://localhost:8080/api/ai/ask \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/uploads/document.pdf",
    "question": "合同的交付日期是什么时候？"
  }'

# 文档分类
curl -X POST http://localhost:8080/api/intelligence/classify \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/uploads/document.pdf"
  }'

# 敏感信息检测
curl -X POST http://localhost:8080/api/intelligence/sensitive-info \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/uploads/document.pdf"
  }'
```

### 7. 批量处理

```bash
# 提交批量转换任务
curl -X POST http://localhost:8080/api/batch/convert \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "files": ["/uploads/doc1.doc", "/uploads/doc2.doc"],
    "targetFormat": "PDF",
    "outputDir": "/outputs/batch/",
    "async": true,
    "callbackUrl": "https://your-server.com/callback"
  }'

# 查询任务状态
curl "http://localhost:8080/api/tasks/{taskId}/status" \
  -H "Authorization: Bearer <token>"
```

### 8. 文件分享

```bash
# 创建分享
curl -X POST http://localhost:8080/api/share/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "f-xxx-xxx",
    "password": "123456",
    "expireHours": 24,
    "maxDownloads": 10,
    "allowPreview": true
  }'

# 访问分享（无需认证）
curl "http://localhost:8080/api/share/{shareId}?token=xxx"

# 下载分享文件
curl "http://localhost:8080/api/share/{shareId}/download?token=xxx&password=123456"
```

---

## 配置说明

### 核心配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: filemaster-pro
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:filemaster;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
    username: sa
    password:
  redis:
    host: localhost
    port: 6379
    password:
    database: 0

# JWT 配置
jwt:
  secret: ${JWT_SECRET:your-secret-key-at-least-256-bits-long}
  access-token-expiration: 86400000      # 24 小时
  refresh-token-expiration: 604800000    # 7 天

# 存储配置
storage:
  default: LOCAL
  local:
    base-path: ./uploads
  minio:
    enabled: false
    endpoint: http://localhost:9000
    access-key: ${MINIO_ACCESS_KEY:}
    secret-key: ${MINIO_SECRET_KEY:}
    bucket: filemaster
  aliyun-oss:
    enabled: false
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: ${OSS_ACCESS_KEY_ID:}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
    bucket: filemaster

# 用户配额
quota:
  default-storage: 10737418240         # 10 GB
  default-daily-upload: 1073741824     # 1 GB
  default-max-files: 10000

# AI 配置
ai:
  provider: claude                      # claude 或 openai
  claude:
    api-key: ${CLAUDE_API_KEY:}
    model: claude-3-sonnet-20240229
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: gpt-4-turbo-preview

# 媒体处理
media:
  ffmpeg:
    path: ffmpeg
    timeout: 600
  whisper:
    mode: api                           # api 或 local
    api-key: ${OPENAI_API_KEY:}
    model: whisper-1

# OCR 配置
ocr:
  default-engine: tesseract
  tesseract:
    data-path: /usr/share/tesseract-ocr/4.00/tessdata
    default-language: chi_sim+eng

# 病毒扫描
clamav:
  enabled: false
  host: localhost
  port: 3310

# 回收站
recycle-bin:
  retention-days: 30
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `JWT_SECRET` | JWT 签名密钥 | 自动生成 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | filemaster123 |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | minioadmin |
| `MINIO_SECRET_KEY` | MinIO 密钥 | minioadmin123 |
| `CLAUDE_API_KEY` | Claude API 密钥 | 空 |
| `OPENAI_API_KEY` | OpenAI API 密钥 | 空 |
| `RABBITMQ_USER` | RabbitMQ 用户名 | admin |
| `RABBITMQ_PASS` | RabbitMQ 密码 | admin123 |

---

## 运维部署手册

### 一、Docker Compose 部署（推荐）

#### 1. 环境准备

```bash
# 安装 Docker 和 Docker Compose
# https://docs.docker.com/get-docker/

# 验证安装
docker --version
docker-compose --version
```

#### 2. 项目部署

```bash
# 1. 克隆项目
git clone https://github.com/your-org/filemaster-pro.git
cd filemaster-pro/docker

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，设置安全的密码

# 3. 创建数据目录
mkdir -p data/{mysql,redis,minio,rabbitmq,prometheus,grafana}

# 4. 启动服务（完整栈）
docker-compose up -d

# 5. 查看服务状态
docker-compose ps

# 6. 查看日志
docker-compose logs -f file-master
```

#### 3. 服务端口

| 服务 | 端口 | 访问地址 | 说明 |
|------|------|----------|------|
| API Gateway | 8080 | http://localhost:8080 | REST API |
| Web UI | 80/443 | http://localhost | Nginx 前端 |
| MySQL | 3306 | localhost:3306 | 数据库 |
| Redis | 6379 | localhost:6379 | 缓存 |
| RabbitMQ | 5672/15672 | localhost:15672 | 消息队列控制台 |
| MinIO | 9000/9001 | localhost:9001 | 对象存储控制台 |
| Meilisearch | 7700 | localhost:7700 | 搜索引擎 |
| Prometheus | 9090 | localhost:9090 | 监控采集 |
| Grafana | 3000 | localhost:3000 | 监控仪表盘 |
| Kibana | 5601 | localhost:5601 | 日志查询 |

#### 4. 常用运维命令

```bash
# 查看所有服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f [service-name]

# 重启服务
docker-compose restart file-master

# 扩容 Worker 节点
docker-compose up -d --scale file-worker=3

# 停止所有服务
docker-compose down

# 停止并清理数据（危险！）
docker-compose down -v

# 更新镜像
docker-compose pull
docker-compose up -d

# 进入容器
docker-compose exec file-master bash

# 备份数据库
docker-compose exec mysql mysqldump -u root -p filemaster > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p filemaster < backup.sql
```

### 二、传统部署

#### 1. 服务器要求

| 配置 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 4 核 | 8 核+ |
| 内存 | 8 GB | 16 GB+ |
| 磁盘 | 100 GB SSD | 500 GB+ SSD |
| 网络 | 10 Mbps | 100 Mbps+ |

#### 2. 依赖安装

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-21-jdk ffmpeg tesseract-ocr tesseract-ocr-chi-sim

# CentOS/RHEL
sudo yum install -y java-21-openjdk ffmpeg tesseract tesseract-langpack-chi_sim

# 验证安装
java -version
ffmpeg -version
tesseract --version
```

#### 3. 应用部署

```bash
# 1. 创建应用目录
sudo mkdir -p /opt/filemaster
cd /opt/filemaster

# 2. 上传 JAR 包
# 方式1：直接上传
scp target/file-processor-1.0.0.jar user@server:/opt/filemaster/

# 方式2：从 Maven 仓库下载
wget https://your-nexus-repo/file-processor-1.0.0.jar

# 3. 创建配置文件
mkdir -p config
cat > config/application-prod.yml << 'EOF'
server:
  port: 8080

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/filemaster?useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:filemaster}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD}

jwt:
  secret: ${JWT_SECRET}

storage:
  default: LOCAL
  local:
    base-path: /data/filemaster/uploads
EOF

# 4. 创建启动脚本
cat > start.sh << 'EOF'
#!/bin/bash
APP_NAME=file-processor
JAR_NAME=file-processor-1.0.0.jar
LOG_PATH=/var/log/filemaster

# 创建日志目录
mkdir -p $LOG_PATH

# 启动应用
nohup java -Xms2g -Xmx4g \
  -Dspring.profiles.active=prod \
  -Dfile.encoding=UTF-8 \
  -jar $JAR_NAME \
  --spring.config.location=config/ \
  > $LOG_PATH/app.log 2>&1 &

echo "Application started, PID: $!"
EOF

chmod +x start.sh

# 5. 创建 Systemd 服务
sudo cat > /etc/systemd/system/filemaster.service << 'EOF'
[Unit]
Description=FileMaster Pro Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=filemaster
Group=filemaster
WorkingDirectory=/opt/filemaster
ExecStart=/opt/filemaster/start.sh
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 6. 启动服务
sudo systemctl daemon-reload
sudo systemctl enable filemaster
sudo systemctl start filemaster

# 7. 查看状态
sudo systemctl status filemaster
sudo tail -f /var/log/filemaster/app.log
```

### 三、监控告警

#### 1. 健康检查端点

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# 详细健康信息
curl http://localhost:8080/actuator/health/details

# 指标数据
curl http://localhost:8080/actuator/metrics

# Prometheus 格式
curl http://localhost:8080/actuator/prometheus
```

#### 2. 关键指标

| 指标类别 | 指标名称 | 告警阈值 |
|----------|----------|----------|
| **API** | 请求速率 | - |
| | P99 响应时间 | > 5s |
| | 错误率 | > 10% |
| **JVM** | 堆内存使用 | > 80% |
| | GC 暂停时间 | > 1s |
| **磁盘** | 使用率 | > 85% |
| **队列** | 任务队列深度 | > 1000 |

#### 3. 日志管理

```bash
# 查看实时日志
tail -f /var/log/filemaster/app.log

# 按关键字搜索
grep "ERROR" /var/log/filemaster/app.log | tail -100

# 查看特定日期日志
cat /var/log/filemaster/app.2024-01-15.log | grep "TaskService"

# ELK 查询示例
# 在 Kibana Dev Tools 中执行
GET /filemaster-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "level": "ERROR" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  }
}
```

### 四、备份恢复

#### 1. 数据库备份

```bash
# 全量备份
mysqldump -u root -p --all-databases > backup_$(date +%Y%m%d).sql

# 仅备份业务数据
mysqldump -u root -p filemaster > filemaster_$(date +%Y%m%d).sql

# 自动备份脚本（添加到 crontab）
cat > /opt/backup/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=/opt/backup
DATE=$(date +%Y%m%d)

# 数据库备份
mysqldump -u root -p${DB_PASSWORD} filemaster > $BACKUP_DIR/db_$DATE.sql

# 文件备份
tar -czf $BACKUP_DIR/files_$DATE.tar.gz /data/filemaster/uploads

# 保留最近 7 天备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
EOF

chmod +x /opt/backup/backup.sh

# 每天凌晨 2 点执行备份
echo "0 2 * * * /opt/backup/backup.sh" | crontab -
```

#### 2. 灾难恢复

```bash
# 1. 停止应用
sudo systemctl stop filemaster

# 2. 恢复数据库
mysql -u root -p filemaster < backup_20240115.sql

# 3. 恢复文件
tar -xzf files_20240115.tar.gz -C /

# 4. 启动应用
sudo systemctl start filemaster

# 5. 验证
curl http://localhost:8080/actuator/health
```

### 五、性能优化

#### 1. JVM 调优

```bash
# 生产环境 JVM 参数
java -Xms4g -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+AlwaysPreTouch \
  -XX:+DisableExplicitGC \
  -jar file-processor.jar
```

#### 2. 数据库优化

```sql
-- 添加常用查询索引
CREATE INDEX idx_file_metadata_user_id ON file_metadata(user_id);
CREATE INDEX idx_file_metadata_deleted ON file_metadata(is_deleted);
CREATE INDEX idx_task_status ON task_record(status, created_at);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- 定期清理历史数据（保留 90 天）
DELETE FROM audit_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
DELETE FROM task_record WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY) AND status IN ('SUCCESS', 'FAILED');
```

#### 3. 缓存策略

```yaml
# Redis 缓存配置
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 小时
      cache-null-values: false
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 项目结构

```
filemaster-pro/
├── src/
│   ├── main/
│   │   ├── java/com/fileprocessor/
│   │   │   ├── annotation/          # 自定义注解
│   │   │   │   ├── AuditLog.java
│   │   │   │   └── RateLimit.java
│   │   │   ├── aop/                 # AOP 切面
│   │   │   │   ├── AuditLogAspect.java
│   │   │   │   └── RateLimitAspect.java
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/          # REST API 控制器
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── BatchController.java
│   │   │   │   ├── DocumentConversionController.java
│   │   │   │   ├── DocumentIntelligenceController.java
│   │   │   │   ├── EncryptionController.java
│   │   │   │   ├── ExcelController.java
│   │   │   │   ├── FileConvertController.java
│   │   │   │   ├── FileTransferController.java
│   │   │   │   ├── MediaController.java
│   │   │   │   ├── MediaEnhancementController.java
│   │   │   │   ├── OcrController.java
│   │   │   │   ├── PdfController.java
│   │   │   │   ├── PreviewController.java
│   │   │   │   ├── RecycleBinController.java
│   │   │   │   ├── ShareController.java
│   │   │   │   ├── TaskController.java
│   │   │   │   ├── TemplateController.java
│   │   │   │   └── WatermarkController.java
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   ├── entity/              # 实体类
│   │   │   ├── exception/           # 异常处理
│   │   │   ├── mapper/              # MyBatis Mapper
│   │   │   ├── ocr/                 # OCR 服务
│   │   │   ├── security/            # 安全认证
│   │   │   ├── service/             # 业务逻辑
│   │   │   ├── util/                # 工具类
│   │   │   └── FileProcessorApplication.java
│   │   └── resources/
│   │       ├── db/migration/        # 数据库迁移脚本
│   │       ├── application.yml      # 主配置
│   │       ├── application-dev.yml  # 开发配置
│   │       ├── application-prod.yml # 生产配置
│   │       └── logback-spring.xml   # 日志配置
│   └── test/                        # 测试代码
├── docker/                          # Docker 部署文件
│   ├── docker-compose.yml
│   ├── Dockerfile
│   ├── nginx/
│   ├── prometheus/
│   └── grafana/
├── docs/                            # 文档
│   └── design/
│       ├── phase1-core-capabilities.md
│       ├── phase2-advanced-features.md
│       ├── phase3-enterprise-features.md
│       ├── phase4-advanced-features.md
│       └── commercial/              # 商用版设计
├── scripts/                         # 部署脚本
├── uploads/                         # 上传文件目录
├── outputs/                         # 输出文件目录
├── pom.xml                          # Maven 配置
├── README.md                        # 本文件
├── CLAUDE.md                        # Claude Code 指引
└── ROADMAP.md                       # 发展规划
```

---

## API 文档

启动应用后访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

主要 API 分组：

| 分组 | 基础路径 | 说明 |
|------|----------|------|
| 认证 | `/api/auth/**` | 登录、刷新令牌 |
| 文件传输 | `/api/file/transfer/**` | 上传、下载、分片 |
| 文档转换 | `/api/file/convert/**` | 格式转换 |
| Excel | `/api/excel/**` | Excel 处理 |
| 图片 | `/api/image/**` | 图片处理 |
| PDF | `/api/pdf/**` | PDF 处理 |
| 水印 | `/api/watermark/**` | 水印处理 |
| 安全 | `/api/security/**` | 加密、解密、病毒扫描 |
| 音视频 | `/api/media/**` | 音视频处理 |
| AI | `/api/ai/**` | AI 功能 |
| 批量 | `/api/batch/**` | 批量处理 |
| 任务 | `/api/tasks/**` | 异步任务 |
| 分享 | `/api/share/**` | 文件分享 |
| 回收站 | `/api/recycle-bin/**` | 回收站 |

---

## 开发文档

### 1. 本地开发环境

```bash
# 1. 克隆代码
git clone https://github.com/your-org/filemaster-pro.git
cd filemaster-pro

# 2. 安装依赖
mvn clean install -DskipTests

# 3. 启动基础服务（使用 Docker）
docker-compose -f docker/docker-compose.dev.yml up -d mysql redis

# 4. 启动应用（开发模式）
mvn spring-boot:run -Dspring.profiles.active=dev

# 5. 运行测试
mvn test

# 6. 代码检查
mvn checkstyle:check
mvn spotbugs:check
```

### 2. 添加新功能

参考 `CLAUDE.md` 中的开发规范：

1. **Controller**: REST 端点，参数校验
2. **Service**: 业务逻辑，事务管理
3. **Util**: 静态工具类，实际处理逻辑
4. **DTO**: 请求/响应数据传输对象
5. **Entity**: 数据库实体
6. **Mapper**: MyBatis 数据访问

### 3. 测试规范

```java
@SpringBootTest
class FileConvertTest {
    
    @Test
    void testDocToDocxConversion() {
        // Given
        String sourcePath = "test-files/sample.doc";
        String targetPath = "test-files/output.docx";
        
        // When
        FileResponse response = fileConvertService.convertDocToDocx(sourcePath, targetPath);
        
        // Then
        assertTrue(response.isSuccess());
        assertTrue(Files.exists(Path.of(targetPath)));
    }
}
```

### 4. 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/xxx`)
3. 提交变更 (`git commit -am 'Add xxx feature'`)
4. 推送分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

---

## 许可证

[MIT License](LICENSE)

---

## 支持与联系

- **GitHub Issues**: https://github.com/your-org/filemaster-pro/issues
- **文档中心**: https://docs.filemaster.pro
- **商业支持**: support@filemaster.pro

---

<p align="center">
  <b>FileMaster Pro - 让文件处理更简单</b>
</p>
