-- ------------------------- info -------------------------
-- @@ver: 1_000_030
-- @@info: 增加 Telemetry 应用、客户端异常、业务统计事件和每日聚合统计表及菜单
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

CREATE TABLE IF NOT EXISTS `base_client_error_issue` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` bigint unsigned NOT NULL COMMENT 'Telemetry应用ID',
  `client_type` varchar(16) NOT NULL COMMENT '客户端类型',
  `fingerprint` varchar(64) NOT NULL COMMENT '异常指纹',
  `title` varchar(500) NOT NULL COMMENT '异常标题',
  `error_type` varchar(128) NOT NULL COMMENT '异常类型',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT 'Issue状态',
  `first_seen_time` datetime NOT NULL COMMENT '首次出现时间',
  `last_seen_time` datetime NOT NULL COMMENT '最后出现时间',
  `event_count` bigint NOT NULL DEFAULT '0' COMMENT '异常事件数量',
  `user_count` bigint NOT NULL DEFAULT '0' COMMENT '受影响用户数',
  `latest_release` varchar(128) DEFAULT NULL COMMENT '最新客户端版本',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_client_error_issue_fingerprint` (`app_id`, `client_type`, `fingerprint`) USING BTREE,
  KEY `idx_client_error_issue_filter` (`app_id`, `client_type`, `status`, `last_seen_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端异常聚合Issue';

CREATE TABLE IF NOT EXISTS `base_client_error_event` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` bigint unsigned NOT NULL COMMENT 'Telemetry应用ID',
  `issue_id` bigint unsigned NOT NULL COMMENT '异常Issue ID',
  `client_type` varchar(16) NOT NULL COMMENT '客户端类型',
  `environment` varchar(16) NOT NULL COMMENT '运行环境',
  `release` varchar(128) NOT NULL COMMENT '客户端版本',
  `session_id` varchar(64) NOT NULL COMMENT '客户端会话ID',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `error_type` varchar(128) NOT NULL COMMENT '异常类型',
  `message` varchar(2000) NOT NULL COMMENT '异常消息',
  `stack` text COMMENT '异常堆栈',
  `breadcrumbs` json DEFAULT NULL COMMENT '异常前行为',
  `context` json DEFAULT NULL COMMENT '客户端上下文',
  `occur_time` datetime NOT NULL COMMENT '客户端发生时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_client_error_event_issue_time` (`issue_id`, `occur_time`) USING BTREE,
  KEY `idx_client_error_event_filter` (`app_id`, `client_type`, `environment`, `occur_time`) USING BTREE,
  KEY `idx_client_error_event_user` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端异常事件';

CREATE TABLE IF NOT EXISTS `base_stat_event` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` bigint unsigned NOT NULL COMMENT 'Telemetry应用ID',
  `client_type` varchar(16) NOT NULL COMMENT '客户端类型',
  `environment` varchar(16) NOT NULL COMMENT '运行环境',
  `release` varchar(128) NOT NULL COMMENT '客户端版本',
  `session_id` varchar(64) NOT NULL COMMENT '客户端会话ID',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `event_code` varchar(128) NOT NULL COMMENT '事件编码',
  `module` varchar(128) DEFAULT NULL COMMENT '业务模块',
  `biz_type` varchar(128) DEFAULT NULL COMMENT '业务类型',
  `biz_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `result` varchar(32) DEFAULT NULL COMMENT '业务结果',
  `duration` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `properties` json DEFAULT NULL COMMENT '业务扩展属性',
  `context` json DEFAULT NULL COMMENT '客户端上下文',
  `occur_time` datetime NOT NULL COMMENT '发生时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stat_event_filter` (`app_id`, `client_type`, `environment`, `occur_time`) USING BTREE,
  KEY `idx_stat_event_code_time` (`event_code`, `occur_time`) USING BTREE,
  KEY `idx_stat_event_user` (`user_id`, `occur_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telemetry业务统计事件';

CREATE TABLE IF NOT EXISTS `base_stat_daily` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `app_id` bigint unsigned NOT NULL COMMENT 'Telemetry应用ID',
  `client_type` varchar(16) NOT NULL COMMENT '客户端类型',
  `environment` varchar(16) NOT NULL COMMENT '运行环境',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `event_code` varchar(128) NOT NULL COMMENT '事件编码',
  `module` varchar(128) DEFAULT NULL COMMENT '业务模块',
  `pv` bigint NOT NULL DEFAULT 0 COMMENT '事件次数',
  `uv` bigint NOT NULL DEFAULT 0 COMMENT '活跃用户数',
  `success_count` bigint NOT NULL DEFAULT 0 COMMENT '成功次数',
  `fail_count` bigint NOT NULL DEFAULT 0 COMMENT '失败次数',
  `avg_duration` double DEFAULT NULL COMMENT '平均耗时毫秒',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stat_daily_date_filter` (`stat_date`, `app_id`, `client_type`, `environment`) USING BTREE,
  KEY `idx_stat_daily_code` (`event_code`, `stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Telemetry每日聚合统计';

-- Telemetry 菜单：系统设置 / Telemetry / 统计看板、异常 Issue、异常事件、业务事件
INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`
)
SELECT
  10030000, 12000000, 'Telemetry', 5, 1, 'mdi:chart-line', 1, 1, '/admin/system/telemetry',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu` WHERE `id` = 10030000 OR `link_url` = '/admin/system/telemetry'
);

INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`
)
SELECT
  10030001, 10030000, '统计看板', 0, 1, NULL, 1, 1, '/admin/system/telemetry/dashboard',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu` WHERE `id` = 10030001 OR `link_url` = '/admin/system/telemetry/dashboard'
);

INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`
)
SELECT
  10030002, 10030000, '异常 Issue', 1, 1, NULL, 1, 1, '/admin/system/telemetry/issue',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu` WHERE `id` = 10030002 OR `link_url` = '/admin/system/telemetry/issue'
);

INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`
)
SELECT
  10030003, 10030000, '异常事件', 2, 1, NULL, 1, 1, '/admin/system/telemetry/event',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu` WHERE `id` = 10030003 OR `link_url` = '/admin/system/telemetry/event'
);

INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`
)
SELECT
  10030004, 10030000, '业务事件', 3, 1, NULL, 1, 1, '/admin/system/telemetry/stat-event',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu` WHERE `id` = 10030004 OR `link_url` = '/admin/system/telemetry/stat-event'
);
