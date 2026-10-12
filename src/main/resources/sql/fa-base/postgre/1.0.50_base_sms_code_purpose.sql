-- ------------------------- info -------------------------
-- @@ver: 1_000_050
-- @@info: 为短信验证码增加用途隔离
-- ------------------------- info -------------------------

ALTER TABLE "base_sms_code"
    ADD COLUMN IF NOT EXISTS "purpose" varchar(20) NOT NULL DEFAULT 'GENERAL';

CREATE INDEX IF NOT EXISTS "idx_base_sms_code_phone_purpose_time"
    ON "base_sms_code" ("phone", "purpose", "crt_time");

COMMENT ON COLUMN "base_sms_code"."purpose" IS '验证码用途';
