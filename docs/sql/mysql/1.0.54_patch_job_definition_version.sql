-- ------------------------- info -------------------------
-- @@ver: 1_000_054
-- @@info: 已执行旧版1.0.54数据库的流程定义历史版本手动补丁
-- ------------------------- info -------------------------

-- 仅在已执行旧版完整1.0.54的目标数据库手动运行；新库使用resources下完整脚本。
-- 此文件位于docs/sql，不参与框架自动升级，不新增升级版本，不修改升级记录。
-- 执行前停止应用，确认没有运行中的任务；执行后只启动已接入版本表的新代码。
-- 测试库专用：清理未关联版本的旧流程运行及其节点/业务日志，然后删除flow_snapshot。
-- 保留任务配置、已关联版本的运行、普通独立任务日志；不执行任务、不回填旧快照。

-- MySQL 5.7兼容；DDL自动提交，建议逐段执行，重复执行不会重复增加本补丁对象。
CREATE TABLE IF NOT EXISTS `base_job_definition_version` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '流程定义版本ID',
    `job_id` int(11) NOT NULL COMMENT '所属流程任务配置ID',
    `version_no` int NOT NULL COMMENT '任务内递增版本号，从1开始',
    `job_name` varchar(255) NOT NULL COMMENT '任务名称快照',
    `detail_log_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '详细日志开关快照:0关闭/1开启',
    `flow_definition` longtext NOT NULL COMMENT '不可变流程定义JSON，包含节点位置',
    `content_hash` char(64) NOT NULL COMMENT '名称、日志开关及规范化定义的SHA-256，应用计算',
    `crt_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '版本创建时间',
    `crt_user` varchar(32) DEFAULT NULL COMMENT '版本创建用户ID',
    `crt_name` varchar(255) DEFAULT NULL COMMENT '版本创建用户名',
    `crt_host` varchar(255) DEFAULT NULL COMMENT '版本创建来源IP',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_definition_version` (`job_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-不可变流程定义历史版本';

SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_job' AND COLUMN_NAME = 'current_definition_version_id') = 0,
    'ALTER TABLE `base_job` ADD COLUMN `current_definition_version_id` int(11) DEFAULT NULL COMMENT ''当前流程定义版本ID，普通任务及旧配置可空''',
    'SELECT 1 AS already_exists'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;

SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_job_log' AND COLUMN_NAME = 'definition_version_id') = 0,
    'ALTER TABLE `base_job_log` ADD COLUMN `definition_version_id` int(11) DEFAULT NULL COMMENT ''本轮不可变流程定义版本ID，普通任务为空''',
    'SELECT 1 AS already_exists'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;

SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_job' AND INDEX_NAME = 'idx_job_current_definition_version') = 0,
    'CREATE INDEX `idx_job_current_definition_version` ON `base_job` (`current_definition_version_id`)',
    'SELECT 1 AS already_exists'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;

SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_job_log' AND INDEX_NAME = 'idx_job_log_definition_version') = 0,
    'CREATE INDEX `idx_job_log_definition_version` ON `base_job_log` (`definition_version_id`)',
    'SELECT 1 AS already_exists'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;

-- 拒绝同一流程同时存在新旧运行的库；客户端必须遇错停止。
SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM base_job_log old_run JOIN base_job_log new_run ON new_run.job_id = old_run.job_id WHERE old_run.job_type = 1 AND old_run.definition_version_id IS NULL AND new_run.job_type = 1 AND new_run.definition_version_id IS NOT NULL) > 0,
    'SELECT * FROM `ERROR_新旧流程运行混合_请先处理再迁移`',
    'SELECT 1 AS migration_guard_passed'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;

-- 无新版运行的受影响流程配置重置统计；独立普通任务不受影响。
UPDATE base_job SET last_job_log_id = NULL, last_execute_time = NULL, last_end_time = NULL, last_status = NULL, last_duration_ms = NULL, last_dispatch_duration_ms = NULL, last_tracking_version = NULL, execute_count = 0, success_count = 0, fail_count = 0, total_duration_ms = 0, duration_sample_count = 0, stats_since = CURRENT_TIMESTAMP
WHERE job_type = 1
  AND EXISTS (SELECT 1 FROM base_job_log old_run WHERE old_run.job_id = base_job.id AND old_run.job_type = 1 AND old_run.definition_version_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM base_job_log new_run WHERE new_run.job_id = base_job.id AND new_run.job_type = 1 AND new_run.definition_version_id IS NOT NULL);

-- 仅清理旧流程运行关联的节点业务日志，不按任务类或job_id扩大范围。
DELETE child FROM base_job_log child
JOIN base_job_flow_node_run node ON node.job_log_id = child.id
JOIN base_job_log parent ON parent.id = node.flow_log_id
WHERE parent.job_type = 1 AND parent.definition_version_id IS NULL AND child.trigger_source = 3 AND parent.job_id NOT IN (SELECT job_id FROM (SELECT DISTINCT job_id FROM base_job_log WHERE job_type = 1 AND definition_version_id IS NOT NULL AND job_id IS NOT NULL) versioned_jobs);
DELETE node FROM base_job_flow_node_run node
JOIN base_job_log parent ON parent.id = node.flow_log_id
WHERE parent.job_type = 1 AND parent.definition_version_id IS NULL AND parent.job_id NOT IN (SELECT job_id FROM (SELECT DISTINCT job_id FROM base_job_log WHERE job_type = 1 AND definition_version_id IS NOT NULL AND job_id IS NOT NULL) versioned_jobs);
DELETE FROM base_job_log WHERE job_type = 1 AND definition_version_id IS NULL
  AND job_id NOT IN (SELECT job_id FROM (SELECT DISTINCT job_id FROM base_job_log WHERE job_type = 1 AND definition_version_id IS NOT NULL AND job_id IS NOT NULL) versioned_jobs);

SET @fa_dag_version_patch_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'base_job_log' AND COLUMN_NAME = 'flow_snapshot') > 0 AND (SELECT COUNT(*) FROM base_job_log WHERE job_type = 1 AND definition_version_id IS NULL) = 0,
    'ALTER TABLE `base_job_log` DROP COLUMN `flow_snapshot`',
    'SELECT 1 AS already_removed'
);
PREPARE fa_dag_version_patch_stmt FROM @fa_dag_version_patch_sql;
EXECUTE fa_dag_version_patch_stmt;
DEALLOCATE PREPARE fa_dag_version_patch_stmt;
