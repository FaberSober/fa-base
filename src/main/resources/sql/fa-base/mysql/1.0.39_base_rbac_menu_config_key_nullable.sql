-- ------------------------- info -------------------------
-- @@ver: 1_000_039
-- @@info: 允许历史模块菜单 SQL 省略跨环境配置标识
-- ------------------------- info -------------------------

ALTER TABLE `base_rbac_menu`
    MODIFY COLUMN `config_key` varchar(64) DEFAULT NULL COMMENT '跨环境菜单配置标识' AFTER `id`;
