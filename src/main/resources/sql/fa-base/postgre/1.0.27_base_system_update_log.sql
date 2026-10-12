-- ------------------------- info -------------------------
-- @@ver: 1_000_027
-- @@info: 加固数据库升级日志与并发控制
-- ------------------------- info -------------------------
-- MySQL 5.7不支持ADD COLUMN IF NOT EXISTS，使用information_schema动态生成幂等DDL。
-- 历史版本未限制模块与版本唯一，可能因重复启动或旧并发逻辑产生重复日志。
-- 保留ID最大的最新记录，先清理重复数据，再创建唯一索引。

ALTER TABLE "base_system_update_log" ADD COLUMN IF NOT EXISTS "status" smallint NOT NULL DEFAULT 1;
COMMENT ON COLUMN "base_system_update_log"."status" IS '执行状态：1成功/9失败';
ALTER TABLE "base_system_update_log" ADD COLUMN IF NOT EXISTS "file_name" varchar(255);
COMMENT ON COLUMN "base_system_update_log"."file_name" IS 'SQL资源文件名';
ALTER TABLE "base_system_update_log" ADD COLUMN IF NOT EXISTS "checksum" char(64);
COMMENT ON COLUMN "base_system_update_log"."checksum" IS 'SQL内容SHA-256';
ALTER TABLE "base_system_update_log" ADD COLUMN IF NOT EXISTS "duration_ms" bigint;
COMMENT ON COLUMN "base_system_update_log"."duration_ms" IS '执行耗时毫秒';
ALTER TABLE "base_system_update_log" ADD COLUMN IF NOT EXISTS "error_msg" text;
COMMENT ON COLUMN "base_system_update_log"."error_msg" IS '失败堆栈';

DELETE FROM "base_system_update_log" AS duplicate_log
USING "base_system_update_log" AS latest_log
WHERE latest_log."no" = duplicate_log."no"
  AND latest_log."ver" = duplicate_log."ver"
  AND latest_log."id" > duplicate_log."id";

CREATE UNIQUE INDEX IF NOT EXISTS "uk_base_system_update_log_no_ver"
  ON "base_system_update_log" ("no", "ver");
