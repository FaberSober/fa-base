-- ------------------------- info -------------------------
-- @@ver: 1_000_017
-- @@info: base_file_biz通用业务附件表增加out_url外部链接字段
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_file_save' AND LOWER(COLUMN_NAME) = 'out_url';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_file_save ADD out_url varchar(512) NULL';
    END IF;
END;

COMMENT ON COLUMN base_file_save.out_url IS '外部链接';
