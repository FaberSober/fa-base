-- ------------------------- info -------------------------
-- @@ver: 1_000_045
-- @@info: 消息租户归属
-- ------------------------- info -------------------------

ALTER TABLE "base_msg"
    ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "base_msg"."tenant_id" IS '租户ID';

CREATE INDEX IF NOT EXISTS "idx_base_msg_to_tenant_read"
    ON "base_msg" ("to_user_id", "tenant_id", "is_read");
