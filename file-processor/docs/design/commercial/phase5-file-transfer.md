# Phase 5: 文件传输与存储完善

## 目标
完善文件上传下载能力，支持 HTTP 传输、大文件处理、断点续传等企业级特性。

## 功能清单

### 1. HTTP 文件上传 API

**多文件上传**
```
POST /api/files/upload
Content-Type: multipart/form-data

参数:
- files: File[] (支持多文件)
- categoryId: Long (分类ID)
- tags: String[] (标签)
- description: String (描述)

响应:
{
  "success": true,
  "data": {
    "uploadedFiles": [
      {
        "fileId": "f-xxx-xxx",
        "originalName": "document.pdf",
        "fileSize": 1024000,
        "storagePath": "/2024/04/11/f-xxx-xxx.pdf",
        "hash": "sha256:abc...",
        "status": "UPLOADED"
      }
    ],
    "totalSize": 1024000,
    "duplicateSkipped": 0
  }
}
```

**分片上传 - 初始化**
```
POST /api/files/upload/init

请求:
{
  "fileName": "large-video.mp4",
  "fileSize": 2147483648,
  "chunkSize": 5242880,
  "fileHash": "sha256:xxx..."
}

响应:
{
  "success": true,
  "data": {
    "uploadId": "upload-xxx-xxx",
    "totalChunks": 410,
    "chunkSize": 5242880,
    "uploadedChunks": [],
    "expiresAt": "2024-04-11T12:00:00Z"
  }
}
```

**分片上传 - 上传块**
```
POST /api/files/upload/chunk/{uploadId}/{chunkNumber}
Content-Type: multipart/form-data

参数:
- chunk: File (分片文件)
- chunkHash: String (分片校验)
```

**分片上传 - 完成**
```
POST /api/files/upload/complete/{uploadId}

响应:
{
  "success": true,
  "data": {
    "fileId": "f-xxx-xxx",
    "fileName": "large-video.mp4",
    "fileSize": 2147483648,
    "storagePath": "/uploads/2024/04/11/f-xxx-xxx.mp4",
    "hash": "sha256:xxx..."
  }
}
```

### 2. 文件秒传
基于文件 Hash 实现秒传功能：
```
POST /api/files/upload/check

请求:
{
  "fileHash": "sha256:xxx...",
  "fileSize": 1024000
}

响应:
{
  "success": true,
  "data": {
    "exists": true,
    "fileId": "f-xxx-xxx",
    "message": "文件已存在，已完成秒传"
  }
}
```

### 3. 文件下载
```
GET /api/files/download/{fileId}
Headers:
  - Accept-Ranges: bytes
  - Content-Disposition: attachment; filename="xxx.pdf"

断点续传:
GET /api/files/download/{fileId}
Range: bytes=0-1023
```

### 4. 批量打包下载
```
POST /api/files/download/batch

请求:
{
  "fileIds": ["f-1", "f-2", "f-3"],
  "archiveName": "documents.zip",
  "archiveFormat": "ZIP"  // ZIP, 7z
}

响应 (异步):
{
  "success": true,
  "data": {
    "taskId": "t-xxx-xxx",
    "status": "PROCESSING",
    "downloadUrl": null
  }
}
```

## 数据库设计

```sql
-- 文件上传会话表
CREATE TABLE file_upload_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upload_id VARCHAR(64) UNIQUE NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(128) NOT NULL,
    chunk_size INT NOT NULL DEFAULT 5242880,
    total_chunks INT NOT NULL,
    uploaded_chunks JSON,
    status VARCHAR(20) NOT NULL,  -- PENDING, UPLOADING, COMPLETED, EXPIRED
    storage_path VARCHAR(1000),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_upload_id (upload_id),
    INDEX idx_file_hash (file_hash),
    INDEX idx_expires_at (expires_at)
);

-- 文件记录表增强
ALTER TABLE file_record ADD COLUMN (
    file_hash VARCHAR(128) COMMENT '文件SHA-256哈希',
    hash_algorithm VARCHAR(20) DEFAULT 'SHA-256',
    storage_type VARCHAR(20) DEFAULT 'LOCAL' COMMENT 'LOCAL, MINIO, OSS',
    storage_bucket VARCHAR(100),
    storage_key VARCHAR(1000),
    access_count INT DEFAULT 0 COMMENT '访问次数',
    last_access_time TIMESTAMP NULL,
    mime_type VARCHAR(100),
    INDEX idx_file_hash (file_hash),
    INDEX idx_storage_type (storage_type)
);
```

## 存储策略

```java
@Service
public class StorageService {
    
    /**
     * 根据文件大小和类型选择存储策略
     */
    public StorageStrategy selectStrategy(FileMetadata file) {
        // 小文件 (< 10MB): 本地存储
        if (file.getSize() < 10 * 1024 * 1024) {
            return localStorageStrategy;
        }
        
        // 大文件: MinIO 对象存储
        return minioStorageStrategy;
    }
    
    /**
     * 生成存储路径
     */
    public String generatePath(String fileId, String extension) {
        LocalDate now = LocalDate.now();
        return String.format("/%d/%02d/%02d/%s.%s",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
            fileId, extension);
    }
}
```

## 秒传实现

```java
@Service
public class FastUploadService {
    
    @Autowired
    private FileRecordRepository fileRepository;
    
    /**
     * 检查文件是否已存在（秒传）
     */
    public FastUploadCheckResult check(String fileHash, long fileSize) {
        // 1. 查询数据库
        Optional<FileRecord> existing = fileRepository
            .findByFileHashAndFileSize(fileHash, fileSize);
        
        if (existing.isPresent()) {
            FileRecord file = existing.get();
            // 2. 验证文件实际存在
            if (storageService.exists(file.getStoragePath())) {
                // 3. 创建引用（软链接）
                String newFileId = generateFileId();
                createFileReference(file.getId(), newFileId);
                
                return FastUploadCheckResult.builder()
                    .exists(true)
                    .fileId(newFileId)
                    .message("文件已存在，秒传成功")
                    .build();
            }
        }
        
        return FastUploadCheckResult.builder()
            .exists(false)
            .uploadId(createUploadSession(fileHash, fileSize))
            .build();
    }
}
```

## API 端点汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/files/upload` | 单/多文件上传 |
| POST | `/api/files/upload/init` | 初始化分片上传 |
| POST | `/api/files/upload/chunk/{uploadId}/{chunkNum}` | 上传分片 |
| POST | `/api/files/upload/complete/{uploadId}` | 完成分片上传 |
| POST | `/api/files/upload/check` | 秒传检查 |
| GET | `/api/files/download/{fileId}` | 文件下载（支持断点续传）|
| POST | `/api/files/download/batch` | 批量打包下载 |
| GET | `/api/files/preview/{fileId}` | 文件预览 |

## 安全考虑

1. **文件类型白名单**: 只允许特定 MIME 类型
2. **文件大小限制**: 单文件最大 2GB
3. **病毒扫描**: 上传后自动扫描
4. **文件名清理**: 防止路径遍历攻击
5. **访问控制**: 基于用户权限的下载控制

## 验收标准

- [ ] 支持单文件上传（最大 2GB）
- [ ] 支持多文件批量上传
- [ ] 支持分片上传和断点续传
- [ ] 支持文件秒传功能
- [ ] 支持断点下载（Range 请求）
- [ ] 支持批量打包下载
- [ ] 上传文件自动病毒扫描
- [ ] 上传进度实时通知（WebSocket）
