-- ------------------------- info -------------------------
-- @@ver: 1_000_038
-- @@info: 增加平台必选租户权限标记
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_menu' AND LOWER(COLUMN_NAME) = 'tenant_required';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_rbac_menu ADD tenant_required tinyint DEFAULT ''0'' NOT NULL';
    END IF;
END;

COMMENT ON COLUMN base_rbac_menu.tenant_required IS '租户必选权限';
