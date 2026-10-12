-- ------------------------- info -------------------------
-- @@ver: 1_000_037
-- @@info: 增加菜单跨环境同步标识并初始化历史菜单
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_menu' AND LOWER(COLUMN_NAME) = 'config_key';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_rbac_menu ADD config_key varchar(64) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_rbac_menu.config_key IS '跨环境菜单配置标识';

UPDATE base_rbac_menu
SET config_key = CONCAT('legacy:', COALESCE(scope, 1), ':', id)
WHERE config_key IS NULL OR config_key = '';

ALTER TABLE base_rbac_menu MODIFY config_key varchar(64) NOT NULL;

COMMENT ON COLUMN base_rbac_menu.config_key IS '跨环境菜单配置标识';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_menu' AND LOWER(INDEX_NAME) = 'uk_base_rbac_menu_config_key';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_rbac_menu_config_key ON base_rbac_menu (config_key)';
    END IF;
END;
