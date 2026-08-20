-- ------------------------- info -------------------------
-- @@ver: 1_000_023
-- @@info: base_msg表增加字段：type消息来源类型，buzz_content业务JSON数据
-- ------------------------- info -------------------------
-- 增加type字段：消息来源1-系统消息\2-流程消息
ALTER TABLE "base_msg" ADD COLUMN "type" smallint NOT NULL DEFAULT 1;
COMMENT ON COLUMN "base_msg"."type" IS '消息来源：1-系统消息，2-流程消息';

-- 增加buzz_content字段：业务JSON数据，使用text进行存储
ALTER TABLE "base_msg" ADD COLUMN "buzz_content" text NULL;
COMMENT ON COLUMN "base_msg"."buzz_content" IS '业务JSON数据';
