-- ------------------------- info -------------------------
-- @@ver: 1_000_047
-- @@info: 增加系统首次启动初始化状态
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS "base_system_bootstrap_state" (
  "state_key" varchar(64) PRIMARY KEY,
  "completed" boolean NOT NULL DEFAULT false
);
COMMENT ON TABLE "base_system_bootstrap_state" IS '系统首次启动初始化状态';
COMMENT ON COLUMN "base_system_bootstrap_state"."state_key" IS '初始化状态标识';
COMMENT ON COLUMN "base_system_bootstrap_state"."completed" IS '是否已完成';

INSERT INTO "base_system_bootstrap_state" ("state_key", "completed")
SELECT 'admin_role_menu', EXISTS (
    SELECT 1 FROM "base_rbac_role_menu" WHERE "role_id" = 1 AND "deleted" = false
)
ON CONFLICT ("state_key") DO NOTHING;

INSERT INTO "base_system_bootstrap_state" ("state_key", "completed")
SELECT 'default_tenant_access', EXISTS (
    SELECT 1
      FROM "tn_tenant" t
      JOIN "tn_tenant_user" tu ON tu."tenant_id" = t."id"
      JOIN "base_rbac_role" r ON r."tenant_id" = t."id" AND r."type" = 3
      JOIN "base_rbac_user_role" ur ON ur."role_id" = r."id" AND ur."user_id" = '1'
      JOIN "base_rbac_role_menu" rm ON rm."role_id" = r."id"
     WHERE t."code" = 'DEFAULT'
       AND t."deleted" = false
       AND tu."user_id" = '1' AND tu."is_admin" = true AND tu."status" = true AND tu."deleted" = false
       AND r."deleted" = false
       AND ur."deleted" = false
       AND rm."deleted" = false
)
ON CONFLICT ("state_key") DO NOTHING;
