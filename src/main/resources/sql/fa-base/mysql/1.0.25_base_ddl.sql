-- ------------------------- info -------------------------
-- @@ver: 1_000_025
-- @@info: 基础表增加租户字段，角色表增加类型字段，增加智能人事和租户菜单
-- ------------------------- info -------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tn_tenant
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tn_tenant` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `code` varchar(64) NOT NULL COMMENT '租户编码',
  `name` varchar(255) NOT NULL COMMENT '租户名称',
  `short_name` varchar(255) DEFAULT NULL COMMENT '租户简称',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用/0-禁用',
  `expire_time` datetime DEFAULT NULL COMMENT '到期时间',
  `contact_name` varchar(255) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
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
  UNIQUE KEY `uk_tn_tenant_code` (`code`) USING BTREE,
  KEY `idx_tn_tenant_name` (`name`) USING BTREE,
  KEY `idx_tn_tenant_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- ----------------------------
-- Table structure for tn_tenant_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tn_tenant_user` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `tenant_id` varchar(32) NOT NULL COMMENT '租户ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `is_admin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否租户管理员：1-是/0-否',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-启用/0-禁用',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
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
  UNIQUE KEY `uk_tn_tenant_user` (`tenant_id`, `user_id`) USING BTREE,
  KEY `idx_tn_tenant_user_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_tn_tenant_user_user_id` (`user_id`) USING BTREE,
  KEY `idx_tn_tenant_user_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户用户关联表';

-- 部门表增加租户ID
ALTER TABLE `base_department` ADD COLUMN `tenant_id` varchar(32) NULL COMMENT '租户ID' AFTER `manager_id`;

-- 角色表增加类型和租户ID
ALTER TABLE `base_rbac_role` ADD COLUMN `type` int(11) NULL COMMENT '类型：1全局超管/2全局/3租户' AFTER `status`;
ALTER TABLE `base_rbac_role` ADD COLUMN `tenant_id` varchar(32) NULL COMMENT '租户ID' AFTER `type`;

-- 兼容历史角色数据：超管为全局超管，其余未绑定租户角色为全局角色
UPDATE `base_rbac_role` SET `type` = CASE WHEN `id` = 1 THEN 1 WHEN `tenant_id` IS NULL THEN 2 ELSE 3 END WHERE `type` IS NULL;

-- 租户相关菜单
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (21030040, 12000000, 1, '租户管理', 4, 1, 'mdi:office-building-cog', 1, 1, '/admin/system/tn', '2026-04-23 14:33:56', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:33:56', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (21030041, 21030040, 1, '租户管理', 0, 1, 'mdi:domain', 1, 1, '/admin/system/tn/tenant', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (21030042, 21030040, 1, '租户用户管理', 1, 1, 'mdi:account-cog-outline', 1, 1, '/admin/system/tn/tenantUser', '2026-04-23 14:35:20', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:35:20', NULL, NULL, NULL, 0);

-- 智能人事相关菜单
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (12010200, 12010000, 1, '部门管理', 1, 1, 'mdi:account-group-outline', 1, 1, '/admin/system/hr/department', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (12010400, 12010000, 1, '超级用户管理', 3, 1, 'mdi:account-supervisor-circle-outline', 1, 1, '/admin/system/hr/userSuper', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
