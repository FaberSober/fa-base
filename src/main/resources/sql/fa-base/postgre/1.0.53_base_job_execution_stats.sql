-- ------------------------- info -------------------------
-- @@ver: 1_000_053
-- @@info: 定时任务增加执行统计和异步跟踪摘要
-- ------------------------- info -------------------------

ALTER TABLE "base_job"
    ADD COLUMN IF NOT EXISTS "last_job_log_id" integer DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_execute_time" timestamp(3) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_end_time" timestamp(3) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_status" smallint DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_duration_ms" bigint DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_dispatch_duration_ms" bigint DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "last_tracking_version" integer DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "execute_count" bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "success_count" bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "fail_count" bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "total_duration_ms" bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "duration_sample_count" bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS "stats_since" timestamp(3) DEFAULT NULL;

COMMENT ON COLUMN "base_job"."last_job_log_id" IS '最近一轮执行记录ID';
COMMENT ON COLUMN "base_job"."last_execute_time" IS '最近一轮实际开始时间';
COMMENT ON COLUMN "base_job"."last_end_time" IS '最近一轮业务结束时间';
COMMENT ON COLUMN "base_job"."last_status" IS '最近一轮执行结果';
COMMENT ON COLUMN "base_job"."last_duration_ms" IS '最近一轮总耗时(毫秒)';
COMMENT ON COLUMN "base_job"."last_dispatch_duration_ms" IS '最近一轮触发耗时(毫秒)';
COMMENT ON COLUMN "base_job"."last_tracking_version" IS '最近一轮口径:0历史/1完整跟踪/2不完整';
COMMENT ON COLUMN "base_job"."execute_count" IS '累计执行次数(含保留历史)';
COMMENT ON COLUMN "base_job"."success_count" IS '累计成功次数(含旧口径历史)';
COMMENT ON COLUMN "base_job"."fail_count" IS '累计失败次数(含旧口径历史)';
COMMENT ON COLUMN "base_job"."total_duration_ms" IS '累计总耗时(毫秒)';
COMMENT ON COLUMN "base_job"."duration_sample_count" IS '参与平均的执行次数';
COMMENT ON COLUMN "base_job"."stats_since" IS '耗时统计起始时间';

ALTER TABLE "base_job_log"
    ADD COLUMN IF NOT EXISTS "dispatch_end_time" timestamp(3) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "dispatch_duration_ms" bigint DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "duration_ms" bigint DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "tracking_version" integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN "base_job_log"."dispatch_end_time" IS '主流程退出时间';
COMMENT ON COLUMN "base_job_log"."dispatch_duration_ms" IS '主流程触发耗时(毫秒)';
COMMENT ON COLUMN "base_job_log"."duration_ms" IS '整轮任务总耗时(毫秒)';
COMMENT ON COLUMN "base_job_log"."tracking_version" IS '统计口径:0历史/1完整跟踪/2不完整';

CREATE INDEX IF NOT EXISTS "idx_job_log_job_begin" ON "base_job_log" ("job_id", "begin_time", "id");

-- 只初始化尚无统计起点的任务，保留已删除历史不可还原的事实。
-- 每个摘要字段使用相同排序选择同一轮，不能分别取开始/结束时间的最大值。
-- 历史秒级duration无法还原异步真实总耗时，不参与累计总耗时和平均耗时计算。
UPDATE "base_job"
SET "execute_count" = (
        SELECT COUNT(*) FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
    ),
    "success_count" = (
        SELECT COUNT(*) FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id" AND l."status" = 2
    ),
    "fail_count" = (
        SELECT COUNT(*) FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id" AND l."status" = 9
    ),
    "last_job_log_id" = (
        SELECT l."id" FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
        ORDER BY l."begin_time" DESC, l."id" DESC LIMIT 1
    ),
    "last_execute_time" = (
        SELECT l."begin_time" FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
        ORDER BY l."begin_time" DESC, l."id" DESC LIMIT 1
    ),
    "last_end_time" = (
        SELECT l."end_time" FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
        ORDER BY l."begin_time" DESC, l."id" DESC LIMIT 1
    ),
    "last_status" = (
        SELECT l."status" FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
        ORDER BY l."begin_time" DESC, l."id" DESC LIMIT 1
    ),
    "last_tracking_version" = CASE WHEN EXISTS (
        SELECT 1 FROM "base_job_log" AS l WHERE l."job_id" = "base_job"."id"
    ) THEN 0 ELSE NULL END,
    "last_duration_ms" = NULL,
    "last_dispatch_duration_ms" = NULL,
    "total_duration_ms" = 0,
    "duration_sample_count" = 0,
    "stats_since" = CURRENT_TIMESTAMP(3)
WHERE "stats_since" IS NULL;
