# Phase 9: 商用级特性

## 目标
实现用户认证授权、API限流、审计日志、文件版本、病毒扫描等企业级功能。

## 功能清单

### 1. 用户认证与授权

**JWT Token 认证**
```
POST /api/auth/login

请求:
{
  "username": "admin",
  "password": "password123"
}

响应:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "admin",
      "roles": ["ADMIN", "USER"]
    }
  }
}
```

```
POST /api/auth/refresh

请求:
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**RBAC 权限模型**
```
用户 -> 角色 -> 权限 -> 资源

角色:
- SUPER_ADMIN: 超级管理员
- ADMIN: 管理员
- USER: 普通用户
- GUEST: 访客

权限:
- file:read    读取文件
- file:write   写入文件
- file:delete  删除文件
- file:share   分享文件
- admin:access 管理权限
```

### 2. API 限流与配额

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = getCurrentUserId();
        String apiKey = request.getHeader("X-API-Key");
        
        // 基于用户的限流
        String key = "rate_limit:user:" + userId + ":" + request.getRequestURI();
        
        Long current = redisTemplate.opsForValue().increment(key);
        if (current == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        
        // 检查配额
        RateLimitConfig config = getRateLimitConfig(userId);
        if (current > config.getRequestsPerMinute()) {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**配额管理**
```
GET /api/user/quota

响应:
{
  "success": true,
  "data": {
    "storage": {
      "total": 10737418240,    // 10GB
      "used": 2147483648,      // 2GB
      "available": 8589934592  // 8GB
    },
    "api": {
      "requestsPerMinute": 100,
      "requestsPerDay": 10000,
      "currentMinute": 45,
      "currentDay": 2340
    },
    "processing": {
      "concurrentTasks": 5,
      "maxFileSize": 2147483648  // 2GB
    }
  }
}
```

### 3. 操作审计日志

```java
@Aspect
@Component
public class AuditLogAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditLog log = new AuditLog();
        log.setAction(auditable.action());
        log.setResourceType(auditable.resourceType());
        log.setUserId(getCurrentUserId());
        log.setIpAddress(getClientIp());
        log.setUserAgent(getUserAgent());
        log.setTimestamp(LocalDateTime.now());
        
        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(log);
        }
    }
}

// 使用示例
@RestController
public class FileController {
    
    @PostMapping("/api/files/delete/{fileId}")
    @Auditable(action = "DELETE", resourceType = "FILE")
    public ResponseEntity<FileResponse> deleteFile(@PathVariable String fileId) {
        // 删除逻辑
    }
}
```

**审计日志查询**
```
GET /api/admin/audit-logs?page=1&size=50&userId=123&action=DELETE

响应:
{
  "success": true,
  "data": {
    "total": 156,
    "logs": [
      {
        "id": 1,
        "action": "DELETE",
        "resourceType": "FILE",
        "resourceId": "f-xxx",
        "userId": 123,
        "username": "admin",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "status": "SUCCESS",
        "timestamp": "2024-04-11T10:30:00Z",
        "details": {"fileName": "document.pdf", "fileSize": 1024000}
      }
    ]
  }
}
```

### 4. 文件分享

```
POST /api/files/{fileId}/share

请求:
{
  "expireDays": 7,           // 0 表示永久
  "password": "123456",      // 可选
  "maxDownloads": 10,        // 0 表示无限制
  "allowPreview": true
}

响应:
{
  "success": true,
  "data": {
    "shareId": "s-xxx-xxx",
    "shareUrl": "https://filemaster.pro/s/s-xxx-xxx",
    "shortUrl": "https://fm.pro/x/Ab3dE",
    "expireAt": "2024-04-18T10:30:00Z",
    "passwordProtected": true
  }
}
```

```
GET /api/share/{shareId}

响应:
{
  "success": true,
  "data": {
    "fileName": "document.pdf",
    "fileSize": 1024000,
    "createdAt": "2024-04-11T10:00:00Z",
    "expireAt": "2024-04-18T10:00:00Z",
    "downloads": 3,
    "maxDownloads": 10,
    "allowPreview": true,
    "passwordRequired": true
  }
}
```

### 5. 病毒扫描

```java
@Service
public class VirusScanService {
    
    @Autowired
    private ClamAVClient clamAVClient;
    
    /**
     * 扫描文件
     */
    public VirusScanResult scan(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            byte[] result = clamAVClient.scan(fileData);
            
            if (ClamAVClient.isCleanReply(result)) {
                return VirusScanResult.clean();
            } else {
                String virusName = new String(result);
                return VirusScanResult.infected(virusName);
            }
        } catch (IOException e) {
            return VirusScanResult.error(e.getMessage());
        }
    }
    
    /**
     * 异步扫描（大文件）
     */
    @Async
    public CompletableFuture<VirusScanResult> scanAsync(String filePath) {
        return CompletableFuture.completedFuture(scan(filePath));
    }
}

// 上传时自动扫描
@Service
public class FileUploadService {
    
    @Autowired
    private VirusScanService virusScanService;
    
    public UploadResult upload(MultipartFile file) {
        // 1. 保存临时文件
        String tempPath = saveTempFile(file);
        
        // 2. 病毒扫描
        VirusScanResult scanResult = virusScanService.scan(tempPath);
        if (!scanResult.isClean()) {
            deleteTempFile(tempPath);
            throw new VirusDetectedException(scanResult.getVirusName());
        }
        
        // 3. 保存到存储
        return saveToStorage(tempPath);
    }
}
```

### 6. 数据加密

```java
@Service
public class EncryptionService {
    
    @Value("${encryption.master-key}")
    private String masterKey;
    
    /**
     * 加密文件（AES-256）
     */
    public void encryptFile(String sourcePath, String targetPath) throws Exception {
        SecretKey key = generateKey();
        byte[] iv = generateIV();
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        try (FileInputStream fis = new FileInputStream(sourcePath);
             FileOutputStream fos = new FileOutputStream(targetPath);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
            
            // 写入 IV
            fos.write(iv);
            
            // 加密内容
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
        }
        
        // 加密并存储密钥
        String encryptedKey = encryptKey(key);
        saveKeyMetadata(targetPath, encryptedKey);
    }
    
    /**
     * 解密文件
     */
    public void decryptFile(String sourcePath, String targetPath) throws Exception {
        // 读取 IV
        byte[] iv = new byte[12];
        try (FileInputStream fis = new FileInputStream(sourcePath)) {
            fis.read(iv);
        }
        
        // 获取解密密钥
        SecretKey key = decryptKey(getKeyMetadata(sourcePath));
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        try (FileInputStream fis = new FileInputStream(sourcePath);
             CipherInputStream cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream(targetPath)) {
            
            // 跳过 IV
            fis.skip(12);
            
            byte[] buffer = new byte[8192];
            int read;
            while ((read = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
    }
}
```

### 7. 回收站功能

```sql
-- 添加删除标记字段
ALTER TABLE file_record ADD COLUMN (
    is_deleted TINYINT(1) DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    original_path VARCHAR(1000) NULL
);

-- 查询未删除文件
SELECT * FROM file_record WHERE is_deleted = 0;

-- 查询回收站
SELECT * FROM file_record WHERE is_deleted = 1;

-- 定时清理（30天后永久删除）
DELETE FROM file_record 
WHERE is_deleted = 1 AND deleted_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

```
POST /api/files/{fileId}/restore  // 恢复
DELETE /api/files/{fileId}/permanent // 永久删除
GET /api/files/trash              // 回收站列表
```

## 数据库设计

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    status TINYINT DEFAULT 1 COMMENT '0-禁用, 1-启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_username (username)
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL
);

-- 角色权限关联
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

-- 审计日志表
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    details JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
);

-- 文件分享表
CREATE TABLE file_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_id VARCHAR(64) UNIQUE NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    created_by BIGINT NOT NULL,
    share_token VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    expire_at TIMESTAMP NULL,
    max_downloads INT DEFAULT 0,
    download_count INT DEFAULT 0,
    allow_preview TINYINT(1) DEFAULT 1,
    status TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_share_id (share_id),
    INDEX idx_file_id (file_id)
);

-- 用户配额表
CREATE TABLE user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    storage_total BIGINT DEFAULT 10737418240,  -- 10GB
    storage_used BIGINT DEFAULT 0,
    requests_per_minute INT DEFAULT 100,
    requests_per_day INT DEFAULT 10000,
    max_file_size BIGINT DEFAULT 2147483648,    -- 2GB
    concurrent_tasks INT DEFAULT 5,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## API 端点汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 用户登出 |
| POST | `/api/auth/refresh` | 刷新 Token |
| GET | `/api/user/profile` | 获取用户信息 |
| GET | `/api/user/quota` | 获取配额信息 |
| POST | `/api/files/{fileId}/share` | 创建分享 |
| GET | `/api/share/{shareId}` | 获取分享信息 |
| POST | `/api/share/{shareId}/verify` | 验证分享密码 |
| GET | `/api/admin/audit-logs` | 审计日志查询 |
| GET | `/api/files/trash` | 回收站列表 |
| POST | `/api/files/{fileId}/restore` | 恢复文件 |

## 验收标准

- [ ] JWT Token 认证（Access + Refresh）
- [ ] RBAC 权限控制
- [ ] API 限流（基于用户/API Key）
- [ ] 用户配额管理
- [ ] 操作审计日志
- [ ] 文件分享（带密码/过期时间）
- [ ] 病毒扫描（ClamAV）
- [ ] 文件加密存储（AES-256）
- [ ] 回收站功能
