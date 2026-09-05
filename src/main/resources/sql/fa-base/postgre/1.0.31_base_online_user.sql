-- ------------------------- info -------------------------
-- @@ver: 1_000_031
-- @@info: 增加后台在线用户菜单与强制下线权限
-- ------------------------- info -------------------------
-- 仅平台角色可使用；会话元信息保存在 Redis，无新增业务表。

INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 12040500, 12040000, 1, '在线用户', 4, 1, 'mdi:account-network-outline', true, 1,
       '/admin/system/monitor/onlineUser', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', false
WHERE NOT EXISTS (SELECT 1 FROM "base_rbac_menu" WHERE "id" = 12040500);

INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 12040501, 12040500, 1, '强制下线', 0, 9, NULL, true, 1,
       '/admin/system/monitor/onlineUser:kickout', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', false
WHERE NOT EXISTS (SELECT 1 FROM "base_rbac_menu" WHERE "id" = 12040501);

-- 为默认超级管理员角色补齐访问路径与操作权限，其他平台角色按需授权。
INSERT INTO "base_rbac_role_menu"
    ("role_id", "menu_id", "half_checked", "crt_time", "crt_user", "crt_name", "crt_host", "deleted")
SELECT 1, m."id", false, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', false
FROM "base_rbac_menu" m
WHERE m."id" IN (12000000, 12040000, 12040500, 12040501)
  -- 空库由 initAdminRoleMenu 统一初始化全部权限，避免提前插入导致其跳过。
  AND EXISTS (SELECT 1 FROM "base_rbac_role_menu" existing WHERE existing."role_id" = 1 AND existing."deleted" = false)
  AND NOT EXISTS (
      SELECT 1 FROM "base_rbac_role_menu" rm
      WHERE rm."role_id" = 1 AND rm."menu_id" = m."id" AND rm."deleted" = false
  );
