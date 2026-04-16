-- 支付功能增强：退款记录表和订单表字段补充

-- 退款记录表
CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    refund_no VARCHAR(64) NOT NULL UNIQUE COMMENT '退款单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    refund_amount DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
    reason VARCHAR(255) COMMENT '退款原因',
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '退款状态：PROCESSING, SUCCESS, FAILED',
    error_message VARCHAR(500) COMMENT '错误信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) COMMENT='退款记录表';

-- 添加订单表字段
ALTER TABLE payment_order
    ADD COLUMN IF NOT EXISTS plan_id BIGINT COMMENT '套餐ID',
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(16) COMMENT '计费周期：MONTHLY/YEARLY',
    ADD COLUMN IF NOT EXISTS expire_at TIMESTAMP COMMENT '订单过期时间',
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(500) COMMENT '错误信息',
    ADD INDEX IF NOT EXISTS idx_plan_id (plan_id),
    ADD INDEX IF NOT EXISTS idx_expire_at (expire_at);

-- 添加用户订阅表字段
ALTER TABLE user_subscription
    ADD COLUMN IF NOT EXISTS order_id BIGINT COMMENT '关联订单ID',
    ADD INDEX IF NOT EXISTS idx_order_id (order_id);

-- 更新已存在的订单状态（将已支付但未关联套餐的订单设为已关闭，如果超过30分钟）
UPDATE payment_order
SET status = 'CLOSED',
    error_message = '订单超时自动关闭'
WHERE status = 'PENDING'
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE);
