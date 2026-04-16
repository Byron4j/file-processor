# FileMaster Pro - 企业级文件处理平台

FileMaster Pro 是一个功能完整的企业级文件处理平台，提供文件管理、格式转换、PDF处理、AI智能分析、团队协作等全方位文件服务。

[![CI/CD](https://github.com/your-org/filemaster-pro/workflows/CI/CD%20Pipeline/badge.svg)](https://github.com/your-org/filemaster-pro/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 🚀 核心特性

### 文件管理
- 📁 **智能文件管理** - 拖拽上传、秒传、断点续传、文件夹管理
- 🔍 **全文搜索** - 基于Elasticsearch的文件名和内容全文检索
- 👁️ **多格式预览** - PDF、Office、图片、音视频在线预览
- 🔗 **文件分享** - 生成分享链接、密码保护、过期设置

### 格式转换
- 📝 **文档转换** - DOC/DOCX/PDF/TXT/HTML互转
- 🖼️ **图片处理** - 格式转换、压缩、缩略图生成
- 🎬 **音视频转换** - MP4/WebM/MOV/MP3/WAV格式互转
- 📊 **Excel处理** - Excel转CSV/JSON、数据提取

### PDF专业工具
- 🔀 **PDF合并/拆分** - 多文件合并、按页拆分
- 🖊️ **PDF水印** - 文字/图片水印、透明度调节
- 🔐 **PDF加密/解密** - 密码保护、权限设置
- 📝 **PDF编辑** - 旋转、删除页面、OCR文字识别

### AI智能功能
- 🤖 **智能摘要** - 文档自动摘要生成
- ❓ **文档问答** - 基于文档内容的AI问答
- 🏷️ **智能标签** - 自动内容标签生成
- 📂 **文档分类** - 合同/发票/报告/简历自动分类

### 团队协作
- 👥 **团队管理** - 创建团队、邀请成员、权限控制
- 💾 **共享空间** - 团队文件夹、协作编辑
- 📊 **用量统计** - 存储使用、操作日志

### 商业化支持
- 💳 **付费套餐** - 免费版/专业版/团队版/企业版
- 🔐 **安全合规** - 审计日志、数据加密、访问控制
- 📈 **运营分析** - 用户统计、收入报表

---

## 🛠️ 技术架构

### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2 | 核心框架 |
| Spring Security | 6.x | 安全认证 |
| MyBatis Plus | 3.5 | ORM框架 |
| MySQL | 8.0 | 主数据库 |
| Redis | 7.x | 缓存/会话 |
| Elasticsearch | 8.x | 全文搜索 |
| MinIO/阿里云OSS | - | 对象存储 |

### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19.x | UI框架 |
| TypeScript | 5.x | 开发语言 |
| Vite | 8.x | 构建工具 |
| Ant Design | 5.x | UI组件库 |
| Zustand | 5.x | 状态管理 |
| PDF.js | 5.x | PDF预览 |
| Video.js | 8.x | 视频播放 |

---

## 📁 项目结构

```
AI-office/
├── file-processor/          # 后端服务
│   ├── src/main/java/
│   │   ├── controller/      # 28个API控制器
│   │   ├── service/         # 业务服务层
│   │   ├── entity/          # 数据实体
│   │   ├── mapper/          # MyBatis映射
│   │   ├── security/        # 安全认证
│   │   ├── config/          # 配置类
│   │   └── util/            # 工具类
│   └── src/test/            # 测试代码
├── filemaster-web/          # 前端应用
│   ├── src/
│   │   ├── pages/           # 页面组件
│   │   ├── components/      # 公共组件
│   │   ├── api/             # API接口
│   │   ├── stores/          # 状态管理
│   │   └── types/           # TypeScript类型
│   └── Dockerfile
├── docker-compose.yml       # 容器编排
└── docs/                    # 文档
    └── COMMERCIAL_ROADMAP.md # 商用完善计划
```

---

## 🚀 快速开始

### 环境要求
- Java 21+
- Node.js 20+
- MySQL 8.0+
- Redis 7+
- Elasticsearch 8+ (可选，用于搜索功能)

### 1. 克隆项目

```bash
git clone https://github.com/your-org/filemaster-pro.git
cd filemaster-pro
```

### 2. 启动依赖服务

```bash
# 使用Docker启动所有依赖
docker-compose up -d mysql redis elasticsearch
```

### 3. 运行后端

```bash
cd file-processor
mvn clean install
mvn spring-boot:run
```

### 4. 运行前端

```bash
cd filemaster-web
npm install
npm run dev
```

### 5. 访问应用

- Web界面: http://localhost:3000
- API文档: http://localhost:8080/swagger-ui.html
- H2控制台: http://localhost:8080/h2-console

---

## 📚 API文档

启动后端服务后，访问 Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

### 主要API模块
- **认证**: `/api/auth/**` - 登录、注册、Token刷新
- **文件**: `/api/files/**` - 文件上传、下载、管理
- **转换**: `/api/file/convert/**` - 格式转换
- **PDF**: `/api/pdf/**` - PDF处理工具
- **AI**: `/api/ai/**` - AI智能功能
- **任务**: `/api/tasks/**` - 异步任务管理
- **分享**: `/api/share/**` - 文件分享
- **团队**: `/api/team/**` - 团队协作
- **订阅**: `/api/subscription/**` - 套餐订阅

---

## 🧪 测试

### 后端测试
```bash
cd file-processor
mvn test
```

### 前端测试
```bash
cd filemaster-web
npm run test
```

---

## 🚢 部署

### Docker部署

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 生产环境配置

1. 复制 `.env.example` 为 `.env` 并填写配置
2. 配置SSL证书
3. 配置外部存储(Minio/阿里云OSS)
4. 配置AI服务API Key

---

## 📖 文档

- [商用完善计划](docs/COMMERCIAL_ROADMAP.md) - 详细的商用路线图
- [演示手册](演示操作手册.md) - 功能演示指南
- [后端CLAUDE.md](file-processor/CLAUDE.md) - 后端开发指南
- [前端README](filemaster-web/README.md) - 前端开发指南

---

## 🗺️ 路线图

### ✅ 已实现功能
- [x] 用户认证与授权 (JWT)
- [x] 文件上传/下载/管理
- [x] 多格式文件预览
- [x] 文档/图片/视频格式转换
- [x] PDF合并/拆分/水印/加密
- [x] AI文档摘要与问答
- [x] 异步任务处理
- [x] 文件分享功能
- [x] 秒传功能
- [x] 全文搜索
- [x] 团队协作
- [x] 订阅套餐
- [x] API文档 (Swagger)
- [x] 审计日志

### ✅ 已实现功能
- [x] 用户认证与授权 (JWT)
- [x] 文件上传/下载/管理
- [x] 多格式文件预览
- [x] 文档/图片/视频格式转换
- [x] PDF合并/拆分/水印/加密
- [x] AI文档摘要与问答
- [x] 异步任务处理
- [x] 文件分享功能
- [x] 秒传功能
- [x] 全文搜索
- [x] 团队协作
- [x] 订阅套餐
- [x] API文档 (Swagger)
- [x] 审计日志
- [x] 支付集成 (支付宝/微信) ✅
- [x] SSL证书配置文档 ✅

### 🚧 待完善功能
- [ ] 在线编辑 (OnlyOffice)
- [ ] 移动端APP
- [ ] 更多AI模型支持

---

## 🤝 贡献指南

欢迎提交Issue和Pull Request!

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

[MIT](LICENSE) © FileMaster Team

---

## 💬 联系我们

- 邮箱: support@filemaster.pro
- 官网: https://filemaster.pro
- 文档: https://docs.filemaster.pro
