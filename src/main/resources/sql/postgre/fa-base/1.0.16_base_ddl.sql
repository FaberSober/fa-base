-- ------------------------- info -------------------------
-- @@ver: 1_000_016
-- @@info: base_file_biz通用业务附件表增加sort排序字段
-- ------------------------- info -------------------------

ALTER TABLE "base_file_biz" ADD COLUMN "sort" integer NULL;
COMMENT ON COLUMN "base_file_biz"."sort" IS '排序';
