-- ------------------------- info -------------------------
-- @@ver: 1_000_020
-- @@info: 菜单：base_rbac_menu增加字段：“模块”
-- ------------------------- info -------------------------
ALTER TABLE base_rbac_menu ADD scope NUMBER(5) DEFAULT 1;

COMMENT ON COLUMN base_rbac_menu.scope IS '模块：1-web/2-app';

UPDATE base_rbac_menu SET scope = 1 WHERE scope IS NULL;
