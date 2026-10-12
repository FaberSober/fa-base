-- ------------------------- info -------------------------
-- @@ver: 1_000_035
-- @@info: 增加统一日历维护菜单
-- ------------------------- info -------------------------

INSERT INTO "base_rbac_menu" (
  "id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url",
  "crt_time", "crt_user", "crt_name", "crt_host", "deleted"
)
SELECT 12021500, 12020000, 1, '统一日历', 12, 1, 'mdi:calendar-clock-outline', TRUE, 1,
       '/admin/system/base/calendar', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE NOT EXISTS (
  SELECT 1 FROM "base_rbac_menu"
  WHERE "id" = 12021500 OR "link_url" = '/admin/system/base/calendar'
)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, 12021500, FALSE, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "deleted" = FALSE
  )
  AND EXISTS (
    SELECT 1 FROM "base_rbac_menu" WHERE "id" = 12021500 AND "deleted" = FALSE
  )
  AND NOT EXISTS (
    SELECT 1 FROM "base_rbac_role_menu"
    WHERE "role_id" = 1 AND "menu_id" = 12021500 AND "deleted" = FALSE
  );
