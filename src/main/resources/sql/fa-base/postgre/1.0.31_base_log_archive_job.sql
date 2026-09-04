-- ------------------------- info -------------------------
-- @@ver: 1_000_031
-- @@info: 注册请求日志月度归档定时任务
-- ------------------------- info -------------------------

INSERT INTO "base_job" (
  "job_name", "cron", "status", "clazz_path", "job_desc",
  "crt_time", "crt_user", "crt_name", "crt_host", "deleted"
)
SELECT
  '按月归档请求日志', '0 0 0 1 * ?', true,
  'com.faber.api.base.admin.jobs.JobLogArchive',
  '每月归档上一个自然月的请求日志',
  CURRENT_TIMESTAMP, '1', 'admin', '127.0.0.1', false
WHERE NOT EXISTS (
  SELECT 1 FROM "base_job"
  WHERE "clazz_path" = 'com.faber.api.base.admin.jobs.JobLogArchive'
);
