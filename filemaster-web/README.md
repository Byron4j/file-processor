# FileMaster Pro - Frontend

企业级文件处理平台前端，基于 React + TypeScript + Ant Design 构建。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19.x | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 8.x | 构建工具 |
| Ant Design | 5.x | UI 组件库 |
| Zustand | 5.x | 状态管理 |
| React Router | 6.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |
| React-i18next | 17.x | 国际化 |
| Video.js | 8.x | 视频播放 |
| PDF.js | 5.x | PDF 预览 |
| Mammoth | 1.x | Office 文档预览 |

## 功能特性

### 核心功能
- **用户认证**：JWT 登录、注册、密码重置
- **文件管理**：上传、下载、删除、重命名、移动、复制
- **文件夹管理**：创建、重命名、删除、树形结构
- **多语言支持**：中文、英文、日文、韩文
- **主题切换**：浅色/深色模式

### 文件预览
- **图片预览**：缩放、旋转、全屏
- **PDF 预览**：分页、缩放、搜索、目录
- **视频播放**：多格式支持、字幕、画质切换
- **音频播放**：播放列表、波形图
- **Office 预览**：DOCX/XLSX/PPTX 转 HTML 预览
- **代码预览**：语法高亮、行号

### 格式转换
- **文档转换**：DOC/DOCX/PDF/TXT/HTML 互转
- **图片转换**：JPEG/PNG/GIF/WebP/TIFF 互转
- **视频转换**：MP4/WebM/MOV/AVI 互转
- **音频转换**：MP3/WAV/OGG/FLAC 互转

### PDF 工具
- 合并、拆分、旋转、删除页面
- 添加文字/图片水印
- 加密/解密

### AI 功能
- 智能文档摘要
- 文档问答
- 自动标签生成
- 文档分类

### 任务管理
- 异步任务提交
- 实时进度显示
- 任务历史记录

### 文件分享
- 创建分享链接
- 密码保护
- 过期时间设置

## 项目结构

```
filemaster-web/
├── public/                    # 静态资源
│   ├── logo.svg
│   └── ...
├── src/
│   ├── api/                   # API 接口
│   │   ├── index.ts          # API 入口
│   │   ├── auth.ts           # 认证接口
│   │   ├── files.ts          # 文件接口
│   │   ├── preview.ts        # 预览接口
│   │   ├── convert.ts        # 转换接口
│   │   ├── pdf.ts            # PDF 接口
│   │   ├── tasks.ts          # 任务接口
│   │   ├── ai.ts             # AI 接口
│   │   └── extract.ts        # 提取接口
│   ├── components/           # 公共组件
│   │   ├── Layout/           # 布局组件
│   │   │   ├── MainLayout.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Header.tsx
│   │   │   └── index.less
│   │   ├── FileManager/      # 文件管理组件
│   │   └── common/           # 通用组件
│   ├── pages/                # 页面组件
│   │   ├── Login/
│   │   ├── Dashboard/
│   │   ├── Files/
│   │   ├── Preview/
│   │   ├── Convert/
│   │   ├── Tools/
│   │   ├── AI/
│   │   ├── Tasks/
│   │   ├── Share/
│   │   └── Admin/
│   ├── stores/               # 状态管理 (Zustand)
│   │   ├── authStore.ts
│   │   ├── fileStore.ts
│   │   ├── uploadStore.ts
│   │   ├── taskStore.ts
│   │   ├── themeStore.ts
│   │   └── index.ts
│   ├── hooks/                # 自定义 Hooks
│   ├── utils/                # 工具函数
│   │   ├── format.ts
│   │   ├── file.ts
│   │   ├── storage.ts
│   │   └── download.ts
│   ├── types/                # TypeScript 类型
│   ├── i18n/                 # 国际化
│   │   ├── locales/
│   │   │   ├── zh-CN.json
│   │   │   ├── en-US.json
│   │   │   ├── ja-JP.json
│   │   │   └── ko-KR.json
│   │   └── index.ts
│   ├── styles/               # 样式文件
│   ├── App.tsx
│   ├── main.tsx
│   └── vite-env.d.ts
├── .env.development          # 开发环境变量
├── .env.production           # 生产环境变量
├── vite.config.ts            # Vite 配置
├── tsconfig.json             # TypeScript 配置
└── package.json
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器默认运行在 `http://localhost:3000`，API 代理到 `http://localhost:8080`。

### 构建生产版本

```bash
npm run build
```

构建输出位于 `dist/` 目录。

### 预览生产版本

```bash
npm run preview
```

### 代码格式化

```bash
npm run format
```

### 代码检查

```bash
npm run lint
```

## 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `VITE_API_BASE_URL` | API 基础 URL | `http://localhost:8080` |
| `VITE_WS_URL` | WebSocket URL | `ws://localhost:8080/ws` |
| `VITE_APP_TITLE` | 应用标题 | `FileMaster Pro` |
| `VITE_APP_VERSION` | 应用版本 | `1.0.0` |

## 路由结构

| 路径 | 描述 | 权限 |
|------|------|------|
| `/login` | 登录页 | 公开 |
| `/share/:shareId` | 文件分享页 | 公开 |
| `/` | 仪表盘 | 需要登录 |
| `/files` | 全部文件 | 需要登录 |
| `/files/recent` | 最近文件 | 需要登录 |
| `/files/favorites` | 收藏 | 需要登录 |
| `/files/trash` | 回收站 | 需要登录 |
| `/preview/:fileId` | 文件预览 | 需要登录 |
| `/convert` | 格式转换 | 需要登录 |
| `/tools/pdf` | PDF 工具 | 需要登录 |
| `/tools/ocr` | OCR 识别 | 需要登录 |
| `/tools/watermark` | 水印工具 | 需要登录 |
| `/ai/summary` | AI 摘要 | 需要登录 |
| `/ai/qa` | 文档问答 | 需要登录 |
| `/tasks` | 任务中心 | 需要登录 |
| `/admin` | 管理后台 | 需要管理员权限 |

## 状态管理

使用 Zustand 进行状态管理，主要 Store：

- **authStore**: 用户认证状态
- **fileStore**: 文件/文件夹状态
- **uploadStore**: 上传任务状态（支持分片上传）
- **taskStore**: 异步任务状态
- **themeStore**: 主题状态（浅色/深色）

## API 集成

所有 API 请求通过 Axios 封装，支持：

- JWT Token 自动注入
- Token 自动刷新
- 请求/响应拦截器
- 统一的错误处理

## 文件预览

### 支持的预览格式

| 类型 | 格式 | 预览方式 |
|------|------|----------|
| 图片 | JPG, PNG, GIF, BMP, WebP | 原生预览 |
| PDF | PDF | PDF.js |
| 视频 | MP4, WebM, MOV, AVI | Video.js |
| 音频 | MP3, WAV, OGG, FLAC | 原生音频 |
| Office | DOCX, XLSX, PPTX | Mammoth.js 转 HTML |
| 代码 | JS, TS, HTML, CSS, PY, etc. | 语法高亮 |
| 文本 | TXT, MD, LOG | 文本预览 |

## 部署

### Docker 部署

```dockerfile
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 浏览器支持

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 许可证

MIT
