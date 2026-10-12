-- ------------------------- info -------------------------
-- @@ver: 1_000_033
-- @@info: 增加授权管理菜单
-- ------------------------- info -------------------------

INSERT INTO "base_rbac_menu" (
  "id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url",
  "crt_time", "crt_user", "crt_name", "crt_host", "deleted"
)
SELECT 12021400, 12020000, 1, '授权管理', 11, 1, 'mdi:certificate-outline', TRUE, 1,
       '/admin/system/base/license', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE NOT EXISTS (
  SELECT 1 FROM "base_rbac_menu"
  WHERE "id" = 12021400 OR "link_url" = '/admin/system/base/license'
)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, 12021400, FALSE, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "deleted" = FALSE
  )
  AND EXISTS (
    SELECT 1 FROM "base_rbac_menu" WHERE "id" = 12021400 AND "deleted" = FALSE
  )
  AND NOT EXISTS (
    SELECT 1 FROM "base_rbac_role_menu"
    WHERE "role_id" = 1 AND "menu_id" = 12021400 AND "deleted" = FALSE
  );
