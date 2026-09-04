-- ------------------------- info -------------------------
-- @@ver: 1_000_034
-- @@info: 增加 Telemetry 业务统计事件表
-- ------------------------- info -------------------------

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
