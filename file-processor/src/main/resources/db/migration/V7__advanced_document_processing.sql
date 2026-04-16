-- 文件版本表
CREATE TABLE IF NOT EXISTS file_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id VARCHAR(64) UNIQUE NOT NULL COMMENT '版本ID',
    file_id VARCHAR(64) NOT NULL COMMENT '文件ID',
    version_number INT NOT NULL COMMENT '版本号',
    storage_path VARCHAR(1000) NOT NULL COMMENT '存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件哈希',
    description VARCHAR(500) COMMENT '版本描述',
    tags JSON COMMENT '标签',
    created_by BIGINT COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (file_id) REFERENCES file_metadata(file_id),
    INDEX idx_file_id (file_id),
    INDEX idx_version_number (file_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件版本表';

-- 文件分割记录表
CREATE TABLE IF NOT EXISTS file_split_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    split_id VARCHAR(64) UNIQUE NOT NULL COMMENT '分割任务ID',
    original_file_id VARCHAR(64) NOT NULL COMMENT '原文件ID',
    original_file_name VARCHAR(500) NOT NULL COMMENT '原文件名',
    original_size BIGINT NOT NULL COMMENT '原文件大小',
    original_hash VARCHAR(128) NOT NULL COMMENT '原文件哈希',
    chunk_size BIGINT NOT NULL COMMENT '分块大小',
    total_chunks INT NOT NULL COMMENT '总分块数',
    output_dir VARCHAR(1000) NOT NULL COMMENT '输出目录',
    manifest_path VARCHAR(1000) NOT NULL COMMENT '清单文件路径',
    status VARCHAR(20) NOT NULL COMMENT '状态: PENDING, PROCESSING, COMPLETED, FAILED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    INDEX idx_split_id (split_id),
    INDEX idx_original_file (original_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分割记录表';
