-- ------------------------- info -------------------------
-- @@ver: 1_000_051
-- @@info: 为短信登录验证码增加消费状态和失败次数
-- ------------------------- info -------------------------

ALTER TABLE `base_sms_code`
    ADD COLUMN `consumed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '验证码是否已消费' AFTER `purpose`,
    ADD COLUMN `failed_attempts` int NOT NULL DEFAULT 0 COMMENT '验证码失败次数' AFTER `consumed`;
