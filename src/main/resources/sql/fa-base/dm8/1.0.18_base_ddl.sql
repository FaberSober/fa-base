-- ------------------------- info -------------------------
-- @@ver: 1_000_018
-- @@info: base_log_api接口日志表增加字段：headers-请求头
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_log_api' AND LOWER(COLUMN_NAME) = 'headers';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_log_api ADD headers CLOB NULL';
    END IF;
END;

COMMENT ON COLUMN base_log_api.headers IS '请求头';
