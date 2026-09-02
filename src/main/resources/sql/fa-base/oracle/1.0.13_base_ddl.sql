-- ------------------------- info -------------------------
-- @@ver: 1_000_013
-- @@info: 增加用户设备表
-- ------------------------- info -------------------------
CREATE TABLE base_user_device (
  id NUMBER(10) NOT NULL,
  user_id VARCHAR2(32) NOT NULL,
  device_id VARCHAR2(255) NOT NULL,
  model VARCHAR2(255),
  manufacturer VARCHAR2(255),
  os VARCHAR2(255),
  os_version VARCHAR2(255),
  enable NUMBER(1) NOT NULL,
  last_online_time timestamp,
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

CREATE SEQUENCE base_user_device_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_user_device_bi BEFORE INSERT ON base_user_device FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_user_device_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

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

CREATE OR REPLACE TRIGGER trg_base_user_device_upd BEFORE UPDATE ON base_user_device FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;
