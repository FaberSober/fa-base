-- ------------------------- info -------------------------
-- @@ver: 1_000_038
-- @@info: 增加平台必选租户权限标记
-- ------------------------- info -------------------------

ALTER TABLE `base_rbac_menu`
    ADD COLUMN `tenant_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT '租户必选权限' AFTER `scope`;
