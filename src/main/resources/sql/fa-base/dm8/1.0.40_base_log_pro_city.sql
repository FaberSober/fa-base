-- ------------------------- info -------------------------
-- @@ver: 1_000_040
-- @@info: 扩大请求日志和登录日志省市字段长度
-- ------------------------- info -------------------------

ALTER TABLE base_log_api MODIFY pro varchar(50) DEFAULT NULL;

COMMENT ON COLUMN base_log_api.pro IS '省';

ALTER TABLE base_log_api MODIFY city varchar(50) DEFAULT NULL;

COMMENT ON COLUMN base_log_api.city IS '市';

ALTER TABLE base_log_login MODIFY pro varchar(50) DEFAULT NULL;

COMMENT ON COLUMN base_log_login.pro IS '省';

ALTER TABLE base_log_login MODIFY city varchar(50) DEFAULT NULL;

COMMENT ON COLUMN base_log_login.city IS '市';
