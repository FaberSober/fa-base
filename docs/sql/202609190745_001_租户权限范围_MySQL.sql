-- 开发阶段手动更新 SQL（MySQL）
-- 适用：已执行 1.0.36，但数据库中还没有 tn_tenant_permission 的开发环境。
-- 手动执行一次即可；新建数据库直接执行合并后的 1.0.36 脚本即可。

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
