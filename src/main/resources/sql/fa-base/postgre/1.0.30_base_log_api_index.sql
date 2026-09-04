-- ------------------------- info -------------------------
-- @@ver: 1_000_030
-- @@info: 增加请求日志归档和查询索引
-- ------------------------- info -------------------------

CREATE INDEX IF NOT EXISTS "base_log_api__idx_crt_time" ON "base_log_api" ("crt_time");
CREATE INDEX IF NOT EXISTS "base_log_api__idx_user_crt_time" ON "base_log_api" ("crt_user", "crt_time");
