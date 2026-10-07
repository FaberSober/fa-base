-- ------------------------- info -------------------------
-- @@ver: 1_000_049
-- @@info: 为用户设备增加信任状态字段
-- ------------------------- info -------------------------

ALTER TABLE "base_user_device"
    ADD COLUMN IF NOT EXISTS "trust_token_hash" char(64) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "trusted_at" timestamp NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "trust_expires_at" timestamp NULL DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "trust_revoked_at" timestamp NULL DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "uk_base_user_device_trust_hash"
    ON "base_user_device" ("trust_token_hash");

COMMENT ON COLUMN "base_user_device"."trust_token_hash" IS '信任令牌哈希';
COMMENT ON COLUMN "base_user_device"."trusted_at" IS '信任建立时间';
COMMENT ON COLUMN "base_user_device"."trust_expires_at" IS '信任到期时间';
COMMENT ON COLUMN "base_user_device"."trust_revoked_at" IS '信任撤销时间';
