-- 单租户切换多租户：fa-base 历史数据迁移（PostgreSQL 18）
-- 前置：已执行 fa-base 1.0.25、1.0.36；已备份数据库；已停止业务写入；租户开关仍为关闭。
-- 说明：默认租户 ID 固定为 md5('fa-default-tenant')，脚本可重复执行。

BEGIN;

-- 1. 建立或恢复历史数据使用的默认租户。
INSERT INTO "tn_tenant"
("id", "code", "name", "short_name", "status", "sort", "description",
 "crt_user", "crt_name", "crt_host", "upd_user", "upd_name", "upd_host", "deleted")
VALUES
(md5('fa-default-tenant'), 'DEFAULT', '默认租户', '默认租户', true, 0,
 '单租户历史数据迁移默认租户', '1', '历史迁移', '127.0.0.1',
 '1', '历史迁移', '127.0.0.1', false)
ON CONFLICT ("code") DO UPDATE SET
  "status" = true,
  "deleted" = false,
  "upd_time" = CURRENT_TIMESTAMP,
  "upd_user" = '1',
  "upd_name" = '历史迁移',
  "upd_host" = '127.0.0.1';

-- 2. 规范历史角色范围：1=全局超管，未绑定租户的角色=全局角色。
--    已绑定租户的角色保持租户角色，不改写其 tenant_id。
UPDATE "base_rbac_role"
SET "type" = CASE
                 WHEN "id" = 1 THEN 1
                 WHEN "tenant_id" IS NULL OR "tenant_id" = '' THEN 2
                 ELSE 3
             END
WHERE "deleted" = false;

-- 3. 回填基础部门的默认租户。
UPDATE "base_department" d
SET "tenant_id" = t."id"
FROM "tn_tenant" t
WHERE t."code" = 'DEFAULT'
  AND t."deleted" = false
  AND (d."tenant_id" IS NULL OR d."tenant_id" = '')
  AND d."deleted" = false;

-- 4. 回填 fa-base Telemetry 历史事件的默认租户。
UPDATE "base_client_error_event" e
SET "tenant_id" = t."id"
FROM "tn_tenant" t
WHERE t."code" = 'DEFAULT'
  AND t."deleted" = false
  AND (e."tenant_id" IS NULL OR e."tenant_id" = '');

UPDATE "base_stat_event" e
SET "tenant_id" = t."id"
FROM "tn_tenant" t
WHERE t."code" = 'DEFAULT'
  AND t."deleted" = false
  AND (e."tenant_id" IS NULL OR e."tenant_id" = '');

-- 5. 将历史有效用户加入默认租户；保留已有 is_admin 值，不批量创建租户管理员。
INSERT INTO "tn_tenant_user"
("id", "tenant_id", "user_id", "is_admin", "status", "sort", "description",
 "crt_user", "crt_name", "crt_host", "upd_user", "upd_name", "upd_host", "deleted")
SELECT md5('fa-default-tenant-user:' || u."id"),
       t."id", u."id", false, true, 0, '单租户历史用户迁移',
       '1', '历史迁移', '127.0.0.1', '1', '历史迁移', '127.0.0.1', false
FROM "base_user" u
JOIN "tn_tenant" t ON t."code" = 'DEFAULT' AND t."deleted" = false
WHERE u."deleted" = false
ON CONFLICT ("tenant_id", "user_id") DO UPDATE SET
  "status" = true,
  "deleted" = false,
  "upd_time" = CURRENT_TIMESTAMP,
  "upd_user" = '1',
  "upd_name" = '历史迁移',
  "upd_host" = '127.0.0.1';

-- 6. 首次迁移时将默认租户权限初始化为当前平台有效权限 A。
--    若默认租户已有有效 C，则视为已人工配置，重复执行不会扩大权限范围。
INSERT INTO "tn_tenant_permission"
("tenant_id", "menu_id", "crt_user", "crt_name", "crt_host", "upd_user", "upd_name", "upd_host", "deleted")
SELECT t."id", m."id", '1', '历史迁移', '127.0.0.1', '1', '历史迁移', '127.0.0.1', false
FROM "tn_tenant" t
JOIN "base_rbac_menu" m ON m."deleted" = false AND m."status" = true
WHERE t."code" = 'DEFAULT'
  AND t."deleted" = false
  AND NOT EXISTS (
      SELECT 1
      FROM "tn_tenant_permission" p
      WHERE p."tenant_id" = t."id"
        AND p."deleted" = false
  )
ON CONFLICT ("tenant_id", "menu_id") DO UPDATE SET
  "deleted" = false,
  "upd_time" = CURRENT_TIMESTAMP,
  "upd_user" = '1',
  "upd_name" = '历史迁移',
  "upd_host" = '127.0.0.1';

COMMIT;

-- 最小验证：以下查询均应返回符合预期的结果。
SELECT "id", "code", "name", "status", "deleted"
FROM "tn_tenant"
WHERE "code" = 'DEFAULT';

SELECT COUNT(*) AS "missing_tenant_users"
FROM "base_user" u
WHERE u."deleted" = false
  AND NOT EXISTS (
      SELECT 1
      FROM "tn_tenant_user" tu
      JOIN "tn_tenant" t ON t."id" = tu."tenant_id" AND t."code" = 'DEFAULT' AND t."deleted" = false
      WHERE tu."user_id" = u."id" AND tu."deleted" = false AND tu."status" = true
  );

SELECT COUNT(*) AS "missing_department_tenant"
FROM "base_department"
WHERE "deleted" = false AND ("tenant_id" IS NULL OR "tenant_id" = '');

SELECT COUNT(*) AS "invalid_role_scope"
FROM "base_rbac_role"
WHERE "deleted" = false
  AND ("type" IS NULL
    OR ("type" = 1 AND "id" <> 1)
    OR ("type" = 2 AND "tenant_id" IS NOT NULL AND "tenant_id" <> '')
    OR ("type" = 3 AND ("tenant_id" IS NULL OR "tenant_id" = '')));

SELECT
  (SELECT COUNT(*) FROM "base_rbac_menu" WHERE "deleted" = false AND "status" = true) AS "platform_permission_count",
  (SELECT COUNT(*)
   FROM "tn_tenant_permission" p
   JOIN "tn_tenant" t ON t."id" = p."tenant_id" AND t."code" = 'DEFAULT' AND t."deleted" = false
   WHERE p."deleted" = false) AS "default_tenant_permission_count";
