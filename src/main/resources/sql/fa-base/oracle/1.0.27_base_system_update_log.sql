-- ------------------------- info -------------------------
-- @@ver: 1_000_027
-- @@info: 加固数据库升级日志与并发控制
-- ------------------------- info -------------------------
ALTER TABLE base_system_update_log ADD status NUMBER(5) DEFAULT 1 NOT NULL;

COMMENT ON COLUMN base_system_update_log.status IS '执行状态：1成功/9失败';

ALTER TABLE base_system_update_log ADD file_name VARCHAR2(255);

COMMENT ON COLUMN base_system_update_log.file_name IS 'SQL资源文件名';

ALTER TABLE base_system_update_log ADD checksum VARCHAR2(64);

COMMENT ON COLUMN base_system_update_log.checksum IS 'SQL内容SHA-256';

ALTER TABLE base_system_update_log ADD duration_ms NUMBER(19);

COMMENT ON COLUMN base_system_update_log.duration_ms IS '执行耗时毫秒';

ALTER TABLE base_system_update_log ADD error_msg CLOB;

COMMENT ON COLUMN base_system_update_log.error_msg IS '失败堆栈';

DELETE FROM base_system_update_log duplicate_log WHERE EXISTS (SELECT 1 FROM base_system_update_log latest_log WHERE latest_log.no = duplicate_log.no AND latest_log.ver = duplicate_log.ver AND latest_log.id > duplicate_log.id);

CREATE UNIQUE INDEX u_base_system_update_log_6f96
  ON base_system_update_log (no, ver);
