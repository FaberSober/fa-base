-- ------------------------- info -------------------------
-- @@ver: 1_000_016
-- @@info: base_file_biz通用业务附件表增加sort排序字段
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_file_biz' AND LOWER(COLUMN_NAME) = 'sort';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_file_biz ADD sort int NULL';
    END IF;
END;

COMMENT ON COLUMN base_file_biz.sort IS '排序';
