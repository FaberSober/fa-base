-- ------------------------- info -------------------------
-- @@ver: 1_000_043
-- @@info: 扩展多客户端设备登记
-- ------------------------- info -------------------------

ALTER TABLE `base_user_device`
    ADD COLUMN `client_type` varchar(16) DEFAULT NULL COMMENT '客户端类型' AFTER `device_id`;

ALTER TABLE `base_log_login`
    ADD COLUMN `client_type` varchar(16) DEFAULT NULL COMMENT '客户端类型' AFTER `agent`,
    ADD COLUMN `device_id` varchar(128) DEFAULT NULL COMMENT '客户端实例ID' AFTER `client_type`;

CREATE INDEX `idx_base_user_device_client`
    ON `base_user_device` (`user_id`, `client_type`, `device_id`(128));
