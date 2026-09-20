-- ------------------------- info -------------------------
-- @@ver: 1_000_037
-- @@info: 增加菜单跨环境同步标识并初始化历史菜单
-- ------------------------- info -------------------------

ALTER TABLE `base_rbac_menu`
    ADD COLUMN `config_key` varchar(64) DEFAULT NULL COMMENT '跨环境菜单配置标识' AFTER `id`;

UPDATE `base_rbac_menu`
SET `config_key` = CONCAT('legacy:', COALESCE(`scope`, 1), ':', `id`)
WHERE `config_key` IS NULL OR `config_key` = '';

ALTER TABLE `base_rbac_menu`
    MODIFY COLUMN `config_key` varchar(64) NOT NULL COMMENT '跨环境菜单配置标识';

ALTER TABLE `base_rbac_menu`
    ADD UNIQUE KEY `uk_base_rbac_menu_config_key` (`config_key`) USING BTREE;
