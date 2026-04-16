-- =============================================
-- FileMaster Pro Initial Data
-- =============================================

-- 插入默认订阅套餐
INSERT INTO `subscription_plan` (`code`, `name`, `description`, `monthly_price`, `yearly_price`, `storage_quota`, `max_file_size`, `max_tasks_per_day`, `max_team_members`, `ai_features_enabled`, `advanced_pdf_enabled`, `priority_support`, `active`)
VALUES
    ('FREE', '免费版', '适合个人用户体验基础功能', 0.00, 0.00, 1, 50, 10, 1, FALSE, FALSE, FALSE, TRUE),
    ('PRO', '专业版', '适合个人用户的高效办公', 29.00, 290.00, 100, 500, 100, 1, TRUE, TRUE, FALSE, TRUE),
    ('TEAM', '团队版', '适合小型团队协作', 99.00, 990.00, 500, 1000, 500, 10, TRUE, TRUE, TRUE, TRUE),
    ('ENTERPRISE', '企业版', '适合大型企业的全面解决方案', 299.00, 2990.00, 2000, 2048, 9999, 100, TRUE, TRUE, TRUE, TRUE);

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO `user` (`username`, `password`, `email`, `status`, `role`, `storage_quota`)
VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Ey', 'admin@filemaster.pro', 'ACTIVE', 'ADMIN', 107374182400);

-- 插入测试用户 (密码: test123)
INSERT INTO `user` (`username`, `password`, `email`, `status`, `role`, `storage_quota`)
VALUES
    ('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Ey', 'test@example.com', 'ACTIVE', 'USER', 1073741824);
