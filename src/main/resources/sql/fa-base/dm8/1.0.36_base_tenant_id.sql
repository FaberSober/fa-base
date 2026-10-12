-- ------------------------- info -------------------------
-- @@ver: 1_000_036
-- @@info: 统一 Telemetry 租户ID字段类型，增加租户权限范围和查询索引
-- ------------------------- info -------------------------

ALTER TABLE base_client_error_event MODIFY tenant_id varchar(32) DEFAULT NULL;

COMMENT ON COLUMN base_client_error_event.tenant_id IS '租户ID';

ALTER TABLE base_stat_event MODIFY tenant_id varchar(32) DEFAULT NULL;

COMMENT ON COLUMN base_stat_event.tenant_id IS '租户ID';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_permission';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE tn_tenant_permission (
    id int IDENTITY(1,1) NOT NULL,
    tenant_id varchar(32) NOT NULL,
    menu_id int NOT NULL,
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
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_permission' AND LOWER(INDEX_NAME) = 'uk_tn_tenant_permission';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_tn_tenant_permission ON tn_tenant_permission (tenant_id, menu_id)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'tn_tenant_permission' AND LOWER(INDEX_NAME) = 'idx_tn_tenant_permission_menu_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tn_tenant_permission_menu_id ON tn_tenant_permission (menu_id)';
    END IF;
END;

COMMENT ON TABLE tn_tenant_permission IS '租户权限范围';
COMMENT ON COLUMN tn_tenant_permission.id IS 'ID';
COMMENT ON COLUMN tn_tenant_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN tn_tenant_permission.menu_id IS '权限ID';
COMMENT ON COLUMN tn_tenant_permission.crt_time IS '创建时间';
COMMENT ON COLUMN tn_tenant_permission.crt_user IS '创建用户ID';
COMMENT ON COLUMN tn_tenant_permission.crt_name IS '创建用户';
COMMENT ON COLUMN tn_tenant_permission.crt_host IS '创建IP';
COMMENT ON COLUMN tn_tenant_permission.upd_time IS '更新时间';
COMMENT ON COLUMN tn_tenant_permission.upd_user IS '更新用户ID';
COMMENT ON COLUMN tn_tenant_permission.upd_name IS '更新用户';
COMMENT ON COLUMN tn_tenant_permission.upd_host IS '更新IP';
COMMENT ON COLUMN tn_tenant_permission.deleted IS '是否删除';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_department' AND LOWER(INDEX_NAME) = 'idx_base_department_tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_department_tenant_id ON base_department (tenant_id)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_rbac_role' AND LOWER(INDEX_NAME) = 'idx_base_rbac_role_tenant_id';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_rbac_role_tenant_id ON base_rbac_role (tenant_id)';
    END IF;
END;
