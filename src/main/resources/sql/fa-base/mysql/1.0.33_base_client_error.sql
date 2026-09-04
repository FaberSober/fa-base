-- ------------------------- info -------------------------
-- @@ver: 1_000_033
-- @@info: 增加客户端异常 Issue 与事件表
-- ------------------------- info -------------------------

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
