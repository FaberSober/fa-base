-- 开发阶段手动更新 SQL（PostgreSQL）
-- 适用：已执行 1.0.37，但数据库中尚未增加 base_rbac_menu.tenant_required 的开发环境。
-- 新建数据库或已执行 1.0.38 的环境无需重复执行。

ALTER TABLE "base_rbac_menu"
    ADD COLUMN IF NOT EXISTS "tenant_required" boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN "base_rbac_menu"."tenant_required" IS '租户必选权限';
