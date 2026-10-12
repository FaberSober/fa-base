-- ------------------------- info -------------------------
-- @@ver: 1_000_006
-- @@info: base_msg字段调整：buzz_type修改为字符串
-- ------------------------- info -------------------------

ALTER TABLE "base_msg" ALTER COLUMN "buzz_type" TYPE varchar(255);
ALTER TABLE "base_msg" ALTER COLUMN "buzz_type" DROP NOT NULL;
ALTER TABLE "base_msg" ALTER COLUMN "buzz_type" DROP DEFAULT;
COMMENT ON COLUMN "base_msg"."buzz_type" IS '业务类型';
