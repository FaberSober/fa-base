-- ------------------------- info -------------------------
-- @@ver: 1_000_025
-- @@info: 基础表增加租户字段，角色表增加类型字段，增加智能人事和租户菜单
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE tn_tenant (
    id varchar(32) NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(255) NOT NULL,
    short_name varchar(255) DEFAULT NULL,
    status tinyint DEFAULT ''1'' NOT NULL,
    expire_time TIMESTAMP DEFAULT NULL,
    contact_name varchar(255) DEFAULT NULL,
    contact_phone varchar(32) DEFAULT NULL,
    contact_email varchar(255) DEFAULT NULL,
    sort int DEFAULT ''0'' NOT NULL,
    description varchar(255) DEFAULT NULL,
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
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant' AND LOWER(INDEX_NAME) = 'uk_tn_tenant_code';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_tn_tenant_code ON tn_tenant (code)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_name';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_name ON tn_tenant (name)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_status';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_status ON tn_tenant (status)';
    END IF;
END;

COMMENT ON TABLE tn_tenant IS '租户表';
COMMENT ON COLUMN tn_tenant.id IS 'ID';
COMMENT ON COLUMN tn_tenant.code IS '租户编码';
COMMENT ON COLUMN tn_tenant.name IS '租户名称';
COMMENT ON COLUMN tn_tenant.short_name IS '租户简称';
COMMENT ON COLUMN tn_tenant.status IS '状态：1-启用/0-禁用';
COMMENT ON COLUMN tn_tenant.expire_time IS '到期时间';
COMMENT ON COLUMN tn_tenant.contact_name IS '联系人';
COMMENT ON COLUMN tn_tenant.contact_phone IS '联系电话';
COMMENT ON COLUMN tn_tenant.contact_email IS '联系邮箱';
COMMENT ON COLUMN tn_tenant.sort IS '排序';
COMMENT ON COLUMN tn_tenant.description IS '描述';
COMMENT ON COLUMN tn_tenant.crt_time IS '创建时间';
COMMENT ON COLUMN tn_tenant.crt_user IS '创建用户ID';
COMMENT ON COLUMN tn_tenant.crt_name IS '创建用户';
COMMENT ON COLUMN tn_tenant.crt_host IS '创建IP';
COMMENT ON COLUMN tn_tenant.upd_time IS '更新时间';
COMMENT ON COLUMN tn_tenant.upd_user IS '更新用户ID';
COMMENT ON COLUMN tn_tenant.upd_name IS '更新用户';
COMMENT ON COLUMN tn_tenant.upd_host IS '更新IP';
COMMENT ON COLUMN tn_tenant.deleted IS '是否删除';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_user';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE tn_tenant_user (
    id varchar(32) NOT NULL,
    tenant_id varchar(32) NOT NULL,
    user_id varchar(32) NOT NULL,
    is_admin tinyint DEFAULT ''0'' NOT NULL,
    status tinyint DEFAULT ''1'' NOT NULL,
    sort int DEFAULT ''0'' NOT NULL,
    description varchar(255) DEFAULT NULL,
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
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_user' AND LOWER(INDEX_NAME) = 'uk_tn_tenant_user';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_tn_tenant_user ON tn_tenant_user (tenant_id, user_id)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_user' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_user_tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_user_tenant_id ON tn_tenant_user (tenant_id)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_user' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_user_user_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_user_user_id ON tn_tenant_user (user_id)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_user' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_user_status';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_user_status ON tn_tenant_user (status)';
    END IF;
END;

COMMENT ON TABLE tn_tenant_user IS '租户用户关联表';
COMMENT ON COLUMN tn_tenant_user.id IS 'ID';
COMMENT ON COLUMN tn_tenant_user.tenant_id IS '租户ID';
COMMENT ON COLUMN tn_tenant_user.user_id IS '用户ID';
COMMENT ON COLUMN tn_tenant_user.is_admin IS '是否租户管理员：1-是/0-否';
COMMENT ON COLUMN tn_tenant_user.status IS '状态：1-启用/0-禁用';
COMMENT ON COLUMN tn_tenant_user.sort IS '排序';
COMMENT ON COLUMN tn_tenant_user.description IS '描述';
COMMENT ON COLUMN tn_tenant_user.crt_time IS '创建时间';
COMMENT ON COLUMN tn_tenant_user.crt_user IS '创建用户ID';
COMMENT ON COLUMN tn_tenant_user.crt_name IS '创建用户';
COMMENT ON COLUMN tn_tenant_user.crt_host IS '创建IP';
COMMENT ON COLUMN tn_tenant_user.upd_time IS '更新时间';
COMMENT ON COLUMN tn_tenant_user.upd_user IS '更新用户ID';
COMMENT ON COLUMN tn_tenant_user.upd_name IS '更新用户';
COMMENT ON COLUMN tn_tenant_user.upd_host IS '更新IP';
COMMENT ON COLUMN tn_tenant_user.deleted IS '是否删除';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_department' AND LOWER(COLUMN_NAME) = 'tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_department ADD tenant_id varchar(32) NULL';
    END IF;
END;

COMMENT ON COLUMN base_department.tenant_id IS '租户ID';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_role' AND LOWER(COLUMN_NAME) = 'type';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_rbac_role ADD type int NULL';
    END IF;
END;

COMMENT ON COLUMN base_rbac_role.type IS '类型：1全局超管/2全局/3租户';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_role' AND LOWER(COLUMN_NAME) = 'tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_rbac_role ADD tenant_id varchar(32) NULL';
    END IF;
END;

COMMENT ON COLUMN base_rbac_role.tenant_id IS '租户ID';

UPDATE base_rbac_role SET type = CASE WHEN id = 1 THEN 1 WHEN tenant_id IS NULL THEN 2 ELSE 3 END WHERE type IS NULL;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, level, icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted)
SELECT 21030040, 12000000, 1, '租户管理', 4, 1, 'mdi:office-building-cog', 1, 1, '/admin/system/tn', '2026-04-23 14:33:56', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:33:56', NULL, NULL, NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_rbac_menu WHERE id = 21030040);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, level, icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted)
SELECT 21030041, 21030040, 1, '租户管理', 0, 1, 'mdi:domain', 1, 1, '/admin/system/tn/tenant', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_rbac_menu WHERE id = 21030041);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, level, icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted)
SELECT 21030042, 21030040, 1, '租户用户管理', 1, 1, 'mdi:account-cog-outline', 1, 1, '/admin/system/tn/tenantUser', '2026-04-23 14:35:20', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:35:20', NULL, NULL, NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_rbac_menu WHERE id = 21030042);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, level, icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted)
SELECT 12010200, 12010000, 1, '部门管理', 1, 1, 'mdi:account-group-outline', 1, 1, '/admin/system/hr/department', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_rbac_menu WHERE id = 12010200);
SET IDENTITY_INSERT base_rbac_menu OFF;

SET IDENTITY_INSERT base_rbac_menu ON;
INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, level, icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted)
SELECT 12010400, 12010000, 1, '超级用户管理', 3, 1, 'mdi:account-supervisor-circle-outline', 1, 1, '/admin/system/hr/userSuper', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_rbac_menu WHERE id = 12010400);
SET IDENTITY_INSERT base_rbac_menu OFF;
