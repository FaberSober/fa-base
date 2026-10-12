-- ------------------------- info -------------------------
-- @@ver: 1_000_026
-- @@info: Portal MVP 用户准入与咨询记录
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user' AND LOWER(COLUMN_NAME) = 'admin_enabled';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_user ADD admin_enabled TINYINT DEFAULT 0 NOT NULL';
        EXECUTE IMMEDIATE 'UPDATE base_user SET admin_enabled = 1 WHERE deleted = 0';
    END IF;
END;

COMMENT ON COLUMN base_user.admin_enabled IS '是否允许访问后台管理端';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'portal_contact_inquiry';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE portal_contact_inquiry (
    id bigint IDENTITY(1,1) NOT NULL,
    name varchar(100) NOT NULL,
    company varchar(200) DEFAULT NULL,
    tel varchar(32) NOT NULL,
    email varchar(255) DEFAULT NULL,
    subject varchar(200) NOT NULL,
    message CLOB NOT NULL,
    status tinyint DEFAULT ''0'' NOT NULL,
    source varchar(32) DEFAULT ''PORTAL'' NOT NULL,
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
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'portal_contact_inquiry' AND LOWER(INDEX_NAME) = 'idx_portal_contact_inquiry_status';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_portal_contact_inquiry_status ON portal_contact_inquiry (status, crt_time)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'portal_contact_inquiry' AND LOWER(INDEX_NAME) = 'idx_portal_contact_inquiry_tel';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_portal_contact_inquiry_tel ON portal_contact_inquiry (tel)';
    END IF;
END;

COMMENT ON TABLE portal_contact_inquiry IS 'Portal-官网咨询';
COMMENT ON COLUMN portal_contact_inquiry.id IS 'ID';
COMMENT ON COLUMN portal_contact_inquiry.name IS '联系人';
COMMENT ON COLUMN portal_contact_inquiry.company IS '公司名称';
COMMENT ON COLUMN portal_contact_inquiry.tel IS '联系电话';
COMMENT ON COLUMN portal_contact_inquiry.email IS '联系邮箱';
COMMENT ON COLUMN portal_contact_inquiry.subject IS '咨询主题';
COMMENT ON COLUMN portal_contact_inquiry.message IS '咨询内容';
COMMENT ON COLUMN portal_contact_inquiry.status IS '处理状态：0待处理/1处理中/2已完成';
COMMENT ON COLUMN portal_contact_inquiry.source IS '来源';
COMMENT ON COLUMN portal_contact_inquiry.crt_time IS '创建时间';
COMMENT ON COLUMN portal_contact_inquiry.crt_user IS '创建用户ID';
COMMENT ON COLUMN portal_contact_inquiry.crt_name IS '创建用户';
COMMENT ON COLUMN portal_contact_inquiry.crt_host IS '创建IP';
COMMENT ON COLUMN portal_contact_inquiry.upd_time IS '更新时间';
COMMENT ON COLUMN portal_contact_inquiry.upd_user IS '更新用户ID';
COMMENT ON COLUMN portal_contact_inquiry.upd_name IS '更新用户';
COMMENT ON COLUMN portal_contact_inquiry.upd_host IS '更新IP';
COMMENT ON COLUMN portal_contact_inquiry.deleted IS '是否删除';
