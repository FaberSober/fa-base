-- ------------------------- info -------------------------
-- @@ver: 1_000_051
-- @@info: 为短信登录验证码增加消费状态和失败次数
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_sms_code' AND LOWER(COLUMN_NAME) = 'consumed';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_sms_code ADD consumed tinyint DEFAULT 0 NOT NULL';
    END IF;
END;

COMMENT ON COLUMN base_sms_code.consumed IS '验证码是否已消费';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_sms_code' AND LOWER(COLUMN_NAME) = 'failed_attempts';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_sms_code ADD failed_attempts int DEFAULT 0 NOT NULL';
    END IF;
END;

COMMENT ON COLUMN base_sms_code.failed_attempts IS '验证码失败次数';
