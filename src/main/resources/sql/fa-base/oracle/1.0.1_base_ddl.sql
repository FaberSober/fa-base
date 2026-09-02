-- ------------------------- info -------------------------
-- @@ver: 1_000_001
-- @@info: 增加BaseFileBiz通用业务附件表
-- ------------------------- info -------------------------
CREATE TABLE base_file_biz (
  id NUMBER(10) NOT NULL,
  main_biz_id VARCHAR2(32) NOT NULL,
  biz_id VARCHAR2(32) NOT NULL,
  type VARCHAR2(32),
  file_id VARCHAR2(32) NOT NULL,
  file_name VARCHAR2(255) NOT NULL,
  ext VARCHAR2(32) NOT NULL,
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

CREATE SEQUENCE base_file_biz_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_file_biz_bi BEFORE INSERT ON base_file_biz FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_file_biz_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_file_biz IS 'BASE-通用业务附件表';

COMMENT ON COLUMN base_file_biz.id IS 'ID';

COMMENT ON COLUMN base_file_biz.main_biz_id IS '主业务ID';

COMMENT ON COLUMN base_file_biz.biz_id IS '业务ID';

COMMENT ON COLUMN base_file_biz.type IS '业务类型';

COMMENT ON COLUMN base_file_biz.file_id IS '附件ID';

COMMENT ON COLUMN base_file_biz.file_name IS '附件名称';

COMMENT ON COLUMN base_file_biz.ext IS '文件扩展名';

COMMENT ON COLUMN base_file_biz.crt_time IS '创建时间';

COMMENT ON COLUMN base_file_biz.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_file_biz.crt_name IS '创建用户';

COMMENT ON COLUMN base_file_biz.crt_host IS '创建IP';

COMMENT ON COLUMN base_file_biz.upd_time IS '更新时间';

COMMENT ON COLUMN base_file_biz.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_file_biz.upd_name IS '更新用户';

COMMENT ON COLUMN base_file_biz.upd_host IS '更新IP';

COMMENT ON COLUMN base_file_biz.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_file_biz_upd BEFORE UPDATE ON base_file_biz FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;
