-- ------------------------- info -------------------------
-- @@ver: 1_000_013
-- @@info: 增加用户设备表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_user_device';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_user_device (
    id int IDENTITY(1,1) NOT NULL,
    user_id varchar(32) NOT NULL,
    device_id varchar(255) NOT NULL,
    model varchar(255) DEFAULT NULL,
    manufacturer varchar(255) DEFAULT NULL,
    os varchar(255) DEFAULT NULL,
    os_version varchar(255) DEFAULT NULL,
    enable tinyint NOT NULL,
    last_online_time timestamp NULL DEFAULT NULL,
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

COMMENT ON TABLE base_user_device IS 'BASE-用户设备';
COMMENT ON COLUMN base_user_device.id IS 'ID';
COMMENT ON COLUMN base_user_device.user_id IS '所属用户ID';
COMMENT ON COLUMN base_user_device.device_id IS '设备ID';
COMMENT ON COLUMN base_user_device.model IS '设备型号';
COMMENT ON COLUMN base_user_device.manufacturer IS '设备厂商';
COMMENT ON COLUMN base_user_device.os IS '系统';
COMMENT ON COLUMN base_user_device.os_version IS '系统版本号';
COMMENT ON COLUMN base_user_device.enable IS '是否允许访问';
COMMENT ON COLUMN base_user_device.last_online_time IS '最后在线时间';
COMMENT ON COLUMN base_user_device.crt_time IS '创建时间';
COMMENT ON COLUMN base_user_device.crt_user IS '创建用户ID';
COMMENT ON COLUMN base_user_device.crt_name IS '创建用户';
COMMENT ON COLUMN base_user_device.crt_host IS '创建IP';
COMMENT ON COLUMN base_user_device.upd_time IS '更新时间';
COMMENT ON COLUMN base_user_device.upd_user IS '更新用户ID';
COMMENT ON COLUMN base_user_device.upd_name IS '更新用户';
COMMENT ON COLUMN base_user_device.upd_host IS '更新IP';
COMMENT ON COLUMN base_user_device.deleted IS '是否删除';
