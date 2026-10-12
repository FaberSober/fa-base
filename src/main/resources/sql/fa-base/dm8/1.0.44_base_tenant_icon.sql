-- ------------------------- info -------------------------
-- @@ver: 1_000_044
-- @@info: 租户图标
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant' AND LOWER(COLUMN_NAME) = 'icon';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE tn_tenant ADD icon varchar(512) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN tn_tenant.icon IS '租户图标文件ID';
