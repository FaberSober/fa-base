-- ------------------------- info -------------------------
-- @@ver: 1_000_018
-- @@info: base_log_api接口日志表增加字段：headers-请求头
-- ------------------------- info -------------------------
ALTER TABLE base_log_api ADD headers CLOB NULL;

COMMENT ON COLUMN base_log_api.headers IS '请求头';
