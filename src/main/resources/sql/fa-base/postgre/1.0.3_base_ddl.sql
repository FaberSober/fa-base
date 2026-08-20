-- ------------------------- info -------------------------
-- @@ver: 1_000_003
-- @@info: 1. 请求日志增加字段remark；
-- ------------------------- info -------------------------

-- 请求日志增加字段remark
ALTER TABLE "base_log_api" ADD COLUMN "remark" text NULL;
COMMENT ON COLUMN "base_log_api"."remark" IS '请求备注';
ALTER TABLE "base_log_api" ADD COLUMN "opr_remark" varchar(255) NULL;
COMMENT ON COLUMN "base_log_api"."opr_remark" IS '操作备注';
