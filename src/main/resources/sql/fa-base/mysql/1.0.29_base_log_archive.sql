-- ------------------------- info -------------------------
-- @@ver: 1_000_029
-- @@info: 增加日志归档元数据表
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_log_archive` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `log_type` varchar(32) NOT NULL COMMENT '日志类型',
  `source_table` varchar(128) NOT NULL COMMENT '原始表名',
  `archive_table` varchar(128) NOT NULL COMMENT '归档表名',
  `archive_month` varchar(7) NOT NULL COMMENT '归档月份，yyyy-MM',
  `data_start_time` datetime DEFAULT NULL COMMENT '数据开始时间',
  `data_end_time` datetime DEFAULT NULL COMMENT '数据结束时间',
  `row_count` bigint NOT NULL DEFAULT '0' COMMENT '数据量',
  `status` varchar(16) NOT NULL COMMENT '归档状态',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `error_message` text COMMENT '异常信息',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_base_log_archive_type_month` (`log_type`, `archive_month`) USING BTREE,
  KEY `idx_base_log_archive_status_month` (`status`, `archive_month`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志归档元数据';
