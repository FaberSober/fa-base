-- ------------------------- info -------------------------
-- @@ver: 1_000_027
-- @@info: 加固数据库升级日志与并发控制
-- ------------------------- info -------------------------

-- MySQL 5.7不支持ADD COLUMN IF NOT EXISTS，使用information_schema动态生成幂等DDL。
SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND COLUMN_NAME = 'status') = 0,
  'ALTER TABLE `base_system_update_log` ADD COLUMN `status` tinyint(4) NOT NULL DEFAULT ''1'' COMMENT ''执行状态：1成功/9失败'' AFTER `crt_time`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND COLUMN_NAME = 'file_name') = 0,
  'ALTER TABLE `base_system_update_log` ADD COLUMN `file_name` varchar(255) DEFAULT NULL COMMENT ''SQL资源文件名'' AFTER `status`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND COLUMN_NAME = 'checksum') = 0,
  'ALTER TABLE `base_system_update_log` ADD COLUMN `checksum` char(64) DEFAULT NULL COMMENT ''SQL内容SHA-256'' AFTER `file_name`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND COLUMN_NAME = 'duration_ms') = 0,
  'ALTER TABLE `base_system_update_log` ADD COLUMN `duration_ms` bigint(20) DEFAULT NULL COMMENT ''执行耗时毫秒'' AFTER `checksum`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND COLUMN_NAME = 'error_msg') = 0,
  'ALTER TABLE `base_system_update_log` ADD COLUMN `error_msg` longtext COMMENT ''失败堆栈'' AFTER `duration_ms`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史版本未限制模块与版本唯一，可能因重复启动或旧并发逻辑产生重复日志。
-- 保留ID最大的最新记录，先清理重复数据，再创建唯一索引。
DELETE duplicate_log
FROM `base_system_update_log` duplicate_log
INNER JOIN `base_system_update_log` latest_log
        ON latest_log.`no` = duplicate_log.`no`
       AND latest_log.`ver` = duplicate_log.`ver`
       AND latest_log.`id` > duplicate_log.`id`;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'base_system_update_log'
     AND INDEX_NAME = 'uk_base_system_update_log_no_ver') = 0,
  'ALTER TABLE `base_system_update_log` ADD UNIQUE KEY `uk_base_system_update_log_no_ver` (`no`(64), `ver`) USING BTREE',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
