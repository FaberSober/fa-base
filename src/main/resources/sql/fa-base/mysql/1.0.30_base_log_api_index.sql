-- ------------------------- info -------------------------
-- @@ver: 1_000_030
-- @@info: 增加请求日志归档和查询索引
-- ------------------------- info -------------------------

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
