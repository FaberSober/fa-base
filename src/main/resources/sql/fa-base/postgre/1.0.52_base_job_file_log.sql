-- ------------------------- info -------------------------
-- @@ver: 1_000_052
-- @@info: 定时任务增加详细日志开关和执行日志相对路径
-- ------------------------- info -------------------------

ALTER TABLE "base_job" ADD COLUMN IF NOT EXISTS "detail_log_enabled" boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN "base_job"."detail_log_enabled" IS '是否记录详细日志';

ALTER TABLE "base_job_log" ADD COLUMN IF NOT EXISTS "log_path" varchar(255) DEFAULT NULL;
COMMENT ON COLUMN "base_job_log"."log_path" IS '本次执行日志相对路径';
