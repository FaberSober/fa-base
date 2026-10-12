-- ------------------------- info -------------------------
-- @@ver: 1_000_043
-- @@info: 扩展多客户端设备登记
-- ------------------------- info -------------------------

ALTER TABLE "base_user_device"
    ADD COLUMN IF NOT EXISTS "client_type" varchar(16) DEFAULT NULL;
COMMENT ON COLUMN "base_user_device"."client_type" IS '客户端类型';

ALTER TABLE "base_log_login"
    ADD COLUMN IF NOT EXISTS "client_type" varchar(16) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS "device_id" varchar(128) DEFAULT NULL;
COMMENT ON COLUMN "base_log_login"."client_type" IS '客户端类型';
COMMENT ON COLUMN "base_log_login"."device_id" IS '客户端实例ID';

CREATE INDEX IF NOT EXISTS "idx_base_user_device_client"
    ON "base_user_device" ("user_id", "client_type", "device_id");
