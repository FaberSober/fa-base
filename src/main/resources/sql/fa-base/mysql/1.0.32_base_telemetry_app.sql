-- ------------------------- info -------------------------
-- @@ver: 1_000_032
-- @@info: 增加 Telemetry 应用表
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_telemetry_app` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_key` varchar(64) NOT NULL COMMENT '客户端上报标识',
  `app_code` varchar(64) NOT NULL COMMENT '应用编码',
  `app_name` varchar(128) NOT NULL COMMENT '应用名称',
  `client_type` varchar(16) NOT NULL COMMENT '客户端类型',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许上报',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
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
  UNIQUE KEY `uk_base_telemetry_app_key` (`app_key`) USING BTREE,
  UNIQUE KEY `uk_base_telemetry_app_code` (`app_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telemetry 应用';
