-- ------------------------- info -------------------------
-- @@ver: 1_000_044
-- @@info: 租户图标
-- ------------------------- info -------------------------

ALTER TABLE "tn_tenant"
    ADD COLUMN IF NOT EXISTS "icon" varchar(512) DEFAULT NULL;
COMMENT ON COLUMN "tn_tenant"."icon" IS '租户图标文件ID';
