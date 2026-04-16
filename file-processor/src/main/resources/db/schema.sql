-- Phase 3 Database Schema
-- File Metadata Table
CREATE TABLE IF NOT EXISTS file_metadata (
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

-- Task Record Table
CREATE TABLE IF NOT EXISTS task_record (
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

-- File Category Table
CREATE TABLE IF NOT EXISTS file_category (
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

-- Initialize category data
INSERT INTO file_category (name, code, description, sort_order) VALUES
('文档', 'DOCUMENT', 'Word、PDF、TXT 等文档', 1),
('表格', 'SPREADSHEET', 'Excel、CSV 等表格', 2),
('演示文稿', 'PRESENTATION', 'PPT、Keynote 等', 3),
('图片', 'IMAGE', 'JPG、PNG、GIF 等', 4),
('压缩包', 'ARCHIVE', 'ZIP、RAR、7z 等', 5),
('音视频', 'MEDIA', 'MP4、MP3 等', 6),
('其他', 'OTHER', '其他类型文件', 99)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);
