-- ------------------------- info -------------------------
-- @@ver: 1_000_011
-- @@info: 增加系统新闻表
-- ------------------------- info -------------------------
CREATE TABLE base_sys_news (
  id NUMBER(10) NOT NULL,
  title VARCHAR2(50) NOT NULL,
  content CLOB,
  cover VARCHAR2(32),
  author VARCHAR2(255),
  pub_time timestamp,
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

CREATE SEQUENCE base_sys_news_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_sys_news_bi BEFORE INSERT ON base_sys_news FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_sys_news_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_sys_news IS 'BASE-系统-新闻';

COMMENT ON COLUMN base_sys_news.id IS 'ID';

COMMENT ON COLUMN base_sys_news.title IS '标题';

COMMENT ON COLUMN base_sys_news.content IS '内容';

COMMENT ON COLUMN base_sys_news.cover IS '封面';

COMMENT ON COLUMN base_sys_news.author IS '作者';

COMMENT ON COLUMN base_sys_news.pub_time IS '发布时间';

COMMENT ON COLUMN base_sys_news.crt_time IS '创建时间';

COMMENT ON COLUMN base_sys_news.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_sys_news.crt_name IS '创建用户';

COMMENT ON COLUMN base_sys_news.crt_host IS '创建IP';

COMMENT ON COLUMN base_sys_news.upd_time IS '更新时间';

COMMENT ON COLUMN base_sys_news.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_sys_news.upd_name IS '更新用户';

COMMENT ON COLUMN base_sys_news.upd_host IS '更新IP';

COMMENT ON COLUMN base_sys_news.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_sys_news_upd BEFORE UPDATE ON base_sys_news FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;
