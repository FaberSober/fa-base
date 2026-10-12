-- ------------------------- info -------------------------
-- @@ver: 1_000_040
-- @@info: 扩大请求日志和登录日志省市字段长度
-- ------------------------- info -------------------------

ALTER TABLE "base_log_api"
    ALTER COLUMN "pro" TYPE varchar(50),
    ALTER COLUMN "city" TYPE varchar(50);

ALTER TABLE "base_log_login"
    ALTER COLUMN "pro" TYPE varchar(50),
    ALTER COLUMN "city" TYPE varchar(50);
