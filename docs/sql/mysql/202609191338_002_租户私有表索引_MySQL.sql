-- 开发阶段手动更新 SQL（MySQL）
-- 适用：已执行 1.0.36，但数据库中缺少租户查询索引的开发环境。
-- 手动执行一次即可；新建数据库直接执行合并后的 1.0.36 脚本即可。

SET @index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'base_department'
      AND index_name = 'idx_base_department_tenant_id'
);
SET @sql := IF(
    @index_exists = 0,
    'ALTER TABLE `base_department` ADD KEY `idx_base_department_tenant_id` (`tenant_id`) USING BTREE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'base_rbac_role'
      AND index_name = 'idx_base_rbac_role_tenant_id'
);
SET @sql := IF(
    @index_exists = 0,
    'ALTER TABLE `base_rbac_role` ADD KEY `idx_base_rbac_role_tenant_id` (`tenant_id`) USING BTREE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
