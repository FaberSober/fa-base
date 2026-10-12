-- ------------------------- info -------------------------
-- @@ver: 1_000_017
-- @@info: base_file_biz通用业务附件表增加out_url外部链接字段
-- ------------------------- info -------------------------

ALTER TABLE "base_file_save" ADD COLUMN "out_url" varchar(512) NULL;
COMMENT ON COLUMN "base_file_save"."out_url" IS '外部链接';
