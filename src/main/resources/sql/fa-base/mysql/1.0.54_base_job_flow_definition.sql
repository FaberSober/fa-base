-- ------------------------- info -------------------------
-- @@ver: 1_000_054
-- @@info: 轻量DAG任务流编排完整数据结构
-- ------------------------- info -------------------------

ALTER TABLE `base_job` ADD COLUMN `job_type` int NOT NULL DEFAULT 0 COMMENT '任务类型:0普通/1流程', ADD COLUMN `flow_definition` longtext DEFAULT NULL COMMENT '流程定义JSON';

-- 流程节点直接引用注册类，无独立任务配置时 job_id 允许为空。
ALTER TABLE `base_job_log`
    MODIFY COLUMN `job_id` int(11) DEFAULT NULL COMMENT '独立任务或流程配置ID，类路径节点可为空',
    ADD COLUMN `job_type` int NOT NULL DEFAULT 0 COMMENT '本轮任务类型:0普通/1流程',
    ADD COLUMN `trigger_source` int NOT NULL DEFAULT 0 COMMENT '触发来源:0历史/1手动/2Cron/3流程节点',
    ADD COLUMN `flow_snapshot` longtext DEFAULT NULL COMMENT '本轮流程完整配置快照',
    ADD COLUMN `clazz_path` varchar(255) DEFAULT NULL COMMENT '执行类路径快照，历史记录可空',
    ADD COLUMN `job_name` varchar(255) DEFAULT NULL COMMENT '任务名称快照，历史记录可空';

CREATE TABLE IF NOT EXISTS `base_job_flow_node_run` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '节点运行ID',
    `flow_log_id` int(11) NOT NULL COMMENT '所属流程执行记录ID',
    `node_id` varchar(100) NOT NULL COMMENT '本轮流程内独立节点ID',
    `clazz_path` varchar(255) NOT NULL COMMENT '引用的注册任务类路径快照',
    `node_name` varchar(255) NOT NULL COMMENT '节点名称快照',
    `attempt` int NOT NULL DEFAULT 1 COMMENT '尝试序号，首版固定1',
    `job_log_id` int(11) DEFAULT NULL COMMENT '实际业务执行日志ID，未启动为空',
    `status` int NOT NULL DEFAULT 0 COMMENT '0等待依赖/10待调度/20启动中/30执行中/40成功/50失败/60阻断/70停止/80未确认',
    `begin_time` datetime(3) DEFAULT NULL COMMENT '实际开始时间',
    `end_time` datetime(3) DEFAULT NULL COMMENT '真实结束时间，未确认不伪造',
    `reason` text COMMENT '失败、阻断、停止或未确认原因',
    `crt_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `upd_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间，由应用维护',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_node_attempt` (`flow_log_id`, `node_id`, `attempt`),
    UNIQUE KEY `uk_flow_node_job_log` (`job_log_id`),
    KEY `idx_flow_node_status` (`flow_log_id`, `status`),
    KEY `idx_flow_node_recovery` (`status`, `upd_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-流程节点运行记录';
