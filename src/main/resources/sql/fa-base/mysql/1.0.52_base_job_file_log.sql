-- ------------------------- info -------------------------
-- @@ver: 1_000_052
-- @@info: 定时任务增加详细日志开关和执行日志相对路径
-- ------------------------- info -------------------------

ALTER TABLE `base_job`
    ADD COLUMN `detail_log_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否记录详细日志:0否/1是';

ALTER TABLE `base_job_log`
    ADD COLUMN `log_path` varchar(255) DEFAULT NULL COMMENT '本次执行日志相对路径';
