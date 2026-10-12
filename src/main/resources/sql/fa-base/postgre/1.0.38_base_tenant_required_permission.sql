-- ------------------------- info -------------------------
-- @@ver: 1_000_038
-- @@info: 增加平台必选租户权限标记
-- ------------------------- info -------------------------

ALTER TABLE "base_rbac_menu"
    ADD COLUMN IF NOT EXISTS "tenant_required" boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN "base_rbac_menu"."tenant_required" IS '租户必选权限';
