-- ------------------------- info -------------------------
-- @@ver: 1_000_036
-- @@info: 统一 Telemetry 租户ID字段类型，增加租户权限范围和查询索引
-- ------------------------- info -------------------------

ALTER TABLE `base_client_error_event`
    MODIFY COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID';

ALTER TABLE `base_stat_event`
    MODIFY COLUMN `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID';

CREATE TABLE IF NOT EXISTS `tn_tenant_permission` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` varchar(32) NOT NULL COMMENT '租户ID',
  `menu_id` int(11) NOT NULL COMMENT '权限ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tn_tenant_permission` (`tenant_id`, `menu_id`) USING BTREE,
  KEY `idx_tn_tenant_permission_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户权限范围';

-- 租户私有/租户范围查询索引
ALTER TABLE `base_department`
    ADD KEY `idx_base_department_tenant_id` (`tenant_id`) USING BTREE;

ALTER TABLE `base_rbac_role`
    ADD KEY `idx_base_rbac_role_tenant_id` (`tenant_id`) USING BTREE;
