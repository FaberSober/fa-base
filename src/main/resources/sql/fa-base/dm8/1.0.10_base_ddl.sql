-- ------------------------- info -------------------------
-- @@ver: 1_000_010
-- @@info: 增加预警表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_alert';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_alert (
    id int IDENTITY(1,1) NOT NULL,
    content varchar(50) NOT NULL,
    type varchar(255) NOT NULL,
    deal tinyint NOT NULL,
    duty_staff varchar(255) DEFAULT NULL,
    deal_staff varchar(255) DEFAULT NULL,
    deal_time timestamp NULL DEFAULT NULL,
    deal_desc varchar(255) DEFAULT NULL,
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

COMMENT ON TABLE base_alert IS 'BASE-告警信息';
COMMENT ON COLUMN base_alert.id IS 'ID';
COMMENT ON COLUMN base_alert.content IS '告警内容';
COMMENT ON COLUMN base_alert.type IS '告警类型';
COMMENT ON COLUMN base_alert.deal IS '是否处理';
COMMENT ON COLUMN base_alert.duty_staff IS '负责人';
COMMENT ON COLUMN base_alert.deal_staff IS '处理人';
COMMENT ON COLUMN base_alert.deal_time IS '处理时间';
COMMENT ON COLUMN base_alert.deal_desc IS '处理描述';
COMMENT ON COLUMN base_alert.crt_time IS '创建时间';
COMMENT ON COLUMN base_alert.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_alert.crt_name IS '创建用户';
COMMENT ON COLUMN base_alert.crt_host IS '创建IP';
COMMENT ON COLUMN base_alert.upd_time IS '更新时间';
COMMENT ON COLUMN base_alert.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_alert.upd_name IS '更新用户';
COMMENT ON COLUMN base_alert.upd_host IS '更新IP';
COMMENT ON COLUMN base_alert.deleted IS '是否删除';
