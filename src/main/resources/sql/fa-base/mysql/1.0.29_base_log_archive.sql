-- ------------------------- info -------------------------
-- @@ver: 1_000_029
-- @@info: 增加日志归档元数据表、请求日志索引和月度归档定时任务
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

SET @fa_log_index_schema = DATABASE();
SET @fa_log_index_sql = (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @fa_log_index_schema AND table_name = 'base_log_api' AND index_name = 'idx_base_log_api_crt_time'),
    'SELECT 1',
    'ALTER TABLE base_log_api ADD INDEX idx_base_log_api_crt_time (crt_time)'
  )
);
PREPARE fa_log_index_stmt FROM @fa_log_index_sql;
EXECUTE fa_log_index_stmt;
DEALLOCATE PREPARE fa_log_index_stmt;

SET @fa_log_index_sql = (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = @fa_log_index_schema AND table_name = 'base_log_api' AND index_name = 'idx_base_log_api_user_crt_time'),
    'SELECT 1',
    'ALTER TABLE base_log_api ADD INDEX idx_base_log_api_user_crt_time (crt_user, crt_time)'
  )
);
PREPARE fa_log_index_stmt FROM @fa_log_index_sql;
EXECUTE fa_log_index_stmt;
DEALLOCATE PREPARE fa_log_index_stmt;

INSERT INTO `base_job` (
  `job_name`, `cron`, `status`, `clazz_path`, `job_desc`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`
)
SELECT
  '按月归档请求日志', '0 0 0 1 * ?', 1,
  'com.faber.api.base.admin.jobs.JobLogArchive',
  '每月归档上一个自然月的请求日志',
  CURRENT_TIMESTAMP, '1', 'admin', '127.0.0.1', 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_job`
  WHERE `clazz_path` = 'com.faber.api.base.admin.jobs.JobLogArchive'
);
