-- ------------------------- info -------------------------
-- @@ver: 1_000_043
-- @@info: 扩展多客户端设备登记
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(COLUMN_NAME) = 'client_type';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user_device ADD client_type varchar(16) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_user_device.client_type IS '客户端类型';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_login' AND LOWER(COLUMN_NAME) = 'client_type';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_log_login ADD client_type varchar(16) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_log_login.client_type IS '客户端类型';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_login' AND LOWER(COLUMN_NAME) = 'device_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_log_login ADD device_id varchar(128) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_log_login.device_id IS '客户端实例ID';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(INDEX_NAME) = 'idx_base_user_device_client';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_user_device_client ON base_user_device (user_id, client_type, device_id)';
    END IF;
END;
