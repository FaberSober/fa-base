-- ------------------------- info -------------------------
-- @@ver: 1_000_023
-- @@info: base_msg表增加字段：type消息来源类型，buzz_content业务JSON数据
-- ------------------------- info -------------------------
ALTER TABLE base_msg ADD type NUMBER(5) DEFAULT 1 NOT NULL;

COMMENT ON COLUMN base_msg.type IS '消息来源：1-系统消息，2-流程消息';

ALTER TABLE base_msg ADD buzz_content CLOB NULL;

COMMENT ON COLUMN base_msg.buzz_content IS '业务JSON数据';
