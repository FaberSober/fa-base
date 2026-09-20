-- 开发阶段手动更新 SQL（MySQL）
-- 适用：已执行 1.0.37，但数据库中尚未增加 base_rbac_menu.tenant_required 的开发环境。
-- 新建数据库或已执行 1.0.38 的环境无需重复执行。

SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'base_rbac_menu'
      AND column_name = 'tenant_required'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `base_rbac_menu` ADD COLUMN `tenant_required` tinyint(1) NOT NULL DEFAULT ''0'' COMMENT ''租户必选权限'' AFTER `scope`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
