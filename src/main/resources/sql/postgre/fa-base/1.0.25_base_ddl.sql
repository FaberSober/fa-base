-- ------------------------- info -------------------------
-- @@ver: 1_000_025
-- @@info: 基础表增加租户字段，角色表增加类型字段，增加智能人事和租户菜单
-- ------------------------- info -------------------------
-- ----------------------------
-- Table structure for tn_tenant
-- ----------------------------
CREATE TABLE IF NOT EXISTS "tn_tenant" (
  "id" varchar(32) NOT NULL,
  "code" varchar(64) NOT NULL,
  "name" varchar(255) NOT NULL,
  "short_name" varchar(255) DEFAULT NULL,
  "status" boolean NOT NULL DEFAULT true,
  "expire_time" timestamp DEFAULT NULL,
  "contact_name" varchar(255) DEFAULT NULL,
  "contact_phone" varchar(32) DEFAULT NULL,
  "contact_email" varchar(255) DEFAULT NULL,
  "sort" integer NOT NULL DEFAULT '0',
  "description" varchar(255) DEFAULT NULL,
  "crt_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "crt_user" varchar(32) NOT NULL,
  "crt_name" varchar(255) NOT NULL,
  "crt_host" varchar(255) DEFAULT NULL,
  "upd_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "upd_user" varchar(32) DEFAULT NULL,
  "upd_name" varchar(255) DEFAULT NULL,
  "upd_host" varchar(255) DEFAULT NULL,
  "deleted" boolean NOT NULL DEFAULT false,
  PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "tn_tenant__uk_tn_tenant_code" ON "tn_tenant" ("code");
CREATE INDEX IF NOT EXISTS "tn_tenant__idx_tn_tenant_name" ON "tn_tenant" ("name");
CREATE INDEX IF NOT EXISTS "tn_tenant__idx_tn_tenant_status" ON "tn_tenant" ("status");
COMMENT ON TABLE "tn_tenant" IS '租户表';
COMMENT ON COLUMN "tn_tenant"."id" IS 'ID';
COMMENT ON COLUMN "tn_tenant"."code" IS '租户编码';
COMMENT ON COLUMN "tn_tenant"."name" IS '租户名称';
COMMENT ON COLUMN "tn_tenant"."short_name" IS '租户简称';
COMMENT ON COLUMN "tn_tenant"."status" IS '状态：1-启用/0-禁用';
COMMENT ON COLUMN "tn_tenant"."expire_time" IS '到期时间';
COMMENT ON COLUMN "tn_tenant"."contact_name" IS '联系人';
COMMENT ON COLUMN "tn_tenant"."contact_phone" IS '联系电话';
COMMENT ON COLUMN "tn_tenant"."contact_email" IS '联系邮箱';
COMMENT ON COLUMN "tn_tenant"."sort" IS '排序';
COMMENT ON COLUMN "tn_tenant"."description" IS '描述';
COMMENT ON COLUMN "tn_tenant"."crt_time" IS '创建时间';
COMMENT ON COLUMN "tn_tenant"."crt_user" IS '创建用户ID';
COMMENT ON COLUMN "tn_tenant"."crt_name" IS '创建用户';
COMMENT ON COLUMN "tn_tenant"."crt_host" IS '创建IP';
COMMENT ON COLUMN "tn_tenant"."upd_time" IS '更新时间';
COMMENT ON COLUMN "tn_tenant"."upd_user" IS '更新用户ID';
COMMENT ON COLUMN "tn_tenant"."upd_name" IS '更新用户';
COMMENT ON COLUMN "tn_tenant"."upd_host" IS '更新IP';
COMMENT ON COLUMN "tn_tenant"."deleted" IS '是否删除';
DROP TRIGGER IF EXISTS "tn_tenant__upd_time" ON "tn_tenant";
CREATE TRIGGER "tn_tenant__upd_time" BEFORE UPDATE ON "tn_tenant" FOR EACH ROW EXECUTE FUNCTION fa_base_set_upd_time();

-- ----------------------------
-- Table structure for tn_tenant_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS "tn_tenant_user" (
  "id" varchar(32) NOT NULL,
  "tenant_id" varchar(32) NOT NULL,
  "user_id" varchar(32) NOT NULL,
  "is_admin" boolean NOT NULL DEFAULT false,
  "status" boolean NOT NULL DEFAULT true,
  "sort" integer NOT NULL DEFAULT '0',
  "description" varchar(255) DEFAULT NULL,
  "crt_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "crt_user" varchar(32) NOT NULL,
  "crt_name" varchar(255) NOT NULL,
  "crt_host" varchar(255) DEFAULT NULL,
  "upd_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "upd_user" varchar(32) DEFAULT NULL,
  "upd_name" varchar(255) DEFAULT NULL,
  "upd_host" varchar(255) DEFAULT NULL,
  "deleted" boolean NOT NULL DEFAULT false,
  PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS "tn_tenant_user__uk_tn_tenant_user" ON "tn_tenant_user" ("tenant_id", "user_id");
CREATE INDEX IF NOT EXISTS "tn_tenant_user__idx_tn_tenant_user_tenant_id" ON "tn_tenant_user" ("tenant_id");
CREATE INDEX IF NOT EXISTS "tn_tenant_user__idx_tn_tenant_user_user_id" ON "tn_tenant_user" ("user_id");
CREATE INDEX IF NOT EXISTS "tn_tenant_user__idx_tn_tenant_user_status" ON "tn_tenant_user" ("status");
COMMENT ON TABLE "tn_tenant_user" IS '租户用户关联表';
COMMENT ON COLUMN "tn_tenant_user"."id" IS 'ID';
COMMENT ON COLUMN "tn_tenant_user"."tenant_id" IS '租户ID';
COMMENT ON COLUMN "tn_tenant_user"."user_id" IS '用户ID';
COMMENT ON COLUMN "tn_tenant_user"."is_admin" IS '是否租户管理员：1-是/0-否';
COMMENT ON COLUMN "tn_tenant_user"."status" IS '状态：1-启用/0-禁用';
COMMENT ON COLUMN "tn_tenant_user"."sort" IS '排序';
COMMENT ON COLUMN "tn_tenant_user"."description" IS '描述';
COMMENT ON COLUMN "tn_tenant_user"."crt_time" IS '创建时间';
COMMENT ON COLUMN "tn_tenant_user"."crt_user" IS '创建用户ID';
COMMENT ON COLUMN "tn_tenant_user"."crt_name" IS '创建用户';
COMMENT ON COLUMN "tn_tenant_user"."crt_host" IS '创建IP';
COMMENT ON COLUMN "tn_tenant_user"."upd_time" IS '更新时间';
COMMENT ON COLUMN "tn_tenant_user"."upd_user" IS '更新用户ID';
COMMENT ON COLUMN "tn_tenant_user"."upd_name" IS '更新用户';
COMMENT ON COLUMN "tn_tenant_user"."upd_host" IS '更新IP';
COMMENT ON COLUMN "tn_tenant_user"."deleted" IS '是否删除';
DROP TRIGGER IF EXISTS "tn_tenant_user__upd_time" ON "tn_tenant_user";
CREATE TRIGGER "tn_tenant_user__upd_time" BEFORE UPDATE ON "tn_tenant_user" FOR EACH ROW EXECUTE FUNCTION fa_base_set_upd_time();

-- 部门表增加租户ID
ALTER TABLE "base_department" ADD COLUMN "tenant_id" varchar(32) NULL;
COMMENT ON COLUMN "base_department"."tenant_id" IS '租户ID';

-- 角色表增加类型和租户ID
ALTER TABLE "base_rbac_role" ADD COLUMN "type" integer NULL;
COMMENT ON COLUMN "base_rbac_role"."type" IS '类型：1全局超管/2全局/3租户';
ALTER TABLE "base_rbac_role" ADD COLUMN "tenant_id" varchar(32) NULL;
COMMENT ON COLUMN "base_rbac_role"."tenant_id" IS '租户ID';

-- 兼容历史角色数据：超管为全局超管，其余未绑定租户角色为全局角色
UPDATE "base_rbac_role" SET "type" = CASE WHEN "id" = 1 THEN 1 WHEN "tenant_id" IS NULL THEN 2 ELSE 3 END WHERE "type" IS NULL;

-- 租户相关菜单
INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (21030040, 12000000, 1, '租户管理', 4, 1, 'mdi:office-building-cog', true, 1, '/admin/system/tn', '2026-04-23 14:33:56', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:33:56', NULL, NULL, NULL, false);
INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (21030041, 21030040, 1, '租户管理', 0, 1, 'mdi:domain', true, 1, '/admin/system/tn/tenant', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:34:30', '1', '超级管理员', '192.168.5.57', false);
INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (21030042, 21030040, 1, '租户用户管理', 1, 1, 'mdi:account-cog-outline', true, 1, '/admin/system/tn/tenantUser', '2026-04-23 14:35:20', '1', '超级管理员', '192.168.5.57', '2026-04-23 14:35:20', NULL, NULL, NULL, false);

-- 智能人事相关菜单
INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (12010200, 12010000, 1, '部门管理', 1, 1, 'mdi:account-group-outline', true, 1, '/admin/system/hr/department', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, false);
INSERT INTO "base_rbac_menu" ("id", "parent_id", "scope", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (12010400, 12010000, 1, '超级用户管理', 3, 1, 'mdi:account-supervisor-circle-outline', true, 1, '/admin/system/hr/userSuper', '2026-07-06 13:58:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, false);

SELECT setval(pg_get_serial_sequence('base_rbac_menu', 'id'), COALESCE((SELECT MAX("id") FROM "base_rbac_menu"), 1), true);
