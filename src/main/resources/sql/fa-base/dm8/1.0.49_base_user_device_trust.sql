-- ------------------------- info -------------------------
-- @@ver: 1_000_049
-- @@info: 为用户设备增加信任状态字段
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(COLUMN_NAME) = 'trust_token_hash';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user_device ADD trust_token_hash char(64) DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_user_device.trust_token_hash IS '信任令牌哈希';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(COLUMN_NAME) = 'trusted_at';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user_device ADD trusted_at timestamp NULL DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_user_device.trusted_at IS '信任建立时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(COLUMN_NAME) = 'trust_expires_at';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user_device ADD trust_expires_at timestamp NULL DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_user_device.trust_expires_at IS '信任到期时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(COLUMN_NAME) = 'trust_revoked_at';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user_device ADD trust_revoked_at timestamp NULL DEFAULT NULL';
    END IF;
END;

COMMENT ON COLUMN base_user_device.trust_revoked_at IS '信任撤销时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device' AND LOWER(INDEX_NAME) = 'uk_base_user_device_trust_hash';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_user_device_trust_hash ON base_user_device (trust_token_hash)';
    END IF;
END;
