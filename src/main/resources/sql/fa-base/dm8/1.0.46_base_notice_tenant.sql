-- ------------------------- info -------------------------
-- @@ver: 1_000_046
-- @@info: 系统公告租户隔离，历史公告保留空租户
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_notice' AND LOWER(COLUMN_NAME) = 'tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_notice ADD tenant_id varchar(32) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_notice.tenant_id IS '租户ID';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_notice' AND LOWER(INDEX_NAME) = 'idx_base_notice_tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_notice_tenant_id ON base_notice (tenant_id)';
    END IF;
END;
