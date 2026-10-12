-- ------------------------- info -------------------------
-- @@ver: 1_000_020
-- @@info: 菜单：base_rbac_menu增加字段：“模块”
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_menu' AND LOWER(COLUMN_NAME) = 'scope';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_rbac_menu ADD scope tinyint NULL DEFAULT 1';
    END IF;
END;

COMMENT ON COLUMN base_rbac_menu.scope IS '模块：1-web/2-app';

UPDATE base_rbac_menu SET scope = 1 WHERE scope IS NULL;
