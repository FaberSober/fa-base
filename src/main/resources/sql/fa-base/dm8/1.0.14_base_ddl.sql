-- ------------------------- info -------------------------
-- @@ver: 1_000_014
-- @@info: 用户表增加字段：last_online_time最后在线时间
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user' AND LOWER(COLUMN_NAME) = 'last_online_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user ADD last_online_time timestamp NULL';
    END IF;
END;

COMMENT ON COLUMN base_user.last_online_time IS '最后在线时间';
