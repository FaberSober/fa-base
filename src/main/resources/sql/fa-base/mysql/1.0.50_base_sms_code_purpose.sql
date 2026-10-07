-- ------------------------- info -------------------------
-- @@ver: 1_000_050
-- @@info: 为短信验证码增加用途隔离
-- ------------------------- info -------------------------

ALTER TABLE `base_sms_code`
    ADD COLUMN `purpose` varchar(20) NOT NULL DEFAULT 'GENERAL' COMMENT '验证码用途' AFTER `code`,
    ADD KEY `idx_base_sms_code_phone_purpose_time` (`phone`, `purpose`, `crt_time`) USING BTREE;
