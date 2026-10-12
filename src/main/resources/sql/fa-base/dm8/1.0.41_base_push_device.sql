-- ------------------------- info -------------------------
-- @@ver: 1_000_041
-- @@info: 增加推送设备绑定表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_push_device';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_push_device (
    id bigint IDENTITY(1,1) NOT NULL,
    user_id varchar(32) NOT NULL,
    provider varchar(32) NOT NULL,
    client_id varchar(255) NOT NULL,
    app_id varchar(128) NOT NULL,
    platform varchar(32) NOT NULL,
    environment varchar(16) NOT NULL,
    enabled tinyint DEFAULT ''1'' NOT NULL,
    last_seen_time timestamp NULL DEFAULT NULL,
    invalid_time timestamp NULL DEFAULT NULL,
    crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crt_user varchar(32) NOT NULL,
    crt_name varchar(255) NOT NULL,
    crt_host varchar(255) DEFAULT NULL,
    upd_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user varchar(32) DEFAULT NULL,
    upd_name varchar(255) DEFAULT NULL,
    upd_host varchar(255) DEFAULT NULL,
    deleted tinyint DEFAULT ''0'' NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_push_device' AND LOWER(INDEX_NAME) = 'uk_base_push_device_binding';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_push_device_binding ON base_push_device (provider, client_id, app_id, environment)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_push_device' AND LOWER(INDEX_NAME) = 'idx_base_push_device_user_enabled';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_push_device_user_enabled ON base_push_device (user_id, enabled)';
    END IF;
END;

COMMENT ON TABLE base_push_device IS 'BASE-推送设备';
COMMENT ON COLUMN base_push_device.id IS 'ID';
COMMENT ON COLUMN base_push_device.user_id IS '用户ID';
COMMENT ON COLUMN base_push_device.provider IS '推送服务商';
COMMENT ON COLUMN base_push_device.client_id IS 'Push Client ID';
COMMENT ON COLUMN base_push_device.app_id IS '应用ID';
COMMENT ON COLUMN base_push_device.platform IS '平台';
COMMENT ON COLUMN base_push_device.environment IS '推送环境';
COMMENT ON COLUMN base_push_device.enabled IS '是否启用';
COMMENT ON COLUMN base_push_device.last_seen_time IS '最后上报时间';
COMMENT ON COLUMN base_push_device.invalid_time IS '失效时间';
COMMENT ON COLUMN base_push_device.crt_time IS '创建时间';
COMMENT ON COLUMN base_push_device.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_push_device.crt_name IS '创建用户';
COMMENT ON COLUMN base_push_device.crt_host IS '创建IP';
COMMENT ON COLUMN base_push_device.upd_time IS '更新时间';
COMMENT ON COLUMN base_push_device.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_push_device.upd_name IS '更新用户';
COMMENT ON COLUMN base_push_device.upd_host IS '更新IP';
COMMENT ON COLUMN base_push_device.deleted IS '逻辑删除';
