-- ------------------------- info -------------------------
-- @@ver: 1_000_034
-- @@info: 增加统一工作日与交易日历基础表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_calendar';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_calendar (
    id bigint IDENTITY(1,1) NOT NULL,
    calendar_code varchar(32) NOT NULL,
    name varchar(64) NOT NULL,
    calendar_type varchar(16) NOT NULL,
    market varchar(16) DEFAULT NULL,
    timezone varchar(64) NOT NULL,
    enabled tinyint DEFAULT ''1'' NOT NULL,
    crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crt_user varchar(32) NOT NULL,
    crt_name varchar(255) NOT NULL,
    crt_host varchar(255) DEFAULT NULL,
    upd_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user varchar(32) DEFAULT NULL,
    upd_name varchar(255) DEFAULT NULL,
    upd_host varchar(255) DEFAULT NULL,
    deleted tinyint DEFAULT ''0'' NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_calendar' AND LOWER(INDEX_NAME) = 'uk_base_calendar_code';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_calendar_code ON base_calendar (calendar_code)';
    END IF;
END;

COMMENT ON TABLE base_calendar IS '统一工作日与交易日历定义';
COMMENT ON COLUMN base_calendar.id IS 'ID';
COMMENT ON COLUMN base_calendar.calendar_code IS '日历编码';
COMMENT ON COLUMN base_calendar.name IS '日历名称';
COMMENT ON COLUMN base_calendar.calendar_type IS '日历类型：OA/交易';
COMMENT ON COLUMN base_calendar.market IS '对应市场';
COMMENT ON COLUMN base_calendar.timezone IS '日期解释时区';
COMMENT ON COLUMN base_calendar.enabled IS '是否启用';
COMMENT ON COLUMN base_calendar.crt_time IS '创建时间';
COMMENT ON COLUMN base_calendar.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_calendar.crt_name IS '创建用户';
COMMENT ON COLUMN base_calendar.crt_host IS '创建IP';
COMMENT ON COLUMN base_calendar.upd_time IS '更新时间';
COMMENT ON COLUMN base_calendar.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_calendar.upd_name IS '更新用户';
COMMENT ON COLUMN base_calendar.upd_host IS '更新IP';
COMMENT ON COLUMN base_calendar.deleted IS '逻辑删除';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_calendar_day';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_calendar_day (
    id bigint IDENTITY(1,1) NOT NULL,
    calendar_code varchar(32) NOT NULL,
    calendar_date date NOT NULL,
    day_type varchar(24) NOT NULL,
    is_open tinyint NOT NULL,
    holiday_name varchar(128) DEFAULT NULL,
    source varchar(64) DEFAULT NULL,
    source_version varchar(64) DEFAULT NULL,
    remark varchar(512) DEFAULT NULL,
    crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crt_user varchar(32) NOT NULL,
    crt_name varchar(255) NOT NULL,
    crt_host varchar(255) DEFAULT NULL,
    upd_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user varchar(32) DEFAULT NULL,
    upd_name varchar(255) DEFAULT NULL,
    upd_host varchar(255) DEFAULT NULL,
    deleted tinyint DEFAULT ''0'' NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_calendar_day' AND LOWER(INDEX_NAME) = 'uk_base_calendar_day_code_date';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_calendar_day_code_date ON base_calendar_day (calendar_code, calendar_date)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_calendar_day' AND LOWER(INDEX_NAME) = 'idx_base_calendar_day_lookup';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_calendar_day_lookup ON base_calendar_day (calendar_code, calendar_date, is_open)';
    END IF;
END;

COMMENT ON TABLE base_calendar_day IS '统一日历单日事实';
COMMENT ON COLUMN base_calendar_day.id IS 'ID';
COMMENT ON COLUMN base_calendar_day.calendar_code IS '日历编码';
COMMENT ON COLUMN base_calendar_day.calendar_date IS '日历日期';
COMMENT ON COLUMN base_calendar_day.day_type IS '日期类型';
COMMENT ON COLUMN base_calendar_day.is_open IS '对所属日历是否开放';
COMMENT ON COLUMN base_calendar_day.holiday_name IS '节假日或特殊日期名称';
COMMENT ON COLUMN base_calendar_day.source IS '数据来源';
COMMENT ON COLUMN base_calendar_day.source_version IS '来源版本或导入批次';
COMMENT ON COLUMN base_calendar_day.remark IS '备注';
COMMENT ON COLUMN base_calendar_day.crt_time IS '创建时间';
COMMENT ON COLUMN base_calendar_day.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_calendar_day.crt_name IS '创建用户';
COMMENT ON COLUMN base_calendar_day.crt_host IS '创建IP';
COMMENT ON COLUMN base_calendar_day.upd_time IS '更新时间';
COMMENT ON COLUMN base_calendar_day.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_calendar_day.upd_name IS '更新用户';
COMMENT ON COLUMN base_calendar_day.upd_host IS '更新IP';
COMMENT ON COLUMN base_calendar_day.deleted IS '逻辑删除';

INSERT INTO base_calendar
    (calendar_code, name, calendar_type, market, timezone, enabled,
     crt_time, crt_user, crt_name, crt_host, deleted)
SELECT 'CN_OA', '中国 OA 工作日历', 'OA', NULL, 'Asia/Shanghai', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_calendar WHERE calendar_code = 'CN_OA');

INSERT INTO base_calendar
    (calendar_code, name, calendar_type, market, timezone, enabled,
     crt_time, crt_user, crt_name, crt_host, deleted)
SELECT 'CN_A_SHARE', '沪深 A 股交易日历', 'TRADING', 'SHSZ', 'Asia/Shanghai', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_calendar WHERE calendar_code = 'CN_A_SHARE');

INSERT INTO base_calendar
    (calendar_code, name, calendar_type, market, timezone, enabled,
     crt_time, crt_user, crt_name, crt_host, deleted)
SELECT 'HK_STOCK', '港股交易日历', 'TRADING', 'HK', 'Asia/Hong_Kong', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_calendar WHERE calendar_code = 'HK_STOCK');

INSERT INTO base_calendar
    (calendar_code, name, calendar_type, market, timezone, enabled,
     crt_time, crt_user, crt_name, crt_host, deleted)
SELECT 'US_STOCK', '美股交易日历（预留）', 'TRADING', 'US', 'America/New_York', 0,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_calendar WHERE calendar_code = 'US_STOCK');
