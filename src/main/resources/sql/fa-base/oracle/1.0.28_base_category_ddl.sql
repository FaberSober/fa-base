-- ------------------------- info -------------------------
-- @@ver: 1_000_028
-- @@info: 增加通用业务分类树
-- ------------------------- info -------------------------
CREATE TABLE base_category (
  id NUMBER(10) NOT NULL,
  module VARCHAR2(32) NOT NULL,
  parent_id NUMBER(10) DEFAULT '0' NOT NULL,
  name VARCHAR2(64) NOT NULL,
  description VARCHAR2(256),
  sort NUMBER(10) DEFAULT '0' NOT NULL,
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

CREATE SEQUENCE base_category_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_category_bi BEFORE INSERT ON base_category FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_category_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

CREATE UNIQUE INDEX u_base_category_ce7f ON base_category (module, parent_id, name, deleted);

CREATE INDEX i_base_category_a968 ON base_category (module, parent_id, sort, deleted);

COMMENT ON TABLE base_category IS '通用业务分类';

COMMENT ON COLUMN base_category.id IS 'ID';

COMMENT ON COLUMN base_category.module IS '所属模块';

COMMENT ON COLUMN base_category.parent_id IS '上级分类ID，0为根节点';

COMMENT ON COLUMN base_category.name IS '分类名称';

COMMENT ON COLUMN base_category.description IS '描述';

COMMENT ON COLUMN base_category.sort IS '排序';

COMMENT ON COLUMN base_category.crt_time IS '创建时间';

COMMENT ON COLUMN base_category.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_category.crt_name IS '创建用户';

COMMENT ON COLUMN base_category.crt_host IS '创建IP';

COMMENT ON COLUMN base_category.upd_time IS '更新时间';

COMMENT ON COLUMN base_category.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_category.upd_name IS '更新用户';

COMMENT ON COLUMN base_category.upd_host IS '更新IP';

COMMENT ON COLUMN base_category.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_category_upd BEFORE UPDATE ON base_category FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;
