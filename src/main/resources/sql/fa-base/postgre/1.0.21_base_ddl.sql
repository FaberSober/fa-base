-- ------------------------- info -------------------------
-- @@ver: 1_000_021
-- @@info: 菜单：base_rbac_menu修改字段：“模块”字段设置为非必填；更新菜单管理路径为“/admin/system/base/menu”
-- ------------------------- info -------------------------

ALTER TABLE "base_rbac_menu" ALTER COLUMN "scope" TYPE smallint;
ALTER TABLE "base_rbac_menu" ALTER COLUMN "scope" DROP NOT NULL;
ALTER TABLE "base_rbac_menu" ALTER COLUMN "scope" SET DEFAULT 1;
COMMENT ON COLUMN "base_rbac_menu"."scope" IS '模块：1-web/2-app';
UPDATE base_rbac_menu SET link_url = '/admin/system/base/menu' WHERE link_url = '/admin/system/base/menuV2';
