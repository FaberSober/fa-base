-- ------------------------- info -------------------------
-- @@ver: 1_000_005
-- @@info: base_file_biz字段调整：biz_id非必填、type必填
-- ------------------------- info -------------------------

ALTER TABLE "base_file_biz" ALTER COLUMN "biz_id" TYPE varchar(32);
ALTER TABLE "base_file_biz" ALTER COLUMN "biz_id" DROP NOT NULL;
COMMENT ON COLUMN "base_file_biz"."biz_id" IS '业务ID';
ALTER TABLE "base_file_biz" ALTER COLUMN "type" TYPE varchar(32);
ALTER TABLE "base_file_biz" ALTER COLUMN "type" SET NOT NULL;
COMMENT ON COLUMN "base_file_biz"."type" IS '业务类型';
