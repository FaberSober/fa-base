-- ------------------------- info -------------------------
-- @@ver: 1_000_045
-- @@info: 消息租户归属
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_msg' AND LOWER(COLUMN_NAME) = 'tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_msg ADD tenant_id varchar(32) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_msg.tenant_id IS '租户ID';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_msg' AND LOWER(INDEX_NAME) = 'idx_base_msg_to_tenant_read';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_msg_to_tenant_read ON base_msg (to_user_id, tenant_id, is_read)';
    END IF;
END;
