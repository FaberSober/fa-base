-- ------------------------- info -------------------------
-- @@ver: 1_000_023
-- @@info: base_msg表增加字段：type消息来源类型，buzz_content业务JSON数据
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_msg' AND LOWER(COLUMN_NAME) = 'type';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_msg ADD type tinyint DEFAULT 1 NOT NULL';
    END IF;
END;

COMMENT ON COLUMN base_msg.type IS '消息来源：1-系统消息，2-流程消息';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_msg' AND LOWER(COLUMN_NAME) = 'buzz_content';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_msg ADD buzz_content CLOB NULL';
    END IF;
END;

COMMENT ON COLUMN base_msg.buzz_content IS '业务JSON数据';
