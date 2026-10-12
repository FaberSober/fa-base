-- ------------------------- info -------------------------
-- @@ver: 1_000_027
-- @@info: 加固数据库升级日志与并发控制
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(COLUMN_NAME) = 'status';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_system_update_log ADD status INT DEFAULT 1 NOT NULL';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(COLUMN_NAME) = 'file_name';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_system_update_log ADD file_name VARCHAR(255)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(COLUMN_NAME) = 'checksum';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_system_update_log ADD checksum CHAR(64)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(COLUMN_NAME) = 'duration_ms';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_system_update_log ADD duration_ms BIGINT';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(COLUMN_NAME) = 'error_msg';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_system_update_log ADD error_msg CLOB';
    END IF;
END;

DELETE FROM base_system_update_log d
WHERE EXISTS (SELECT 1 FROM base_system_update_log n WHERE n.no = d.no AND n.ver = d.ver AND n.id > d.id);

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_update_log' AND LOWER(INDEX_NAME) = 'uk_base_system_update_log_no_ver';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_system_update_log_no_ver ON base_system_update_log (no, ver)';
    END IF;
END;
