-- ------------------------- info -------------------------
-- @@ver: 1_000_014
-- @@info: 用户表增加字段：last_online_time最后在线时间
-- ------------------------- info -------------------------
ALTER TABLE base_user ADD last_online_time timestamp NULL;

COMMENT ON COLUMN base_user.last_online_time IS '最后在线时间';
