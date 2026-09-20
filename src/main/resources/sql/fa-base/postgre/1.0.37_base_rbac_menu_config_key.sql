-- ------------------------- info -------------------------
-- @@ver: 1_000_037
-- @@info: 增加菜单跨环境同步标识并初始化历史菜单
-- ------------------------- info -------------------------

ALTER TABLE "base_rbac_menu"
    ADD COLUMN IF NOT EXISTS "config_key" varchar(64);

UPDATE "base_rbac_menu"
SET "config_key" = 'legacy:' || COALESCE("scope", 1)::text || ':' || "id"::text
WHERE "config_key" IS NULL OR "config_key" = '';

ALTER TABLE "base_rbac_menu"
    ALTER COLUMN "config_key" SET NOT NULL;

COMMENT ON COLUMN "base_rbac_menu"."config_key" IS '跨环境菜单配置标识';

CREATE UNIQUE INDEX IF NOT EXISTS "base_rbac_menu__uk_config_key"
    ON "base_rbac_menu" ("config_key");
