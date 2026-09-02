-- ------------------------- info -------------------------
-- @@ver: 1_000_010
-- @@info: 增加预警表
-- ------------------------- info -------------------------
CREATE TABLE base_alert (
  id NUMBER(10) NOT NULL,
  content VARCHAR2(50) NOT NULL,
  type VARCHAR2(255) NOT NULL,
  deal NUMBER(1) NOT NULL,
  duty_staff VARCHAR2(255),
  deal_staff VARCHAR2(255),
  deal_time timestamp,
  deal_desc VARCHAR2(255),
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

CREATE SEQUENCE base_alert_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_alert_bi BEFORE INSERT ON base_alert FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_alert_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

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

CREATE OR REPLACE TRIGGER trg_base_alert_upd BEFORE UPDATE ON base_alert FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;
