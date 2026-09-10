-- ------------------------- info -------------------------
-- @@ver: 1_000_032
-- @@info: 增加 Telemetry 全局概览菜单
-- ------------------------- info -------------------------

UPDATE "base_rbac_menu" SET "name" = '应用看板', "sort" = 1
 WHERE "id" = 10030001 AND "link_url" = '/admin/system/telemetry/dashboard';
UPDATE "base_rbac_menu" SET "sort" = 2 WHERE "id" = 10030002 AND "parent_id" = 10030000;
UPDATE "base_rbac_menu" SET "sort" = 3 WHERE "id" = 10030003 AND "parent_id" = 10030000;
UPDATE "base_rbac_menu" SET "sort" = 4 WHERE "id" = 10030004 AND "parent_id" = 10030000;
UPDATE "base_rbac_menu" SET "sort" = 5 WHERE "id" = 10030005 AND "parent_id" = 10030000;

INSERT INTO "base_rbac_menu" (
  "id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url",
  "crt_time", "crt_user", "crt_name", "crt_host", "deleted"
)
SELECT 10030006, 10030000, 1, '全局概览', 0, 1, NULL, TRUE, 1,
       '/admin/system/telemetry/overview', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE NOT EXISTS (
  SELECT 1 FROM "base_rbac_menu" WHERE "id" = 10030006 OR "link_url" = '/admin/system/telemetry/overview'
)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, 10030006, 0, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "deleted" = FALSE
  )
  AND NOT EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "menu_id" = 10030006 AND "deleted" = FALSE
  );
