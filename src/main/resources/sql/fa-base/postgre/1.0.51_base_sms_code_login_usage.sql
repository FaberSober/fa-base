-- ------------------------- info -------------------------
-- @@ver: 1_000_051
-- @@info: 为短信登录验证码增加消费状态和失败次数
-- ------------------------- info -------------------------

ALTER TABLE "base_sms_code"
    ADD COLUMN IF NOT EXISTS "consumed" boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS "failed_attempts" integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN "base_sms_code"."consumed" IS '验证码是否已消费';
COMMENT ON COLUMN "base_sms_code"."failed_attempts" IS '验证码失败次数';
