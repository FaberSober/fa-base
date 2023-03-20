-- ------------------------- info -------------------------
-- @@ver: 1_000_003
-- @@info: 1. 请求日志增加字段remark；
-- ------------------------- info -------------------------

-- 请求日志增加字段remark
ALTER TABLE `base_log_api` ADD COLUMN `remark` longtext NULL COMMENT '请求备注' AFTER `ret_status`;