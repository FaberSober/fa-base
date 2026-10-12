-- ------------------------- info -------------------------
-- @@ver: 1_000_049
-- @@info: 为用户设备增加信任状态字段
-- ------------------------- info -------------------------

ALTER TABLE `base_user_device`
    ADD COLUMN `trust_token_hash` char(64) DEFAULT NULL COMMENT '信任令牌哈希',
    ADD COLUMN `trusted_at` timestamp NULL DEFAULT NULL COMMENT '信任建立时间',
    ADD COLUMN `trust_expires_at` timestamp NULL DEFAULT NULL COMMENT '信任到期时间',
    ADD COLUMN `trust_revoked_at` timestamp NULL DEFAULT NULL COMMENT '信任撤销时间';

ALTER TABLE `base_user_device`
    ADD UNIQUE KEY `uk_base_user_device_trust_hash` (`trust_token_hash`) USING BTREE;
