-- ------------------------- info -------------------------
-- @@ver: 1_000_044
-- @@info: 租户图标
-- ------------------------- info -------------------------

ALTER TABLE `tn_tenant`
    ADD COLUMN `icon` varchar(512) DEFAULT NULL COMMENT '租户图标文件ID' AFTER `short_name`;
