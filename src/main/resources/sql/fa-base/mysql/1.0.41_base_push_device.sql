-- ------------------------- info -------------------------
-- @@ver: 1_000_041
-- @@info: 增加推送设备绑定表
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_push_device` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `provider` varchar(32) NOT NULL COMMENT '推送服务商',
  `client_id` varchar(255) NOT NULL COMMENT 'Push Client ID',
  `app_id` varchar(128) NOT NULL COMMENT '应用ID',
  `platform` varchar(32) NOT NULL COMMENT '平台',
  `environment` varchar(16) NOT NULL COMMENT '推送环境',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `last_seen_time` timestamp NULL DEFAULT NULL COMMENT '最后上报时间',
  `invalid_time` timestamp NULL DEFAULT NULL COMMENT '失效时间',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_base_push_device_binding` (`provider`, `client_id`, `app_id`, `environment`) USING BTREE,
  KEY `idx_base_push_device_user_enabled` (`user_id`, `enabled`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-推送设备';
