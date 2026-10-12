-- ------------------------- info -------------------------
-- @@ver: 1_000_046
-- @@info: 系统公告租户隔离，历史公告保留空租户
-- ------------------------- info -------------------------

ALTER TABLE "base_notice"
    ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "base_notice"."tenant_id" IS '租户ID';

CREATE INDEX IF NOT EXISTS "idx_base_notice_tenant_id" ON "base_notice" ("tenant_id");
