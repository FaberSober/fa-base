-- ------------------------- info -------------------------
-- @@ver: 1_000_042
-- @@info: 增加在线客户端菜单与查看权限
-- ------------------------- info -------------------------

INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
VALUES (12040600, 12040000, 1, '在线客户端', 5, 1, 'mdi:cellphone-link', true, 1,
        '/admin/system/monitor/remoteClient', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, m."id", false, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', false
FROM "base_rbac_menu" m
WHERE m."id" IN (12000000, 12040000, 12040600)
  AND EXISTS (SELECT 1 FROM "base_rbac_role_menu" existing WHERE existing."role_id" = 1 AND existing."deleted" = false)
  AND NOT EXISTS (
      SELECT 1 FROM "base_rbac_role_menu" rm
      WHERE rm."role_id" = 1 AND rm."menu_id" = m."id" AND rm."deleted" = false
  );
