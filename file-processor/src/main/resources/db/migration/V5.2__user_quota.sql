-- 用户配额表
CREATE TABLE IF NOT EXISTS user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    total_storage_quota BIGINT NOT NULL DEFAULT 10737418240 COMMENT '总存储配额（字节），默认10GB',
    used_storage_quota BIGINT NOT NULL DEFAULT 0 COMMENT '已使用存储空间（字节）',
    daily_upload_limit BIGINT NOT NULL DEFAULT 1073741824 COMMENT '每日上传限制（字节），默认1GB',
    daily_upload_used BIGINT NOT NULL DEFAULT 0 COMMENT '今日已上传（字节）',
    daily_reset_at TIMESTAMP NULL COMMENT '每日上传限制最后重置时间',
    max_file_count INT NOT NULL DEFAULT 10000 COMMENT '最大文件数限制',
    current_file_count INT NOT NULL DEFAULT 0 COMMENT '当前文件数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户存储配额表';
