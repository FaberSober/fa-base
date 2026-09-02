-- ------------------------- info -------------------------
-- @@ver: 1_000_025
-- @@info: 基础表增加租户字段，角色表增加类型字段，增加智能人事和租户菜单
-- ------------------------- info -------------------------
CREATE TABLE tn_tenant (
  id VARCHAR2(32) NOT NULL,
  code VARCHAR2(64) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  short_name VARCHAR2(255),
  status NUMBER(1) DEFAULT 1 NOT NULL,
  expire_time timestamp,
  contact_name VARCHAR2(255),
  contact_phone VARCHAR2(32),
  contact_email VARCHAR2(255),
  sort NUMBER(10) DEFAULT '0' NOT NULL,
  description VARCHAR2(255),
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  crt_user VARCHAR2(32) NOT NULL,
  crt_name VARCHAR2(255) NOT NULL,
  crt_host VARCHAR2(255),
  upd_time timestamp DEFAULT CURRENT_TIMESTAMP,
  upd_user VARCHAR2(32),
  upd_name VARCHAR2(255),
  upd_host VARCHAR2(255),
  deleted NUMBER(1) DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX u_tn_tenant_91a2 ON tn_tenant (code);

CREATE INDEX i_tn_tenant_08ed ON tn_tenant (name);

CREATE INDEX i_tn_tenant_4990 ON tn_tenant (status);

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

CREATE OR REPLACE TRIGGER trg_tn_tenant_upd BEFORE UPDATE ON tn_tenant FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE tn_tenant_user (
  id VARCHAR2(32) NOT NULL,
  tenant_id VARCHAR2(32) NOT NULL,
  user_id VARCHAR2(32) NOT NULL,
  is_admin NUMBER(1) DEFAULT 0 NOT NULL,
  status NUMBER(1) DEFAULT 1 NOT NULL,
  sort NUMBER(10) DEFAULT '0' NOT NULL,
  description VARCHAR2(255),
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  crt_user VARCHAR2(32) NOT NULL,
  crt_name VARCHAR2(255) NOT NULL,
  crt_host VARCHAR2(255),
  upd_time timestamp DEFAULT CURRENT_TIMESTAMP,
  upd_user VARCHAR2(32),
  upd_name VARCHAR2(255),
  upd_host VARCHAR2(255),
  deleted NUMBER(1) DEFAULT 0 NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX u_tn_tenant_user_812a ON tn_tenant_user (tenant_id, user_id);

CREATE INDEX i_tn_tenant_user_457e ON tn_tenant_user (tenant_id);

CREATE INDEX i_tn_tenant_user_6e96 ON tn_tenant_user (user_id);

CREATE INDEX i_tn_tenant_user_5fa3 ON tn_tenant_user (status);

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

CREATE OR REPLACE TRIGGER trg_tn_tenant_user_upd BEFORE UPDATE ON tn_tenant_user FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

ALTER TABLE base_department ADD tenant_id VARCHAR2(32) NULL;

COMMENT ON COLUMN base_department.tenant_id IS '租户ID';

ALTER TABLE base_rbac_role ADD type NUMBER(10) NULL;

COMMENT ON COLUMN base_rbac_role.type IS '类型：1全局超管/2全局/3租户';

ALTER TABLE base_rbac_role ADD tenant_id VARCHAR2(32) NULL;

COMMENT ON COLUMN base_rbac_role.tenant_id IS '租户ID';

UPDATE base_rbac_role SET type = CASE WHEN id = 1 THEN 1 WHEN tenant_id IS NULL THEN 2 ELSE 3 END WHERE type IS NULL;

INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (21030040, 12000000, 1, '租户管理', 4, 1, 'mdi:office-building-cog', 1, 1, '/admin/system/tn', TIMESTAMP '2026-04-23 14:33:56', '1', '超级管理员', '192.168.5.57', TIMESTAMP '2026-04-23 14:33:56', NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (21030041, 21030040, 1, '租户管理', 0, 1, 'mdi:domain', 1, 1, '/admin/system/tn/tenant', TIMESTAMP '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', TIMESTAMP '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', 0);

INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (21030042, 21030040, 1, '租户用户管理', 1, 1, 'mdi:account-cog-outline', 1, 1, '/admin/system/tn/tenantUser', TIMESTAMP '2026-04-23 14:35:20', '1', '超级管理员', '192.168.5.57', TIMESTAMP '2026-04-23 14:35:20', NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12010200, 12010000, 1, '部门管理', 1, 1, 'mdi:account-group-outline', 1, 1, '/admin/system/hr/department', TIMESTAMP '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, scope, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12010400, 12010000, 1, '超级用户管理', 3, 1, 'mdi:account-supervisor-circle-outline', 1, 1, '/admin/system/hr/userSuper', TIMESTAMP '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
