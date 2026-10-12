-- ------------------------- info -------------------------
-- @@ver: 1_000_048
-- @@info: 增加浏览器 Web Push 订阅表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_browser_push_subscription';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_browser_push_subscription (
    id bigint IDENTITY(1,1) NOT NULL,
    user_id varchar(32) NOT NULL,
    endpoint_hash char(64) NOT NULL,
    endpoint CLOB NOT NULL,
    p256dh varchar(128) NOT NULL,
    auth varchar(64) NOT NULL,
    crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crt_user varchar(32) NOT NULL,
    crt_name varchar(255) NOT NULL,
    crt_host varchar(255) DEFAULT NULL,
    upd_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user varchar(32) DEFAULT NULL,
    upd_name varchar(255) DEFAULT NULL,
    upd_host varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_browser_push_subscription' AND LOWER(INDEX_NAME) = 'uk_base_browser_push_subscription_endpoint_hash';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_browser_push_subscription_endpoint_hash ON base_browser_push_subscription (endpoint_hash)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_browser_push_subscription' AND LOWER(INDEX_NAME) = 'idx_base_browser_push_subscription_user';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_browser_push_subscription_user ON base_browser_push_subscription (user_id)';
    END IF;
END;

COMMENT ON TABLE base_browser_push_subscription IS 'BASE-浏览器推送订阅';
COMMENT ON COLUMN base_browser_push_subscription.id IS 'ID';
COMMENT ON COLUMN base_browser_push_subscription.user_id IS '用户ID';
COMMENT ON COLUMN base_browser_push_subscription.endpoint_hash IS '推送地址SHA-256';
COMMENT ON COLUMN base_browser_push_subscription.endpoint IS '浏览器推送地址';
COMMENT ON COLUMN base_browser_push_subscription.p256dh IS '客户端公钥';
COMMENT ON COLUMN base_browser_push_subscription.auth IS '客户端认证密钥';
COMMENT ON COLUMN base_browser_push_subscription.crt_time IS '创建时间';
COMMENT ON COLUMN base_browser_push_subscription.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_browser_push_subscription.crt_name IS '创建用户';
COMMENT ON COLUMN base_browser_push_subscription.crt_host IS '创建IP';
COMMENT ON COLUMN base_browser_push_subscription.upd_time IS '更新时间';
COMMENT ON COLUMN base_browser_push_subscription.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_browser_push_subscription.upd_name IS '更新用户';
COMMENT ON COLUMN base_browser_push_subscription.upd_host IS '更新IP';
