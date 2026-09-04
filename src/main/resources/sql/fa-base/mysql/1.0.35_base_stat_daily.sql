-- ------------------------- info -------------------------
-- @@ver: 1_000_035
-- @@info: 增加 Telemetry 每日聚合统计表
-- ------------------------- info -------------------------

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
