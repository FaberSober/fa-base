-- ------------------------- info -------------------------
-- @@ver: 1_000_029
-- @@info: 增加日志归档元数据表、请求日志索引和月度归档定时任务
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_archive';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_log_archive (
    id bigint IDENTITY(1,1) NOT NULL,
    log_type varchar(32) NOT NULL,
    source_table varchar(128) NOT NULL,
    archive_table varchar(128) NOT NULL,
    archive_month varchar(7) NOT NULL,
    data_start_time TIMESTAMP DEFAULT NULL,
    data_end_time TIMESTAMP DEFAULT NULL,
    row_count bigint DEFAULT ''0'' NOT NULL,
    status varchar(16) NOT NULL,
    archive_time TIMESTAMP DEFAULT NULL,
    error_message CLOB,
    crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crt_user varchar(32) NOT NULL,
    crt_name varchar(255) NOT NULL,
    crt_host varchar(255) DEFAULT NULL,
    upd_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user varchar(32) DEFAULT NULL,
    upd_name varchar(255) DEFAULT NULL,
    upd_host varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_archive' AND LOWER(INDEX_NAME) = 'uk_base_log_archive_type_month';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_log_archive_type_month ON base_log_archive (log_type, archive_month)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_archive' AND LOWER(INDEX_NAME) = 'idx_base_log_archive_status_month';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_log_archive_status_month ON base_log_archive (status, archive_month)';
    END IF;
END;

COMMENT ON TABLE base_log_archive IS '日志归档元数据';
COMMENT ON COLUMN base_log_archive.id IS 'ID';
COMMENT ON COLUMN base_log_archive.log_type IS '日志类型';
COMMENT ON COLUMN base_log_archive.source_table IS '原始表名';
COMMENT ON COLUMN base_log_archive.archive_table IS '归档表名';
COMMENT ON COLUMN base_log_archive.archive_month IS '归档月份，yyyy-MM';
COMMENT ON COLUMN base_log_archive.data_start_time IS '数据开始时间';
COMMENT ON COLUMN base_log_archive.data_end_time IS '数据结束时间';
COMMENT ON COLUMN base_log_archive.row_count IS '数据量';
COMMENT ON COLUMN base_log_archive.status IS '归档状态';
COMMENT ON COLUMN base_log_archive.archive_time IS '归档时间';
COMMENT ON COLUMN base_log_archive.error_message IS '异常信息';
COMMENT ON COLUMN base_log_archive.crt_time IS '创建时间';
COMMENT ON COLUMN base_log_archive.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_log_archive.crt_name IS '创建用户';
COMMENT ON COLUMN base_log_archive.crt_host IS '创建IP';
COMMENT ON COLUMN base_log_archive.upd_time IS '更新时间';
COMMENT ON COLUMN base_log_archive.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_log_archive.upd_name IS '更新用户';
COMMENT ON COLUMN base_log_archive.upd_host IS '更新IP';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_api' AND LOWER(INDEX_NAME) = 'idx_base_log_api_crt_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_log_api_crt_time ON base_log_api (crt_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_api' AND LOWER(INDEX_NAME) = 'idx_base_log_api_user_crt_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_log_api_user_crt_time ON base_log_api (crt_user, crt_time)';
    END IF;
END;

INSERT INTO base_job (
  job_name, cron, status, clazz_path, job_desc,
  crt_time, crt_user, crt_name, crt_host, deleted
)
SELECT
  '按月归档请求日志', '0 0 0 1 * ?', 1,
  'com.faber.api.base.admin.jobs.JobLogArchive',
  '每月归档上一个自然月的请求日志',
  CURRENT_TIMESTAMP, '1', 'admin', '127.0.0.1', 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_job
  WHERE clazz_path = 'com.faber.api.base.admin.jobs.JobLogArchive'
);
