-- Phase 5: File Transfer Enhancement
-- 文件传输功能完善：分片上传、秒传、版本管理

-- ============================================
-- 1. 文件上传会话表（用于分片上传）
-- ============================================
CREATE TABLE IF NOT EXISTS file_upload_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upload_id VARCHAR(64) NOT NULL COMMENT '上传会话唯一标识',
    file_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件总大小(字节)',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件SHA-256哈希（用于秒传）',
    chunk_size INT NOT NULL DEFAULT 5242880 COMMENT '分片大小(字节)，默认5MB',
    total_chunks INT NOT NULL COMMENT '总分片数',
    uploaded_chunks JSON COMMENT '已上传的分片索引列表',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/UPLOADING/COMPLETED/EXPIRED',
    storage_path VARCHAR(1000) COMMENT '最终存储路径',
    target_file_id VARCHAR(64) COMMENT '关联的文件ID（完成后填充）',
    expire_at TIMESTAMP NOT NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_upload_id (upload_id),
    INDEX idx_file_hash (file_hash),
    INDEX idx_status (status),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传会话表';

-- ============================================
-- 2. 文件版本表
-- ============================================
CREATE TABLE IF NOT EXISTS file_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id VARCHAR(64) NOT NULL COMMENT '版本唯一标识',
    file_id VARCHAR(64) NOT NULL COMMENT '关联的文件ID',
    version_number INT NOT NULL COMMENT '版本号',
    storage_path VARCHAR(1000) NOT NULL COMMENT '版本存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件哈希',
    description VARCHAR(500) COMMENT '版本描述',
    tags JSON COMMENT '标签',
    created_by VARCHAR(64) COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_version_id (version_id),
    INDEX idx_file_id (file_id),
    INDEX idx_version_number (file_id, version_number),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件版本表';

-- ============================================
-- 3. 文件分享表
-- ============================================
CREATE TABLE IF NOT EXISTS file_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    share_id VARCHAR(64) NOT NULL COMMENT '分享唯一标识',
    file_id VARCHAR(64) NOT NULL COMMENT '分享的文件ID',
    created_by VARCHAR(64) NOT NULL COMMENT '创建者',
    share_token VARCHAR(255) NOT NULL COMMENT '分享令牌',
    password_hash VARCHAR(255) COMMENT '访问密码哈希',
    expire_at TIMESTAMP NULL COMMENT '过期时间',
    max_downloads INT DEFAULT 0 COMMENT '最大下载次数(0=无限制)',
    download_count INT DEFAULT 0 COMMENT '已下载次数',
    allow_preview TINYINT(1) DEFAULT 1 COMMENT '是否允许预览',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 0-失效, 1-有效',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_share_id (share_id),
    INDEX idx_file_id (file_id),
    INDEX idx_created_by (created_by),
    INDEX idx_expire_at (expire_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- ============================================
-- 4. 修改文件元数据表，添加更多字段
-- ============================================
ALTER TABLE file_metadata
    -- 添加文件哈希（用于秒传）
    ADD COLUMN IF NOT EXISTS file_hash VARCHAR(128) COMMENT '文件SHA-256哈希' AFTER sha256_hash,
    ADD COLUMN IF NOT EXISTS hash_algorithm VARCHAR(20) DEFAULT 'SHA-256' COMMENT '哈希算法',

    -- 存储信息
    ADD COLUMN IF NOT EXISTS storage_bucket VARCHAR(100) COMMENT '存储桶名' AFTER storage_path,
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(1000) COMMENT '对象存储Key' AFTER storage_bucket,

    -- 访问统计
    ADD COLUMN IF NOT EXISTS access_count INT DEFAULT 0 COMMENT '访问次数' AFTER reference_count,
    ADD COLUMN IF NOT EXISTS last_access_at TIMESTAMP NULL COMMENT '最后访问时间' AFTER access_count,

    -- 删除标记（软删除）
    ADD COLUMN IF NOT EXISTS is_deleted TINYINT(1) DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER status,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL COMMENT '删除时间' AFTER is_deleted,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(64) COMMENT '删除者' AFTER deleted_at,

    -- 索引优化
    ADD INDEX IF NOT EXISTS idx_file_hash (file_hash),
    ADD INDEX IF NOT EXISTS idx_is_deleted (is_deleted);

-- ============================================
-- 5. 用户配额表
-- ============================================
CREATE TABLE IF NOT EXISTS user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    storage_total BIGINT DEFAULT 10737418240 COMMENT '总存储配额(字节)，默认10GB',
    storage_used BIGINT DEFAULT 0 COMMENT '已用存储(字节)',
    requests_per_minute INT DEFAULT 100 COMMENT '每分钟请求限制',
    requests_per_day INT DEFAULT 10000 COMMENT '每天请求限制',
    max_file_size BIGINT DEFAULT 2147483648 COMMENT '单文件大小限制(字节)，默认2GB',
    concurrent_tasks INT DEFAULT 5 COMMENT '并发任务数限制',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_storage_used (storage_used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配额表';

-- ============================================
-- 6. 审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(50) NOT NULL COMMENT '操作类型: UPLOAD/DOWNLOAD/DELETE/SHARE',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型: FILE/TASK/USER',
    resource_id VARCHAR(64) COMMENT '资源ID',
    user_id VARCHAR(64) COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    request_method VARCHAR(10) COMMENT 'HTTP方法',
    request_url VARCHAR(1000) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    status VARCHAR(20) NOT NULL COMMENT '状态: SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    execution_time_ms INT COMMENT '执行时间(毫秒)',
    details JSON COMMENT '详细信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_action (action),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 7. 创建文件回收站视图
-- ============================================
CREATE OR REPLACE VIEW file_recycle_bin AS
SELECT
    file_id,
    original_name,
    file_size,
    storage_path,
    mime_type,
    deleted_at,
    deleted_by
FROM file_metadata
WHERE is_deleted = 1
ORDER BY deleted_at DESC;
