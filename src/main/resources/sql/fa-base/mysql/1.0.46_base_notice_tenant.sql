-- ------------------------- info -------------------------
-- @@ver: 1_000_046
-- @@info: 系统公告租户隔离，历史公告保留空租户
-- ------------------------- info -------------------------

ALTER TABLE `base_notice`
    ADD COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID' AFTER `id`;

CREATE INDEX `idx_base_notice_tenant_id` ON `base_notice` (`tenant_id`);
