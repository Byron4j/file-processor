-- =============================================
-- FileMaster Pro Database Schema
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `phone` VARCHAR(20),
    `avatar` VARCHAR(255),
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `role` VARCHAR(20) DEFAULT 'USER',
    `plan_id` BIGINT,
    `plan_expire_time` DATETIME,
    `storage_used` BIGINT DEFAULT 0,
    `storage_quota` BIGINT DEFAULT 1073741824, -- 1GB default
    `last_login_time` DATETIME,
    `last_login_ip` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 订阅套餐表
CREATE TABLE IF NOT EXISTS `subscription_plan` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL,
    `description` TEXT,
    `monthly_price` DECIMAL(10, 2) NOT NULL,
    `yearly_price` DECIMAL(10, 2) NOT NULL,
    `storage_quota` BIGINT NOT NULL, -- in GB
    `max_file_size` INT NOT NULL, -- in MB
    `max_tasks_per_day` INT DEFAULT 100,
    `max_team_members` INT DEFAULT 1,
    `ai_features_enabled` BOOLEAN DEFAULT FALSE,
    `advanced_pdf_enabled` BOOLEAN DEFAULT FALSE,
    `priority_support` BOOLEAN DEFAULT FALSE,
    `active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户订阅表
CREATE TABLE IF NOT EXISTS `user_subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `plan_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `billing_cycle` VARCHAR(20) NOT NULL, -- MONTHLY, YEARLY
    `current_period_start` DATE NOT NULL,
    `current_period_end` DATE NOT NULL,
    `next_billing_date` DATE,
    `current_price` DECIMAL(10, 2) NOT NULL,
    `payment_method` VARCHAR(20),
    `provider_subscription_id` VARCHAR(255),
    `cancelled_at` DATETIME,
    `cancellation_reason` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 支付订单表
CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `subscription_id` BIGINT,
    `order_no` VARCHAR(64) NOT NULL UNIQUE,
    `order_type` VARCHAR(20) NOT NULL, -- SUBSCRIPTION, UPGRADE
    `amount` DECIMAL(10, 2) NOT NULL,
    `currency` VARCHAR(10) DEFAULT 'CNY',
    `payment_method` VARCHAR(20),
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `paid_at` DATETIME,
    `provider_transaction_id` VARCHAR(255),
    `error_message` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文件指纹表 (秒传)
CREATE TABLE IF NOT EXISTS `file_fingerprint` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `md5_hash` VARCHAR(32) NOT NULL,
    `sha256_hash` VARCHAR(64),
    `file_size` BIGINT NOT NULL,
    `storage_path` VARCHAR(500) NOT NULL,
    `reference_count` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_accessed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_md5_size` (`md5_hash`, `file_size`),
    INDEX `idx_sha256` (`sha256_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文件元数据表
CREATE TABLE IF NOT EXISTS `file_metadata` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `file_type` VARCHAR(50),
    `file_size` BIGINT NOT NULL,
    `storage_path` VARCHAR(500) NOT NULL,
    `fingerprint_id` BIGINT,
    `md5_hash` VARCHAR(32),
    `folder_id` BIGINT,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `deleted_at` DATETIME,
    `tags` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_folder_id` (`folder_id`),
    INDEX `idx_fingerprint_id` (`fingerprint_id`),
    INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文件夹表
CREATE TABLE IF NOT EXISTS `folder` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,
    `name` VARCHAR(255) NOT NULL,
    `path` VARCHAR(500) NOT NULL,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `deleted_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 团队表
CREATE TABLE IF NOT EXISTS `team` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `owner_id` BIGINT NOT NULL,
    `invite_code` VARCHAR(20) UNIQUE,
    `member_count` INT DEFAULT 1,
    `storage_quota` BIGINT DEFAULT 107374182400, -- 100GB
    `storage_used` BIGINT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'ACTIVE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 团队成员表
CREATE TABLE IF NOT EXISTS `team_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` VARCHAR(20) DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `username` VARCHAR(50),
    `action` VARCHAR(100) NOT NULL,
    `resource_type` VARCHAR(50),
    `resource_id` VARCHAR(100),
    `ip_address` VARCHAR(50),
    `user_agent` VARCHAR(500),
    `request_method` VARCHAR(10),
    `request_path` VARCHAR(255),
    `request_params` TEXT,
    `status_code` INT,
    `error_message` TEXT,
    `execution_time` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action` (`action`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 异步任务表
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `task_type` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED
    `progress` INT DEFAULT 0,
    `input_params` JSON,
    `result` JSON,
    `error_message` TEXT,
    `started_at` DATETIME,
    `completed_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文件分享表
CREATE TABLE IF NOT EXISTS `file_share` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `file_id` BIGINT NOT NULL,
    `share_code` VARCHAR(32) NOT NULL UNIQUE,
    `access_password` VARCHAR(255),
    `expire_at` DATETIME,
    `allow_download` BOOLEAN DEFAULT TRUE,
    `access_count` INT DEFAULT 0,
    `last_accessed_at` DATETIME,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_share_code` (`share_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
