-- ------------------------- info -------------------------
-- @@ver: 1_000_030
-- @@info: 增加 Telemetry 应用、客户端异常、业务统计事件和每日聚合统计表及菜单
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_telemetry_app';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_telemetry_app (
    id bigint IDENTITY(1,1) NOT NULL,
    app_key varchar(64) NOT NULL,
    app_code varchar(64) NOT NULL,
    app_name varchar(128) NOT NULL,
    client_type varchar(16) NOT NULL,
    enabled tinyint DEFAULT ''1'' NOT NULL,
    remark varchar(500) DEFAULT NULL,
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
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_telemetry_app' AND LOWER(INDEX_NAME) = 'uk_base_telemetry_app_key';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_telemetry_app_key ON base_telemetry_app (app_key)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_telemetry_app' AND LOWER(INDEX_NAME) = 'uk_base_telemetry_app_code';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_telemetry_app_code ON base_telemetry_app (app_code)';
    END IF;
END;

COMMENT ON TABLE base_telemetry_app IS 'Telemetry 应用';
COMMENT ON COLUMN base_telemetry_app.id IS 'ID';
COMMENT ON COLUMN base_telemetry_app.app_key IS '客户端上报标识';
COMMENT ON COLUMN base_telemetry_app.app_code IS '应用编码';
COMMENT ON COLUMN base_telemetry_app.app_name IS '应用名称';
COMMENT ON COLUMN base_telemetry_app.client_type IS '客户端类型';
COMMENT ON COLUMN base_telemetry_app.enabled IS '是否允许上报';
COMMENT ON COLUMN base_telemetry_app.remark IS '备注';
COMMENT ON COLUMN base_telemetry_app.crt_time IS '创建时间';
COMMENT ON COLUMN base_telemetry_app.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_telemetry_app.crt_name IS '创建用户';
COMMENT ON COLUMN base_telemetry_app.crt_host IS '创建IP';
COMMENT ON COLUMN base_telemetry_app.upd_time IS '更新时间';
COMMENT ON COLUMN base_telemetry_app.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_telemetry_app.upd_name IS '更新用户';
COMMENT ON COLUMN base_telemetry_app.upd_host IS '更新IP';
COMMENT ON COLUMN base_telemetry_app.deleted IS '逻辑删除';

INSERT INTO base_telemetry_app (
  app_key, app_code, app_name, client_type, enabled, remark,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  'fa-admin-app-key', 'fa-admin-web', 'FA Admin 管理端', 'WEB', 1, '默认管理端遥测应用',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_telemetry_app
  WHERE app_key = 'fa-admin-app-key' OR app_code = 'fa-admin-web'
);

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_issue';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_client_error_issue (
    id bigint IDENTITY(1,1) NOT NULL,
    app_id bigint NOT NULL,
    client_type varchar(16) NOT NULL,
    fingerprint varchar(64) NOT NULL,
    title varchar(500) NOT NULL,
    error_type varchar(128) NOT NULL,
    status varchar(16) DEFAULT ''OPEN'' NOT NULL,
    first_seen_time TIMESTAMP NOT NULL,
    last_seen_time TIMESTAMP NOT NULL,
    event_count bigint DEFAULT ''0'' NOT NULL,
    user_count bigint DEFAULT ''0'' NOT NULL,
    latest_release varchar(128) DEFAULT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_issue' AND LOWER(INDEX_NAME) = 'uk_client_error_issue_fingerprint';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_client_error_issue_fingerprint ON base_client_error_issue (app_id, client_type, fingerprint)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_issue' AND LOWER(INDEX_NAME) = 'idx_client_error_issue_filter';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_client_error_issue_filter ON base_client_error_issue (app_id, client_type, status, last_seen_time)';
    END IF;
END;

COMMENT ON TABLE base_client_error_issue IS '客户端异常聚合Issue';
COMMENT ON COLUMN base_client_error_issue.id IS 'ID';
COMMENT ON COLUMN base_client_error_issue.app_id IS 'Telemetry应用ID';
COMMENT ON COLUMN base_client_error_issue.client_type IS '客户端类型';
COMMENT ON COLUMN base_client_error_issue.fingerprint IS '异常指纹';
COMMENT ON COLUMN base_client_error_issue.title IS '异常标题';
COMMENT ON COLUMN base_client_error_issue.error_type IS '异常类型';
COMMENT ON COLUMN base_client_error_issue.status IS 'Issue状态';
COMMENT ON COLUMN base_client_error_issue.first_seen_time IS '首次出现时间';
COMMENT ON COLUMN base_client_error_issue.last_seen_time IS '最后出现时间';
COMMENT ON COLUMN base_client_error_issue.event_count IS '异常事件数量';
COMMENT ON COLUMN base_client_error_issue.user_count IS '受影响用户数';
COMMENT ON COLUMN base_client_error_issue.latest_release IS '最新客户端版本';
COMMENT ON COLUMN base_client_error_issue.create_time IS '创建时间';
COMMENT ON COLUMN base_client_error_issue.update_time IS '更新时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_event';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_client_error_event (
    id bigint IDENTITY(1,1) NOT NULL,
    app_id bigint NOT NULL,
    issue_id bigint NOT NULL,
    client_type varchar(16) NOT NULL,
    environment varchar(16) NOT NULL,
    release varchar(128) NOT NULL,
    session_id varchar(64) NOT NULL,
    user_id varchar(64) DEFAULT NULL,
    tenant_id varchar(64) DEFAULT NULL,
    error_type varchar(128) NOT NULL,
    message varchar(2000) NOT NULL,
    stack CLOB,
    breadcrumbs CLOB DEFAULT NULL,
    "context" CLOB DEFAULT NULL,
    occur_time TIMESTAMP NOT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_event' AND LOWER(INDEX_NAME) = 'idx_client_error_event_issue_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_client_error_event_issue_time ON base_client_error_event (issue_id, occur_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_event' AND LOWER(INDEX_NAME) = 'idx_client_error_event_filter';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_client_error_event_filter ON base_client_error_event (app_id, client_type, environment, occur_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_client_error_event' AND LOWER(INDEX_NAME) = 'idx_client_error_event_user';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_client_error_event_user ON base_client_error_event (user_id)';
    END IF;
END;

COMMENT ON TABLE base_client_error_event IS '客户端异常事件';
COMMENT ON COLUMN base_client_error_event.id IS 'ID';
COMMENT ON COLUMN base_client_error_event.app_id IS 'Telemetry应用ID';
COMMENT ON COLUMN base_client_error_event.issue_id IS '异常Issue ID';
COMMENT ON COLUMN base_client_error_event.client_type IS '客户端类型';
COMMENT ON COLUMN base_client_error_event.environment IS '运行环境';
COMMENT ON COLUMN base_client_error_event.release IS '客户端版本';
COMMENT ON COLUMN base_client_error_event.session_id IS '客户端会话ID';
COMMENT ON COLUMN base_client_error_event.user_id IS '用户ID';
COMMENT ON COLUMN base_client_error_event.tenant_id IS '租户ID';
COMMENT ON COLUMN base_client_error_event.error_type IS '异常类型';
COMMENT ON COLUMN base_client_error_event.message IS '异常消息';
COMMENT ON COLUMN base_client_error_event.stack IS '异常堆栈';
COMMENT ON COLUMN base_client_error_event.breadcrumbs IS '异常前行为';
COMMENT ON COLUMN base_client_error_event."context" IS '客户端上下文';
COMMENT ON COLUMN base_client_error_event.occur_time IS '客户端发生时间';
COMMENT ON COLUMN base_client_error_event.create_time IS '服务端接收时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_event';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_stat_event (
    id bigint IDENTITY(1,1) NOT NULL,
    app_id bigint NOT NULL,
    client_type varchar(16) NOT NULL,
    environment varchar(16) NOT NULL,
    release varchar(128) NOT NULL,
    session_id varchar(64) NOT NULL,
    user_id varchar(64) DEFAULT NULL,
    tenant_id varchar(64) DEFAULT NULL,
    event_type varchar(32) NOT NULL,
    event_code varchar(128) NOT NULL,
    module varchar(128) DEFAULT NULL,
    biz_type varchar(128) DEFAULT NULL,
    biz_id varchar(128) DEFAULT NULL,
    result varchar(32) DEFAULT NULL,
    duration bigint DEFAULT NULL,
    properties CLOB DEFAULT NULL,
    "context" CLOB DEFAULT NULL,
    occur_time TIMESTAMP NOT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_event' AND LOWER(INDEX_NAME) = 'idx_stat_event_filter';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_stat_event_filter ON base_stat_event (app_id, client_type, environment, occur_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_event' AND LOWER(INDEX_NAME) = 'idx_stat_event_code_time';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_stat_event_code_time ON base_stat_event (event_code, occur_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_event' AND LOWER(INDEX_NAME) = 'idx_stat_event_user';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_stat_event_user ON base_stat_event (user_id, occur_time)';
    END IF;
END;

COMMENT ON TABLE base_stat_event IS 'Telemetry业务统计事件';
COMMENT ON COLUMN base_stat_event.id IS 'ID';
COMMENT ON COLUMN base_stat_event.app_id IS 'Telemetry应用ID';
COMMENT ON COLUMN base_stat_event.client_type IS '客户端类型';
COMMENT ON COLUMN base_stat_event.environment IS '运行环境';
COMMENT ON COLUMN base_stat_event.release IS '客户端版本';
COMMENT ON COLUMN base_stat_event.session_id IS '客户端会话ID';
COMMENT ON COLUMN base_stat_event.user_id IS '用户ID';
COMMENT ON COLUMN base_stat_event.tenant_id IS '租户ID';
COMMENT ON COLUMN base_stat_event.event_type IS '事件类型';
COMMENT ON COLUMN base_stat_event.event_code IS '事件编码';
COMMENT ON COLUMN base_stat_event.module IS '业务模块';
COMMENT ON COLUMN base_stat_event.biz_type IS '业务类型';
COMMENT ON COLUMN base_stat_event.biz_id IS '业务ID';
COMMENT ON COLUMN base_stat_event.result IS '业务结果';
COMMENT ON COLUMN base_stat_event.duration IS '耗时毫秒';
COMMENT ON COLUMN base_stat_event.properties IS '业务扩展属性';
COMMENT ON COLUMN base_stat_event."context" IS '客户端上下文';
COMMENT ON COLUMN base_stat_event.occur_time IS '发生时间';
COMMENT ON COLUMN base_stat_event.create_time IS '服务端接收时间';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_daily';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_stat_daily (
    id bigint IDENTITY(1,1) NOT NULL,
    stat_date date NOT NULL,
    app_id bigint NOT NULL,
    client_type varchar(16) NOT NULL,
    environment varchar(16) NOT NULL,
    event_type varchar(32) NOT NULL,
    event_code varchar(128) NOT NULL,
    module varchar(128) DEFAULT NULL,
    pv bigint DEFAULT 0 NOT NULL,
    uv bigint DEFAULT 0 NOT NULL,
    success_count bigint DEFAULT 0 NOT NULL,
    fail_count bigint DEFAULT 0 NOT NULL,
    avg_duration double DEFAULT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_daily' AND LOWER(INDEX_NAME) = 'idx_stat_daily_date_filter';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_stat_daily_date_filter ON base_stat_daily (stat_date, app_id, client_type, environment)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_stat_daily' AND LOWER(INDEX_NAME) = 'idx_stat_daily_code';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_stat_daily_code ON base_stat_daily (event_code, stat_date)';
    END IF;
END;

COMMENT ON TABLE base_stat_daily IS 'Telemetry每日聚合统计';
COMMENT ON COLUMN base_stat_daily.id IS 'ID';
COMMENT ON COLUMN base_stat_daily.stat_date IS '统计日期';
COMMENT ON COLUMN base_stat_daily.app_id IS 'Telemetry应用ID';
COMMENT ON COLUMN base_stat_daily.client_type IS '客户端类型';
COMMENT ON COLUMN base_stat_daily.environment IS '运行环境';
COMMENT ON COLUMN base_stat_daily.event_type IS '事件类型';
COMMENT ON COLUMN base_stat_daily.event_code IS '事件编码';
COMMENT ON COLUMN base_stat_daily.module IS '业务模块';
COMMENT ON COLUMN base_stat_daily.pv IS '事件次数';
COMMENT ON COLUMN base_stat_daily.uv IS '活跃用户数';
COMMENT ON COLUMN base_stat_daily.success_count IS '成功次数';
COMMENT ON COLUMN base_stat_daily.fail_count IS '失败次数';
COMMENT ON COLUMN base_stat_daily.avg_duration IS '平均耗时毫秒';
COMMENT ON COLUMN base_stat_daily.create_time IS '创建时间';
COMMENT ON COLUMN base_stat_daily.update_time IS '更新时间';

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  10030000, 12000000, 'Telemetry', 5, 1, 'mdi:chart-line', 1, 1, '/admin/system/telemetry',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030000 OR link_url = '/admin/system/telemetry'
);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  10030001, 10030000, '统计看板', 0, 1, NULL, 1, 1, '/admin/system/telemetry/dashboard',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030001 OR link_url = '/admin/system/telemetry/dashboard'
);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  10030002, 10030000, '异常 Issue', 1, 1, NULL, 1, 1, '/admin/system/telemetry/issue',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030002 OR link_url = '/admin/system/telemetry/issue'
);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  10030003, 10030000, '异常事件', 2, 1, NULL, 1, 1, '/admin/system/telemetry/event',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030003 OR link_url = '/admin/system/telemetry/event'
);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT
  10030004, 10030000, '业务事件', 3, 1, NULL, 1, 1, '/admin/system/telemetry/stat-event',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030004 OR link_url = '/admin/system/telemetry/stat-event'
);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (
  id, parent_id, name, sort, level, icon, status, link_type, link_url,
  crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted
)
SELECT 10030005, 10030000, '应用管理', 4, 1, NULL, 1, 1, '/admin/system/telemetry/app',
  CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM base_rbac_menu WHERE id = 10030005 OR link_url = '/admin/system/telemetry/app'
);
SET IDENTITY_INSERT base_rbac_menu OFF;
