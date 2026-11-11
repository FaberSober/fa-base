-- ------------------------- info -------------------------
-- @@ver: 1_000_055
-- @@info: 增加定时任务可视化菜单
-- ------------------------- info -------------------------

INSERT INTO "base_rbac_menu" (
  "id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url",
  "crt_time", "crt_user", "crt_name", "crt_host", "deleted"
)
SELECT 12021600, 12020000, 1, '定时任务可视化', 13, 1, 'mdi:chart-timeline-variant', TRUE, 1,
       '/admin/system/base/job/visual', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE NOT EXISTS (
  SELECT 1 FROM "base_rbac_menu"
  WHERE "id" = 12021600 OR "link_url" = '/admin/system/base/job/visual'
)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, 12021600, FALSE, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', FALSE
WHERE EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "deleted" = FALSE
  )
  AND EXISTS (
    SELECT 1 FROM "base_rbac_menu" WHERE "id" = 12021600 AND "deleted" = FALSE
  )
  AND NOT EXISTS (
    SELECT 1 FROM "base_rbac_role_menu"
    WHERE "role_id" = 1 AND "menu_id" = 12021600 AND "deleted" = FALSE
  );
