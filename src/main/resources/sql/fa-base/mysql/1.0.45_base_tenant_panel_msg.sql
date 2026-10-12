-- ------------------------- info -------------------------
-- @@ver: 1_000_045
-- @@info: 消息租户归属
-- ------------------------- info -------------------------

ALTER TABLE `base_msg`
    ADD COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID' AFTER `to_user_id`;

CREATE INDEX `idx_base_msg_to_tenant_read`
    ON `base_msg` (`to_user_id`, `tenant_id`, `is_read`);
