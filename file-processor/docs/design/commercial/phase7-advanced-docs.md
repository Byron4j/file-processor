# Phase 7: 高级文档处理

## 目标
实现文档压缩包创建、大文件分割合并、文档版本管理等高级功能。

## 功能清单

### 1. 创建压缩包

```
POST /api/archive/create

请求:
{
  "sourcePaths": [
    "/uploads/doc1.pdf",
    "/uploads/doc2.docx",
    "/uploads/images/"
  ],
  "targetPath": "/outputs/archive.zip",
  "format": "ZIP",           // ZIP, 7z, TAR, TAR.GZ
  "compressionLevel": 6,     // 0-9
  "password": "optional",
  "encryptFileNames": false  // 7z 支持
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/archive.zip",
    "format": "ZIP",
    "totalFiles": 15,
    "originalSize": 52428800,
    "compressedSize": 31457280,
    "compressionRatio": 0.60
  }
}
```

### 2. 大文件分割

```
POST /api/file/split

请求:
{
  "sourcePath": "/uploads/large-file.zip",
  "outputDir": "/outputs/split/",
  "chunkSize": 104857600,    // 每块 100MB
  "namingPattern": "part-{index}.bin"
}

响应:
{
  "success": true,
  "data": {
    "totalChunks": 11,
    "chunkSize": 104857600,
    "chunks": [
      "/outputs/split/part-001.bin",
      "/outputs/split/part-002.bin",
      ...
    ],
    "manifestPath": "/outputs/split/manifest.json"
  }
}
```

Manifest 文件格式:
```json
{
  "originalFileName": "large-file.zip",
  "originalSize": 1153433600,
  "originalHash": "sha256:xxx...",
  "chunkSize": 104857600,
  "totalChunks": 11,
  "chunks": [
    {"index": 1, "fileName": "part-001.bin", "size": 104857600, "hash": "sha256:..."},
    {"index": 2, "fileName": "part-002.bin", "size": 104857600, "hash": "sha256:..."}
  ]
}
```

### 3. 文件合并

```
POST /api/file/merge

请求:
{
  "manifestPath": "/outputs/split/manifest.json",
  "targetPath": "/outputs/merged-file.zip",
  "verifyHash": true
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/merged-file.zip",
    "size": 1153433600,
    "hash": "sha256:xxx...",
    "hashVerified": true
  }
}
```

### 4. 文件版本管理

```
POST /api/files/{fileId}/versions/save

请求:
{
  "description": "第二次修订",
  "tags": ["修订版", "重要"]
}

响应:
{
  "success": true,
  "data": {
    "versionId": "v-xxx-xxx",
    "versionNumber": 2,
    "fileId": "f-xxx-xxx",
    "description": "第二次修订",
    "createdAt": "2024-04-11T10:30:00Z"
  }
}
```

```
GET /api/files/{fileId}/versions

响应:
{
  "success": true,
  "data": {
    "total": 5,
    "versions": [
      {
        "versionId": "v-5",
        "versionNumber": 5,
        "description": "最终版",
        "size": 1024000,
        "createdAt": "2024-04-11T14:00:00Z",
        "isLatest": true
      },
      {
        "versionId": "v-4",
        "versionNumber": 4,
        "description": "第三次修订",
        "size": 1020000,
        "createdAt": "2024-04-11T12:00:00Z"
      }
    ]
  }
}
```

```
POST /api/files/{fileId}/versions/restore

请求:
{
  "versionId": "v-3"
}
```

### 5. 文件比较

```
POST /api/files/compare

请求:
{
  "fileId1": "f-xxx-xxx",
  "fileId2": "f-yyy-yyy",
  "compareMode": "TEXT"  // TEXT, BINARY, STRUCTURE
}

响应 (文本模式):
{
  "success": true,
  "data": {
    "similarity": 0.85,
    "differences": [
      {
        "type": "MODIFIED",
        "lineNumber": 45,
        "oldText": "原内容",
        "newText": "新内容"
      },
      {
        "type": "ADDED",
        "lineNumber": 60,
        "newText": "新增内容"
      }
    ],
    "addedLines": 5,
    "deletedLines": 3,
    "modifiedLines": 12
  }
}
```

### 6. 文件格式修复

```
POST /api/file/repair

请求:
{
  "sourcePath": "/uploads/corrupt.pdf",
  "targetPath": "/outputs/repaired.pdf",
  "fileType": "PDF"
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/repaired.pdf",
    "repairsMade": [
      "Fixed cross-reference table",
      "Repaired 3 damaged pages"
    ],
    "warnings": [
      "Some images may be corrupted"
    ]
  }
}
```

## 数据库设计

```sql
-- 文件版本表
CREATE TABLE file_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id VARCHAR(64) UNIQUE NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    version_number INT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    file_hash VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    tags JSON,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_record(file_id),
    INDEX idx_file_id (file_id),
    INDEX idx_version_number (file_id, version_number)
);

-- 文件分割记录表
CREATE TABLE file_split_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    split_id VARCHAR(64) UNIQUE NOT NULL,
    original_file_id VARCHAR(64) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    original_size BIGINT NOT NULL,
    original_hash VARCHAR(128) NOT NULL,
    chunk_size BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    output_dir VARCHAR(1000) NOT NULL,
    manifest_path VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_split_id (split_id),
    INDEX idx_original_file (original_file_id)
);
```

## 实现代码

```java
@Service
public class ArchiveCreationService {
    
    @Autowired
    private StorageService storageService;
    
    /**
     * 创建 ZIP 压缩包
     */
    public ArchiveResult createZip(CreateArchiveRequest request) {
        Path targetPath = Paths.get(request.getTargetPath());
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(targetPath.toFile()))) {
            
            zos.setLevel(request.getCompressionLevel());
            
            long totalOriginalSize = 0;
            int fileCount = 0;
            
            for (String sourcePath : request.getSourcePaths()) {
                Path path = Paths.get(sourcePath);
                
                if (Files.isDirectory(path)) {
                    // 递归添加目录
                    Files.walk(path).forEach(file -> {
                        if (Files.isRegularFile(file)) {
                            addToZip(zos, file, path, request);
                            totalOriginalSize += Files.size(file);
                            fileCount++;
                        }
                    });
                } else {
                    addToZip(zos, path, path.getParent(), request);
                    totalOriginalSize += Files.size(path);
                    fileCount++;
                }
            }
            
            long compressedSize = Files.size(targetPath);
            
            return ArchiveResult.builder()
                .targetPath(targetPath.toString())
                .totalFiles(fileCount)
                .originalSize(totalOriginalSize)
                .compressedSize(compressedSize)
                .compressionRatio((double) compressedSize / totalOriginalSize)
                .build();
                
        } catch (IOException e) {
            throw new ArchiveException("Failed to create archive", e);
        }
    }
    
    private void addToZip(ZipOutputStream zos, Path file, Path basePath, 
                          CreateArchiveRequest request) {
        String entryName = basePath.relativize(file).toString();
        
        ZipEntry entry = new ZipEntry(entryName);
        
        // 密码保护
        if (request.getPassword() != null) {
            // 使用 AES 加密
            entry.setMethod(ZipEntry.DEFLATED);
        }
        
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }
}
```

```java
@Service
public class FileVersionService {
    
    @Autowired
    private FileVersionRepository versionRepository;
    
    @Autowired
    private StorageService storageService;
    
    /**
     * 保存文件版本
     */
    @Transactional
    public FileVersion saveVersion(String fileId, SaveVersionRequest request) {
        FileRecord file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 获取下一个版本号
        int nextVersion = versionRepository.findMaxVersionByFileId(fileId) + 1;
        
        // 复制文件到版本存储
        String versionStoragePath = generateVersionPath(fileId, nextVersion);
        storageService.copy(file.getStoragePath(), versionStoragePath);
        
        FileVersion version = FileVersion.builder()
            .versionId(generateVersionId())
            .fileId(fileId)
            .versionNumber(nextVersion)
            .storagePath(versionStoragePath)
            .fileSize(file.getFileSize())
            .fileHash(file.getFileHash())
            .description(request.getDescription())
            .tags(request.getTags())
            .build();
        
        return versionRepository.save(version);
    }
    
    /**
     * 恢复到指定版本
     */
    @Transactional
    public void restoreVersion(String fileId, String versionId) {
        FileVersion version = versionRepository.findByVersionId(versionId)
            .orElseThrow(() -> new VersionNotFoundException(versionId));
        
        FileRecord file = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // 先保存当前版本
        saveVersion(fileId, SaveVersionRequest.builder()
            .description("自动保存：恢复前的版本")
            .build());
        
        // 恢复指定版本
        storageService.copy(version.getStoragePath(), file.getStoragePath());
        
        file.setFileSize(version.getFileSize());
        file.setFileHash(version.getFileHash());
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(file);
    }
}
```

## API 端点汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/archive/create` | 创建压缩包 |
| POST | `/api/file/split` | 分割大文件 |
| POST | `/api/file/merge` | 合并文件 |
| POST | `/api/files/{fileId}/versions/save` | 保存版本 |
| GET | `/api/files/{fileId}/versions` | 获取版本列表 |
| POST | `/api/files/{fileId}/versions/restore` | 恢复版本 |
| POST | `/api/files/compare` | 文件比较 |
| POST | `/api/file/repair` | 文件修复 |

## 验收标准

- [ ] 支持创建 ZIP/7z/TAR 压缩包
- [ ] 支持密码保护加密
- [ ] 支持大文件分割（可配置分块大小）
- [ ] 支持文件合并（带 Hash 校验）
- [ ] 支持文件版本管理（保存/恢复/列表）
- [ ] 支持文本文件比较
- [ ] 支持 PDF/Office 文件修复
- [ ] 版本历史保留策略（如保留最近 10 个版本）
