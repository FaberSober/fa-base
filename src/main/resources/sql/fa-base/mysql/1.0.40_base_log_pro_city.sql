-- ------------------------- info -------------------------
-- @@ver: 1_000_040
-- @@info: 扩大请求日志和登录日志省市字段长度
-- ------------------------- info -------------------------

ALTER TABLE `base_log_api`
    MODIFY COLUMN `pro` varchar(50) DEFAULT NULL COMMENT '省',
    MODIFY COLUMN `city` varchar(50) DEFAULT NULL COMMENT '市';

ALTER TABLE `base_log_login`
    MODIFY COLUMN `pro` varchar(50) DEFAULT NULL COMMENT '省',
    MODIFY COLUMN `city` varchar(50) DEFAULT NULL COMMENT '市';
