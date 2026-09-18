-- ------------------------- info -------------------------
-- @@ver: 1_000_036
-- @@info: 统一 Telemetry 租户ID字段类型
-- ------------------------- info -------------------------

ALTER TABLE `base_client_error_event`
    MODIFY COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID';

ALTER TABLE `base_stat_event`
    MODIFY COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID';
