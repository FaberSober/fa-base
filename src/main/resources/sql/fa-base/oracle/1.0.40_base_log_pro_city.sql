-- ------------------------- info -------------------------
-- @@ver: 1_000_040
-- @@info: 扩大请求日志和登录日志省市字段长度
-- ------------------------- info -------------------------

ALTER TABLE base_log_api
    MODIFY (pro VARCHAR2(50), city VARCHAR2(50));

ALTER TABLE base_log_login
    MODIFY (pro VARCHAR2(50), city VARCHAR2(50));
