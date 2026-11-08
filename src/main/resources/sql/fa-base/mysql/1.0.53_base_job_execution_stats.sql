-- ------------------------- info -------------------------
-- @@ver: 1_000_053
-- @@info: 定时任务增加执行统计和异步跟踪摘要
-- ------------------------- info -------------------------

ALTER TABLE `base_job`
    ADD COLUMN `last_job_log_id` int(11) DEFAULT NULL COMMENT '最近一轮执行记录ID',
    ADD COLUMN `last_execute_time` datetime(3) DEFAULT NULL COMMENT '最近一轮实际开始时间',
    ADD COLUMN `last_end_time` datetime(3) DEFAULT NULL COMMENT '最近一轮业务结束时间',
    ADD COLUMN `last_status` tinyint(4) DEFAULT NULL COMMENT '最近一轮执行结果',
    ADD COLUMN `last_duration_ms` bigint DEFAULT NULL COMMENT '最近一轮总耗时(毫秒)',
    ADD COLUMN `last_dispatch_duration_ms` bigint DEFAULT NULL COMMENT '最近一轮触发耗时(毫秒)',
    ADD COLUMN `last_tracking_version` int(11) DEFAULT NULL COMMENT '最近一轮口径:0历史/1完整跟踪/2不完整',
    ADD COLUMN `execute_count` bigint NOT NULL DEFAULT 0 COMMENT '累计执行次数(含保留历史)',
    ADD COLUMN `success_count` bigint NOT NULL DEFAULT 0 COMMENT '累计成功次数(含旧口径历史)',
    ADD COLUMN `fail_count` bigint NOT NULL DEFAULT 0 COMMENT '累计失败次数(含旧口径历史)',
    ADD COLUMN `total_duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '累计总耗时(毫秒)',
    ADD COLUMN `duration_sample_count` bigint NOT NULL DEFAULT 0 COMMENT '参与平均的执行次数',
    ADD COLUMN `stats_since` datetime(3) DEFAULT NULL COMMENT '耗时统计起始时间';

ALTER TABLE `base_job_log`
    ADD COLUMN `dispatch_end_time` datetime(3) DEFAULT NULL COMMENT '主流程退出时间',
    ADD COLUMN `dispatch_duration_ms` bigint DEFAULT NULL COMMENT '主流程触发耗时(毫秒)',
    ADD COLUMN `duration_ms` bigint DEFAULT NULL COMMENT '整轮任务总耗时(毫秒)',
    ADD COLUMN `tracking_version` int(11) NOT NULL DEFAULT 0 COMMENT '统计口径:0历史/1完整跟踪/2不完整';

CREATE INDEX `idx_job_log_job_begin` ON `base_job_log` (`job_id`, `begin_time`, `id`);

-- 只初始化尚无统计起点的任务，保留已删除历史不可还原的事实。
-- 每个摘要字段使用相同排序选择同一轮，不能分别取开始/结束时间的最大值。
-- 历史秒级duration无法还原异步真实总耗时，不参与累计总耗时和平均耗时计算。
UPDATE `base_job`
SET `execute_count` = (
        SELECT COUNT(*) FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
    ),
    `success_count` = (
        SELECT COUNT(*) FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id` AND l.`status` = 2
    ),
    `fail_count` = (
        SELECT COUNT(*) FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id` AND l.`status` = 9
    ),
    `last_job_log_id` = (
        SELECT l.`id` FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
        ORDER BY l.`begin_time` DESC, l.`id` DESC LIMIT 1
    ),
    `last_execute_time` = (
        SELECT l.`begin_time` FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
        ORDER BY l.`begin_time` DESC, l.`id` DESC LIMIT 1
    ),
    `last_end_time` = (
        SELECT l.`end_time` FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
        ORDER BY l.`begin_time` DESC, l.`id` DESC LIMIT 1
    ),
    `last_status` = (
        SELECT l.`status` FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
        ORDER BY l.`begin_time` DESC, l.`id` DESC LIMIT 1
    ),
    `last_tracking_version` = CASE WHEN EXISTS (
        SELECT 1 FROM `base_job_log` AS l WHERE l.`job_id` = `base_job`.`id`
    ) THEN 0 ELSE NULL END,
    `last_duration_ms` = NULL,
    `last_dispatch_duration_ms` = NULL,
    `total_duration_ms` = 0,
    `duration_sample_count` = 0,
    `stats_since` = CURRENT_TIMESTAMP(3)
WHERE `stats_since` IS NULL;
