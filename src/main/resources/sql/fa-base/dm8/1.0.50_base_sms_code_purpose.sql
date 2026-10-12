-- ------------------------- info -------------------------
-- @@ver: 1_000_050
-- @@info: 为短信验证码增加用途隔离
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_sms_code' AND LOWER(COLUMN_NAME) = 'purpose';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_sms_code ADD purpose varchar(20) DEFAULT ''GENERAL'' NOT NULL';
    END IF;
END;

COMMENT ON COLUMN base_sms_code.purpose IS '验证码用途';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_sms_code' AND LOWER(INDEX_NAME) = 'idx_base_sms_code_phone_purpose_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_sms_code_phone_purpose_time ON base_sms_code (phone, purpose, crt_time)';
    END IF;
END;
