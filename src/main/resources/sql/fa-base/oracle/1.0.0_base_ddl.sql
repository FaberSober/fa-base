-- ------------------------- info -------------------------
-- @@ver: 1_000_000
-- @@info: 初始化表结构
-- ------------------------- info -------------------------

CREATE TABLE base_area (
  id NUMBER(10) NOT NULL,
  "LEVEL" NUMBER(5) NOT NULL,
  parent_code NUMBER(19) DEFAULT '0' NOT NULL,
  area_code NUMBER(19) DEFAULT '0' NOT NULL,
  zip_code NUMBER(10) DEFAULT '000000' NOT NULL,
  city_code VARCHAR2(6) DEFAULT '' NOT NULL,
  name VARCHAR2(50) DEFAULT '' NOT NULL,
  short_name VARCHAR2(50) DEFAULT '' NOT NULL,
  merger_name VARCHAR2(50) DEFAULT '' NOT NULL,
  pinyin VARCHAR2(30) DEFAULT '' NOT NULL,
  lng NUMBER(10,6) DEFAULT '0.000000' NOT NULL,
  lat NUMBER(10,6) DEFAULT '0.000000' NOT NULL,
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_area_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_area_bi BEFORE INSERT ON base_area FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_area_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

CREATE UNIQUE INDEX u_base_area_8fa9 ON base_area (area_code);

CREATE INDEX i_base_area_2d01 ON base_area (parent_code);

CREATE INDEX i_base_area_b3a9 ON base_area ("LEVEL");

CREATE INDEX i_base_area_b292 ON base_area (name);

CREATE INDEX i_base_area_ce6b ON base_area ("LEVEL",name);

CREATE INDEX i_base_area_e4b2 ON base_area (short_name);

CREATE INDEX i_base_area_f95d ON base_area ("LEVEL",short_name);

COMMENT ON TABLE base_area IS '中国行政地区表';

COMMENT ON COLUMN base_area.id IS 'ID';

COMMENT ON COLUMN base_area."LEVEL" IS '层级';

COMMENT ON COLUMN base_area.parent_code IS '父级行政代码';

COMMENT ON COLUMN base_area.area_code IS '行政代码';

COMMENT ON COLUMN base_area.zip_code IS '邮政编码';

COMMENT ON COLUMN base_area.city_code IS '区号';

COMMENT ON COLUMN base_area.name IS '名称';

COMMENT ON COLUMN base_area.short_name IS '简称';

COMMENT ON COLUMN base_area.merger_name IS '组合名';

COMMENT ON COLUMN base_area.pinyin IS '拼音';

COMMENT ON COLUMN base_area.lng IS '经度';

COMMENT ON COLUMN base_area.lat IS '纬度';

CREATE TABLE base_config (
  id NUMBER(10) NOT NULL,
  biz VARCHAR2(255) NOT NULL,
  type VARCHAR2(255) NOT NULL,
  data CLOB NOT NULL,
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

CREATE SEQUENCE base_config_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_config_bi BEFORE INSERT ON base_config FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_config_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_config IS 'BASE-配置-通用';

COMMENT ON COLUMN base_config.id IS 'ID';

COMMENT ON COLUMN base_config.biz IS '业务模块';

COMMENT ON COLUMN base_config.type IS '配置类型';

COMMENT ON COLUMN base_config.data IS '配置JSON';

COMMENT ON COLUMN base_config.crt_time IS '创建时间';

COMMENT ON COLUMN base_config.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_config.crt_name IS '创建用户';

COMMENT ON COLUMN base_config.crt_host IS '创建IP';

COMMENT ON COLUMN base_config.upd_time IS '更新时间';

COMMENT ON COLUMN base_config.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_config.upd_name IS '更新用户';

COMMENT ON COLUMN base_config.upd_host IS '更新IP';

COMMENT ON COLUMN base_config.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_config_upd BEFORE UPDATE ON base_config FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_config_scene (
  id NUMBER(10) NOT NULL,
  biz VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  data CLOB NOT NULL,
  system NUMBER(1) DEFAULT 0 NOT NULL,
  default_scene NUMBER(1) DEFAULT 0 NOT NULL,
  hide NUMBER(1) DEFAULT 0 NOT NULL,
  sort NUMBER(10) NOT NULL,
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

CREATE SEQUENCE base_config_scene_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_config_scene_bi BEFORE INSERT ON base_config_scene FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_config_scene_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_config_scene IS 'BASE-配置-查询场景';

COMMENT ON COLUMN base_config_scene.id IS 'ID';

COMMENT ON COLUMN base_config_scene.biz IS '业务模块';

COMMENT ON COLUMN base_config_scene.name IS '场景名称';

COMMENT ON COLUMN base_config_scene.data IS '配置JSON';

COMMENT ON COLUMN base_config_scene.system IS '是否系统';

COMMENT ON COLUMN base_config_scene.default_scene IS '是否默认';

COMMENT ON COLUMN base_config_scene.hide IS '是否隐藏';

COMMENT ON COLUMN base_config_scene.sort IS '排序ID';

COMMENT ON COLUMN base_config_scene.crt_time IS '创建时间';

COMMENT ON COLUMN base_config_scene.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_config_scene.crt_name IS '创建用户';

COMMENT ON COLUMN base_config_scene.crt_host IS '创建IP';

COMMENT ON COLUMN base_config_scene.upd_time IS '更新时间';

COMMENT ON COLUMN base_config_scene.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_config_scene.upd_name IS '更新用户';

COMMENT ON COLUMN base_config_scene.upd_host IS '更新IP';

COMMENT ON COLUMN base_config_scene.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_config_scene_upd BEFORE UPDATE ON base_config_scene FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_config_sys (
  id NUMBER(10) NOT NULL,
  data CLOB NOT NULL,
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

CREATE SEQUENCE base_config_sys_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_config_sys_bi BEFORE INSERT ON base_config_sys FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_config_sys_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_config_sys IS 'BASE-配置-系统配置';

COMMENT ON COLUMN base_config_sys.id IS 'ID';

COMMENT ON COLUMN base_config_sys.data IS '配置JSON';

COMMENT ON COLUMN base_config_sys.crt_time IS '创建时间';

COMMENT ON COLUMN base_config_sys.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_config_sys.crt_name IS '创建用户';

COMMENT ON COLUMN base_config_sys.crt_host IS '创建IP';

COMMENT ON COLUMN base_config_sys.upd_time IS '更新时间';

COMMENT ON COLUMN base_config_sys.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_config_sys.upd_name IS '更新用户';

COMMENT ON COLUMN base_config_sys.upd_host IS '更新IP';

COMMENT ON COLUMN base_config_sys.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_config_sys_upd BEFORE UPDATE ON base_config_sys FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_department (
  id VARCHAR2(32) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  description VARCHAR2(255),
  parent_id VARCHAR2(32) NOT NULL,
  sort NUMBER(10) DEFAULT '1',
  type VARCHAR2(255),
  manager_id VARCHAR2(32),
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

COMMENT ON TABLE base_department IS 'BASE-部门';

COMMENT ON COLUMN base_department.id IS 'ID';

COMMENT ON COLUMN base_department.name IS '部门名称';

COMMENT ON COLUMN base_department.description IS '备注';

COMMENT ON COLUMN base_department.parent_id IS '父部门ID';

COMMENT ON COLUMN base_department.sort IS '排序';

COMMENT ON COLUMN base_department.type IS '类型';

COMMENT ON COLUMN base_department.manager_id IS '负责人ID';

COMMENT ON COLUMN base_department.crt_time IS '创建时间';

COMMENT ON COLUMN base_department.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_department.crt_name IS '创建用户';

COMMENT ON COLUMN base_department.crt_host IS '创建IP';

COMMENT ON COLUMN base_department.upd_time IS '更新时间';

COMMENT ON COLUMN base_department.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_department.upd_name IS '更新用户';

COMMENT ON COLUMN base_department.upd_host IS '更新IP';

COMMENT ON COLUMN base_department.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_department_upd BEFORE UPDATE ON base_department FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_dict (
  id NUMBER(10) NOT NULL,
  code VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  parent_id NUMBER(10) NOT NULL,
  sort_id NUMBER(10) DEFAULT '0',
  description VARCHAR2(255),
  options CLOB,
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

CREATE SEQUENCE base_dict_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_dict_bi BEFORE INSERT ON base_dict FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_dict_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_dict IS 'BASE-字典分类';

COMMENT ON COLUMN base_dict.id IS 'ID';

COMMENT ON COLUMN base_dict.code IS '编码';

COMMENT ON COLUMN base_dict.name IS '名称';

COMMENT ON COLUMN base_dict.parent_id IS '上级节点';

COMMENT ON COLUMN base_dict.sort_id IS '排序ID';

COMMENT ON COLUMN base_dict.description IS '描述';

COMMENT ON COLUMN base_dict.options IS '字典数组{value:1,label:名称,deleted:是否删除}';

COMMENT ON COLUMN base_dict.crt_time IS '创建时间';

COMMENT ON COLUMN base_dict.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_dict.crt_name IS '创建用户';

COMMENT ON COLUMN base_dict.crt_host IS '创建IP';

COMMENT ON COLUMN base_dict.upd_time IS '更新时间';

COMMENT ON COLUMN base_dict.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_dict.upd_name IS '更新用户';

COMMENT ON COLUMN base_dict.upd_host IS '更新IP';

COMMENT ON COLUMN base_dict.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_dict_upd BEFORE UPDATE ON base_dict FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_entity_log (
  id NUMBER(10) NOT NULL,
  biz_type VARCHAR2(255) NOT NULL,
  biz_id VARCHAR2(255) NOT NULL,
  action NUMBER(5) NOT NULL,
  content CLOB,
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  crt_user VARCHAR2(32) NOT NULL,
  crt_name VARCHAR2(255) NOT NULL,
  crt_host VARCHAR2(255),
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_entity_log_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_entity_log_bi BEFORE INSERT ON base_entity_log FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_entity_log_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_entity_log IS 'BASE-实体变更日志';

COMMENT ON COLUMN base_entity_log.id IS 'ID';

COMMENT ON COLUMN base_entity_log.biz_type IS '业务类型';

COMMENT ON COLUMN base_entity_log.biz_id IS '业务ID';

COMMENT ON COLUMN base_entity_log.action IS '动作1新增/2更新/3删除';

COMMENT ON COLUMN base_entity_log.content IS '动作内容';

COMMENT ON COLUMN base_entity_log.crt_time IS '创建时间';

COMMENT ON COLUMN base_entity_log.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_entity_log.crt_name IS '创建用户';

COMMENT ON COLUMN base_entity_log.crt_host IS '创建IP';

CREATE TABLE base_file_save (
  id VARCHAR2(32) NOT NULL,
  url VARCHAR2(512) NOT NULL,
  "size" NUMBER(19) NOT NULL,
  filename VARCHAR2(255) NOT NULL,
  original_filename VARCHAR2(255) NOT NULL,
  base_path VARCHAR2(255) NOT NULL,
  path VARCHAR2(255) NOT NULL,
  ext VARCHAR2(32) NOT NULL,
  content_type VARCHAR2(255) NOT NULL,
  platform VARCHAR2(32) NOT NULL,
  th_url VARCHAR2(512),
  th_filename VARCHAR2(255),
  th_size NUMBER(19),
  th_content_type VARCHAR2(32),
  object_id VARCHAR2(32),
  object_type VARCHAR2(32),
  attr CLOB,
  md5 VARCHAR2(32),
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

COMMENT ON TABLE base_file_save IS 'BASE-用户文件表';

COMMENT ON COLUMN base_file_save.id IS 'ID';

COMMENT ON COLUMN base_file_save.url IS '文件访问地址';

COMMENT ON COLUMN base_file_save."size" IS '文件大小，单位字节';

COMMENT ON COLUMN base_file_save.filename IS '文件名';

COMMENT ON COLUMN base_file_save.original_filename IS '原始文件名';

COMMENT ON COLUMN base_file_save.base_path IS '基础存储路径';

COMMENT ON COLUMN base_file_save.path IS '存储路径';

COMMENT ON COLUMN base_file_save.ext IS '文件扩展名';

COMMENT ON COLUMN base_file_save.content_type IS 'MIME类型';

COMMENT ON COLUMN base_file_save.platform IS '存储平台';

COMMENT ON COLUMN base_file_save.th_url IS '缩略图访问路径';

COMMENT ON COLUMN base_file_save.th_filename IS '缩略图名称';

COMMENT ON COLUMN base_file_save.th_size IS '缩略图大小，单位字节';

COMMENT ON COLUMN base_file_save.th_content_type IS '缩略图MIME类型';

COMMENT ON COLUMN base_file_save.object_id IS '文件所属对象id';

COMMENT ON COLUMN base_file_save.object_type IS '文件所属对象类型，例如用户头像，评价图片';

COMMENT ON COLUMN base_file_save.attr IS '附加属性';

COMMENT ON COLUMN base_file_save.md5 IS '文件MD5';

COMMENT ON COLUMN base_file_save.crt_time IS '创建时间';

COMMENT ON COLUMN base_file_save.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_file_save.crt_name IS '创建用户';

COMMENT ON COLUMN base_file_save.crt_host IS '创建IP';

COMMENT ON COLUMN base_file_save.upd_time IS '更新时间';

COMMENT ON COLUMN base_file_save.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_file_save.upd_name IS '更新用户';

COMMENT ON COLUMN base_file_save.upd_host IS '更新IP';

COMMENT ON COLUMN base_file_save.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_file_save_upd BEFORE UPDATE ON base_file_save FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_job (
  id NUMBER(10) NOT NULL,
  job_name VARCHAR2(255) NOT NULL,
  cron VARCHAR2(255) NOT NULL,
  status NUMBER(1) NOT NULL,
  clazz_path VARCHAR2(255) NOT NULL,
  job_desc VARCHAR2(255),
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

CREATE SEQUENCE base_job_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_job_bi BEFORE INSERT ON base_job FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_job_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_job IS 'BASE-系统定时任务';

COMMENT ON COLUMN base_job.id IS 'ID';

COMMENT ON COLUMN base_job.job_name IS '任务名称';

COMMENT ON COLUMN base_job.cron IS 'cron表达式';

COMMENT ON COLUMN base_job.status IS '状态:0未启动false/1启动true';

COMMENT ON COLUMN base_job.clazz_path IS '任务执行方法';

COMMENT ON COLUMN base_job.job_desc IS '任务描述';

COMMENT ON COLUMN base_job.crt_time IS '创建时间';

COMMENT ON COLUMN base_job.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_job.crt_name IS '创建用户';

COMMENT ON COLUMN base_job.crt_host IS '创建IP';

COMMENT ON COLUMN base_job.upd_time IS '更新时间';

COMMENT ON COLUMN base_job.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_job.upd_name IS '更新用户';

COMMENT ON COLUMN base_job.upd_host IS '更新IP';

COMMENT ON COLUMN base_job.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_job_upd BEFORE UPDATE ON base_job FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_job_log (
  id NUMBER(10) NOT NULL,
  job_id NUMBER(10) NOT NULL,
  begin_time timestamp NOT NULL,
  end_time timestamp,
  status NUMBER(5) NOT NULL,
  duration NUMBER(10) NOT NULL,
  err_msg CLOB,
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_job_log_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_job_log_bi BEFORE INSERT ON base_job_log FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_job_log_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_job_log IS 'BASE-定时任务日志';

COMMENT ON COLUMN base_job_log.id IS 'ID';

COMMENT ON COLUMN base_job_log.job_id IS '任务ID';

COMMENT ON COLUMN base_job_log.begin_time IS '创建时间';

COMMENT ON COLUMN base_job_log.end_time IS '结束时间';

COMMENT ON COLUMN base_job_log.status IS '执行结果：1-执行中/2-成功/9-失败';

COMMENT ON COLUMN base_job_log.duration IS '执行花费时间';

COMMENT ON COLUMN base_job_log.err_msg IS '错误日志';

CREATE TABLE base_log_api (
  id NUMBER(10) NOT NULL,
  biz VARCHAR2(255),
  opr VARCHAR2(255),
  crud VARCHAR2(1),
  url CLOB NOT NULL,
  method VARCHAR2(10) NOT NULL,
  agent CLOB,
  os VARCHAR2(255),
  browser VARCHAR2(255),
  version VARCHAR2(255),
  fa_from VARCHAR2(255),
  version_code NUMBER(19),
  version_name VARCHAR2(255),
  mobile NUMBER(1),
  duration NUMBER(10) NOT NULL,
  pro VARCHAR2(10),
  city VARCHAR2(10),
  addr VARCHAR2(255),
  request CLOB,
  req_size NUMBER(10),
  response CLOB,
  ret_size NUMBER(10),
  ret_status NUMBER(10) NOT NULL,
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP,
  crt_user VARCHAR2(32),
  crt_name VARCHAR2(255),
  crt_host VARCHAR2(255),
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_log_api_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_log_api_bi BEFORE INSERT ON base_log_api FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_log_api_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_log_api IS 'BASE-URL请求日志';

COMMENT ON COLUMN base_log_api.id IS 'ID';

COMMENT ON COLUMN base_log_api.biz IS '模块';

COMMENT ON COLUMN base_log_api.opr IS '操作';

COMMENT ON COLUMN base_log_api.crud IS 'CRUD类型';

COMMENT ON COLUMN base_log_api.url IS '请求URL';

COMMENT ON COLUMN base_log_api.method IS '请求类型';

COMMENT ON COLUMN base_log_api.agent IS '访问客户端';

COMMENT ON COLUMN base_log_api.os IS '操作系统';

COMMENT ON COLUMN base_log_api.browser IS '浏览器';

COMMENT ON COLUMN base_log_api.version IS '浏览器版本';

COMMENT ON COLUMN base_log_api.fa_from IS '客户端来源';

COMMENT ON COLUMN base_log_api.version_code IS '客户端版本号';

COMMENT ON COLUMN base_log_api.version_name IS '客户端版本名';

COMMENT ON COLUMN base_log_api.mobile IS '是否为移动终端';

COMMENT ON COLUMN base_log_api.duration IS '请求花费时间';

COMMENT ON COLUMN base_log_api.pro IS '省';

COMMENT ON COLUMN base_log_api.city IS '市';

COMMENT ON COLUMN base_log_api.addr IS '地址';

COMMENT ON COLUMN base_log_api.request IS '请求内容';

COMMENT ON COLUMN base_log_api.req_size IS '请求体大小';

COMMENT ON COLUMN base_log_api.response IS '返回内容';

COMMENT ON COLUMN base_log_api.ret_size IS '返回内容大小';

COMMENT ON COLUMN base_log_api.ret_status IS '返回码';

COMMENT ON COLUMN base_log_api.crt_time IS '创建时间';

COMMENT ON COLUMN base_log_api.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_log_api.crt_name IS '创建用户';

COMMENT ON COLUMN base_log_api.crt_host IS '创建IP';

CREATE TABLE base_log_login (
  id NUMBER(10) NOT NULL,
  agent CLOB,
  os VARCHAR2(255),
  browser VARCHAR2(255),
  version VARCHAR2(255),
  mobile NUMBER(1),
  pro VARCHAR2(10),
  city VARCHAR2(10),
  addr VARCHAR2(255),
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

CREATE SEQUENCE base_log_login_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_log_login_bi BEFORE INSERT ON base_log_login FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_log_login_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_log_login IS 'BASE-登录日志';

COMMENT ON COLUMN base_log_login.id IS 'ID';

COMMENT ON COLUMN base_log_login.agent IS '访问客户端';

COMMENT ON COLUMN base_log_login.os IS '操作系统';

COMMENT ON COLUMN base_log_login.browser IS '浏览器';

COMMENT ON COLUMN base_log_login.version IS '浏览器版本';

COMMENT ON COLUMN base_log_login.mobile IS '是否为移动终端';

COMMENT ON COLUMN base_log_login.pro IS '省';

COMMENT ON COLUMN base_log_login.city IS '市';

COMMENT ON COLUMN base_log_login.addr IS '地址';

COMMENT ON COLUMN base_log_login.crt_time IS '创建时间';

COMMENT ON COLUMN base_log_login.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_log_login.crt_name IS '创建用户';

COMMENT ON COLUMN base_log_login.crt_host IS '创建IP';

COMMENT ON COLUMN base_log_login.upd_time IS '更新时间';

COMMENT ON COLUMN base_log_login.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_log_login.upd_name IS '更新用户';

COMMENT ON COLUMN base_log_login.upd_host IS '更新IP';

COMMENT ON COLUMN base_log_login.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_log_login_upd BEFORE UPDATE ON base_log_login FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_msg (
  id NUMBER(10) NOT NULL,
  from_user_name VARCHAR2(255) NOT NULL,
  from_user_id VARCHAR2(32) NOT NULL,
  to_user_name VARCHAR2(255) NOT NULL,
  to_user_id VARCHAR2(32) NOT NULL,
  content CLOB,
  is_read NUMBER(1) DEFAULT 0 NOT NULL,
  read_time timestamp,
  buzz_type NUMBER(10),
  buzz_id VARCHAR2(255),
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

CREATE SEQUENCE base_msg_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_msg_bi BEFORE INSERT ON base_msg FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_msg_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_msg IS 'BASE-消息';

COMMENT ON COLUMN base_msg.id IS 'ID';

COMMENT ON COLUMN base_msg.from_user_name IS '来源用户';

COMMENT ON COLUMN base_msg.from_user_id IS '来源用户ID';

COMMENT ON COLUMN base_msg.to_user_name IS '接收用户';

COMMENT ON COLUMN base_msg.to_user_id IS '接收用户ID';

COMMENT ON COLUMN base_msg.content IS '消息内容';

COMMENT ON COLUMN base_msg.is_read IS '是否已读';

COMMENT ON COLUMN base_msg.read_time IS '已读时间';

COMMENT ON COLUMN base_msg.buzz_type IS '业务类型';

COMMENT ON COLUMN base_msg.buzz_id IS '业务ID';

COMMENT ON COLUMN base_msg.crt_time IS '创建时间';

COMMENT ON COLUMN base_msg.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_msg.crt_name IS '创建用户';

COMMENT ON COLUMN base_msg.crt_host IS '创建IP';

COMMENT ON COLUMN base_msg.upd_time IS '更新时间';

COMMENT ON COLUMN base_msg.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_msg.upd_name IS '更新用户';

COMMENT ON COLUMN base_msg.upd_host IS '更新IP';

COMMENT ON COLUMN base_msg.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_msg_upd BEFORE UPDATE ON base_msg FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_notice (
  id NUMBER(10) NOT NULL,
  title VARCHAR2(50) NOT NULL,
  content VARCHAR2(255) NOT NULL,
  status NUMBER(1) DEFAULT 1 NOT NULL,
  strong_notice NUMBER(1) DEFAULT 0 NOT NULL,
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

CREATE SEQUENCE base_notice_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_notice_bi BEFORE INSERT ON base_notice FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_notice_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_notice IS 'BASE-通知与公告';

COMMENT ON COLUMN base_notice.id IS 'ID';

COMMENT ON COLUMN base_notice.title IS '标题';

COMMENT ON COLUMN base_notice.content IS '内容';

COMMENT ON COLUMN base_notice.status IS '是否有效';

COMMENT ON COLUMN base_notice.strong_notice IS '是否强提醒';

COMMENT ON COLUMN base_notice.crt_time IS '创建时间';

COMMENT ON COLUMN base_notice.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_notice.crt_name IS '创建用户';

COMMENT ON COLUMN base_notice.crt_host IS '创建IP';

COMMENT ON COLUMN base_notice.upd_time IS '更新时间';

COMMENT ON COLUMN base_notice.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_notice.upd_name IS '更新用户';

COMMENT ON COLUMN base_notice.upd_host IS '更新IP';

COMMENT ON COLUMN base_notice.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_notice_upd BEFORE UPDATE ON base_notice FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_rbac_menu (
  id NUMBER(10) NOT NULL,
  parent_id NUMBER(10) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  sort NUMBER(10) NOT NULL,
  "LEVEL" NUMBER(5) NOT NULL,
  icon VARCHAR2(255),
  status NUMBER(1) NOT NULL,
  link_type NUMBER(5) NOT NULL,
  link_url VARCHAR2(255) NOT NULL,
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

CREATE SEQUENCE base_rbac_menu_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_rbac_menu_bi BEFORE INSERT ON base_rbac_menu FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_rbac_menu_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_rbac_menu IS 'BASE-菜单表';

COMMENT ON COLUMN base_rbac_menu.id IS 'ID';

COMMENT ON COLUMN base_rbac_menu.parent_id IS '父级ID';

COMMENT ON COLUMN base_rbac_menu.name IS '名称';

COMMENT ON COLUMN base_rbac_menu.sort IS '排序';

COMMENT ON COLUMN base_rbac_menu."LEVEL" IS '菜单等级：0-模块/1-菜单/9-按钮';

COMMENT ON COLUMN base_rbac_menu.icon IS '图标标识';

COMMENT ON COLUMN base_rbac_menu.status IS '是否启用0-禁用/1-启用';

COMMENT ON COLUMN base_rbac_menu.link_type IS '链接类型【1-内部链接(默认)2-外部链接】';

COMMENT ON COLUMN base_rbac_menu.link_url IS '链接地址【pathinfo#method】';

COMMENT ON COLUMN base_rbac_menu.crt_time IS '创建时间';

COMMENT ON COLUMN base_rbac_menu.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_rbac_menu.crt_name IS '创建用户';

COMMENT ON COLUMN base_rbac_menu.crt_host IS '创建IP';

COMMENT ON COLUMN base_rbac_menu.upd_time IS '更新时间';

COMMENT ON COLUMN base_rbac_menu.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_rbac_menu.upd_name IS '更新用户';

COMMENT ON COLUMN base_rbac_menu.upd_host IS '更新IP';

COMMENT ON COLUMN base_rbac_menu.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_rbac_menu_upd BEFORE UPDATE ON base_rbac_menu FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_rbac_role (
  id NUMBER(10) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  remarks VARCHAR2(255),
  status NUMBER(1) NOT NULL,
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

CREATE SEQUENCE base_rbac_role_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_rbac_role_bi BEFORE INSERT ON base_rbac_role FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_rbac_role_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_rbac_role IS 'BASE-角色表';

COMMENT ON COLUMN base_rbac_role.id IS 'ID';

COMMENT ON COLUMN base_rbac_role.name IS '角色名称';

COMMENT ON COLUMN base_rbac_role.remarks IS '角色描述';

COMMENT ON COLUMN base_rbac_role.status IS '是否启用';

COMMENT ON COLUMN base_rbac_role.crt_time IS '创建时间';

COMMENT ON COLUMN base_rbac_role.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_rbac_role.crt_name IS '创建用户';

COMMENT ON COLUMN base_rbac_role.crt_host IS '创建IP';

COMMENT ON COLUMN base_rbac_role.upd_time IS '更新时间';

COMMENT ON COLUMN base_rbac_role.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_rbac_role.upd_name IS '更新用户';

COMMENT ON COLUMN base_rbac_role.upd_host IS '更新IP';

COMMENT ON COLUMN base_rbac_role.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_rbac_role_upd BEFORE UPDATE ON base_rbac_role FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_rbac_role_menu (
  id NUMBER(10) NOT NULL,
  role_id NUMBER(10) NOT NULL,
  menu_id NUMBER(10) NOT NULL,
  half_checked NUMBER(1) NOT NULL,
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

CREATE SEQUENCE base_rbac_role_menu_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_rbac_role_menu_bi BEFORE INSERT ON base_rbac_role_menu FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_rbac_role_menu_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_rbac_role_menu IS 'BASE-角色权限对应表';

COMMENT ON COLUMN base_rbac_role_menu.id IS 'ID';

COMMENT ON COLUMN base_rbac_role_menu.role_id IS '角色ID';

COMMENT ON COLUMN base_rbac_role_menu.menu_id IS '权限ID';

COMMENT ON COLUMN base_rbac_role_menu.half_checked IS '是否半勾选';

COMMENT ON COLUMN base_rbac_role_menu.crt_time IS '创建时间';

COMMENT ON COLUMN base_rbac_role_menu.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_rbac_role_menu.crt_name IS '创建用户';

COMMENT ON COLUMN base_rbac_role_menu.crt_host IS '创建IP';

COMMENT ON COLUMN base_rbac_role_menu.upd_time IS '更新时间';

COMMENT ON COLUMN base_rbac_role_menu.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_rbac_role_menu.upd_name IS '更新用户';

COMMENT ON COLUMN base_rbac_role_menu.upd_host IS '更新IP';

COMMENT ON COLUMN base_rbac_role_menu.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_rbac_role_menu_upd BEFORE UPDATE ON base_rbac_role_menu FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_rbac_user_role (
  id NUMBER(10) NOT NULL,
  user_id VARCHAR2(32) NOT NULL,
  role_id NUMBER(10) NOT NULL,
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

CREATE SEQUENCE base_rbac_user_role_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_rbac_user_role_bi BEFORE INSERT ON base_rbac_user_role FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_rbac_user_role_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_rbac_user_role IS 'BASE-用户角色关联表';

COMMENT ON COLUMN base_rbac_user_role.id IS 'ID';

COMMENT ON COLUMN base_rbac_user_role.user_id IS '用户ID';

COMMENT ON COLUMN base_rbac_user_role.role_id IS '角色ID';

COMMENT ON COLUMN base_rbac_user_role.crt_time IS '创建时间';

COMMENT ON COLUMN base_rbac_user_role.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_rbac_user_role.crt_name IS '创建用户';

COMMENT ON COLUMN base_rbac_user_role.crt_host IS '创建IP';

COMMENT ON COLUMN base_rbac_user_role.upd_time IS '更新时间';

COMMENT ON COLUMN base_rbac_user_role.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_rbac_user_role.upd_name IS '更新用户';

COMMENT ON COLUMN base_rbac_user_role.upd_host IS '更新IP';

COMMENT ON COLUMN base_rbac_user_role.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_rbac_user_role_upd BEFORE UPDATE ON base_rbac_user_role FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_sms_code (
  id NUMBER(10) NOT NULL,
  phone VARCHAR2(15) NOT NULL,
  code VARCHAR2(6) NOT NULL,
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_sms_code_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_sms_code_bi BEFORE INSERT ON base_sms_code FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_sms_code_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_sms_code IS 'BASE-短信验证码';

COMMENT ON COLUMN base_sms_code.id IS 'ID';

COMMENT ON COLUMN base_sms_code.phone IS '手机号';

COMMENT ON COLUMN base_sms_code.code IS '短信验证码';

COMMENT ON COLUMN base_sms_code.crt_time IS '创建时间';

CREATE TABLE base_system_update_log (
  id NUMBER(10) NOT NULL,
  no VARCHAR2(255) NOT NULL,
  name VARCHAR2(255) NOT NULL,
  ver NUMBER(10) NOT NULL,
  ver_no VARCHAR2(255) NOT NULL,
  remark VARCHAR2(255),
  log CLOB,
  crt_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);

CREATE SEQUENCE base_system_update_log_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_system_update_log_bi BEFORE INSERT ON base_system_update_log FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_system_update_log_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_system_update_log IS 'BASE-系统版本日志表';

COMMENT ON COLUMN base_system_update_log.id IS 'ID';

COMMENT ON COLUMN base_system_update_log.no IS '模块编码';

COMMENT ON COLUMN base_system_update_log.name IS '模块名称';

COMMENT ON COLUMN base_system_update_log.ver IS '版本号';

COMMENT ON COLUMN base_system_update_log.ver_no IS '版本编码';

COMMENT ON COLUMN base_system_update_log.remark IS '备注信息';

COMMENT ON COLUMN base_system_update_log.log IS 'SQL执行内容';

COMMENT ON COLUMN base_system_update_log.crt_time IS '创建时间';

CREATE TABLE base_user (
  id VARCHAR2(32) NOT NULL,
  department_id VARCHAR2(32) NOT NULL,
  username VARCHAR2(255) NOT NULL,
  password VARCHAR2(255),
  name VARCHAR2(255) NOT NULL,
  tel VARCHAR2(20) NOT NULL,
  birthday date,
  sex NUMBER(5),
  address VARCHAR2(255),
  email VARCHAR2(255),
  status NUMBER(1) NOT NULL,
  admin_enabled NUMBER(1) DEFAULT 0 NOT NULL,
  role_names VARCHAR2(255),
  description VARCHAR2(255),
  img VARCHAR2(255),
  api_token VARCHAR2(255),
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

COMMENT ON TABLE base_user IS 'BASE-用户';

COMMENT ON COLUMN base_user.id IS 'ID';

COMMENT ON COLUMN base_user.department_id IS '部门ID';

COMMENT ON COLUMN base_user.username IS '账户';

COMMENT ON COLUMN base_user.password IS '密码';

COMMENT ON COLUMN base_user.name IS '姓名';

COMMENT ON COLUMN base_user.tel IS '手机号';

COMMENT ON COLUMN base_user.birthday IS '生日';

COMMENT ON COLUMN base_user.sex IS '性别0-女1-男2-未知';

COMMENT ON COLUMN base_user.address IS '地址';

COMMENT ON COLUMN base_user.email IS '邮箱';

COMMENT ON COLUMN base_user.status IS '状态：1-有效/0-锁定';

COMMENT ON COLUMN base_user.admin_enabled IS '是否允许访问后台管理端';

COMMENT ON COLUMN base_user.role_names IS '角色名称';

COMMENT ON COLUMN base_user.description IS '描述';

COMMENT ON COLUMN base_user.img IS '头像URL';

COMMENT ON COLUMN base_user.api_token IS 'api token';

COMMENT ON COLUMN base_user.crt_time IS '创建时间';

COMMENT ON COLUMN base_user.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_user.crt_name IS '创建用户';

COMMENT ON COLUMN base_user.crt_host IS '创建IP';

COMMENT ON COLUMN base_user.upd_time IS '更新时间';

COMMENT ON COLUMN base_user.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_user.upd_name IS '更新用户';

COMMENT ON COLUMN base_user.upd_host IS '更新IP';

COMMENT ON COLUMN base_user.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_user_upd BEFORE UPDATE ON base_user FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE portal_contact_inquiry (
  id NUMBER(19) NOT NULL,
  name VARCHAR2(100) NOT NULL,
  company VARCHAR2(200),
  tel VARCHAR2(32) NOT NULL,
  email VARCHAR2(255),
  subject VARCHAR2(200) NOT NULL,
  message CLOB NOT NULL,
  status NUMBER(5) DEFAULT '0' NOT NULL,
  source VARCHAR2(32) DEFAULT 'PORTAL' NOT NULL,
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

CREATE SEQUENCE portal_contact_inquiry_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_portal_contact_inquiry_bi BEFORE INSERT ON portal_contact_inquiry FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT portal_contact_inquiry_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

CREATE INDEX i_portal_contact_inquiry_73dd ON portal_contact_inquiry (status, crt_time);

CREATE INDEX i_portal_contact_inquiry_a0f2 ON portal_contact_inquiry (tel);

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

CREATE OR REPLACE TRIGGER trg_portal_contact_inquiry_upd BEFORE UPDATE ON portal_contact_inquiry FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

CREATE TABLE base_user_token (
  id VARCHAR2(32) NOT NULL,
  user_id VARCHAR2(32) NOT NULL,
  valid NUMBER(1) DEFAULT 1 NOT NULL,
  remark VARCHAR2(255) NOT NULL,
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

COMMENT ON TABLE base_user_token IS 'BASE-用户token';

COMMENT ON COLUMN base_user_token.id IS 'ID';

COMMENT ON COLUMN base_user_token.user_id IS '用户ID';

COMMENT ON COLUMN base_user_token.valid IS '是否有效';

COMMENT ON COLUMN base_user_token.remark IS '备注';

COMMENT ON COLUMN base_user_token.crt_time IS '创建时间';

COMMENT ON COLUMN base_user_token.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_user_token.crt_name IS '创建用户';

COMMENT ON COLUMN base_user_token.crt_host IS '创建IP';

COMMENT ON COLUMN base_user_token.upd_time IS '更新时间';

COMMENT ON COLUMN base_user_token.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_user_token.upd_name IS '更新用户';

COMMENT ON COLUMN base_user_token.upd_host IS '更新IP';

COMMENT ON COLUMN base_user_token.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_user_token_upd BEFORE UPDATE ON base_user_token FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

INSERT INTO base_config_sys (id, data, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (1, '{"cop": "faberxu@gmail.com", "logo": "1", "title": "FaAdmin", "loginBg": "1", "copColor": "#ffffff", "subTitle": "FaAdmin通用管理平台", "portalLink": null, "titleColor": "#ffffff", "logoWithText": "1", "loginPageType": "cute", "safeCaptchaOn": true, "subTitleColor": "#ffffff", "storeLocalPath": "/opt/fa-admin/file", "topMenuBarStyle": "default", "safePasswordType": 1, "safePasswordLenMax": 30, "safePasswordLenMin": 3, "safeRegistrationOn": true, "safeTokenExpireHour": 24}', TIMESTAMP '2023-04-04 16:10:50', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2023-04-04 16:10:50', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_department (id, name, description, parent_id, sort, type, manager_id, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES ('1', '办公室', '', '0', 1, NULL, '1', TIMESTAMP '2020-06-26 16:07:35', '1', 'admin', '123.116.43.116', TIMESTAMP '2020-06-26 16:07:35', '1', 'admin', '123.116.43.116', 0);

INSERT INTO base_department (id, name, description, parent_id, sort, type, manager_id, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES ('c1eafdca1d4bd02b90c4cd15e3528e66', '部门1', NULL, '1', 0, NULL, NULL, TIMESTAMP '2022-12-08 17:19:03', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2022-12-08 21:49:02', '1', '超级管理员', '221.231.169.192', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (2, 'common', '常用字典', 0, 3, NULL, '[]', TIMESTAMP '2022-12-10 18:29:12', '1', 'admin', '127.0.0.1', TIMESTAMP '2022-12-10 18:29:13', '1', '超级管理员', '192.168.58.1', 1);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (6, 'common_sex', '性别', 12, 0, NULL, '[{"id": 1, "label": "女", "value": "0", "deleted": false}, {"id": 2, "label": "男", "value": "1", "deleted": false}, {"id": 3, "label": "保密", "value": "2", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:02', '1', 'admin', '127.0.0.1', TIMESTAMP '2020-06-19 09:50:36', '1', 'admin', '114.242.249.111', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (7, 'common_education', '学历', 12, 2, NULL, '[{"id": 1, "label": "小学", "value": "1", "deleted": false}, {"id": 2, "label": "中学", "value": "2", "deleted": false}, {"id": 3, "label": "高中", "value": "3", "deleted": false}, {"id": 4, "label": "大学", "value": "4", "deleted": false}, {"id": 5, "label": "博士", "value": "6", "deleted": false}, {"id": 6, "label": "研究生", "value": "5", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:03', '1', 'admin', '127.0.0.1', TIMESTAMP '2020-06-19 09:50:45', '1', 'admin', '114.242.249.111', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (8, 'common_politics', '政治面貌', 12, 3, NULL, '[{"id": 0, "label": "群众", "value": "1", "deleted": false}, {"id": 1, "label": "团员", "value": "2", "deleted": false}, {"id": 2, "label": "党员", "value": "3", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:05', '1', 'admin', '127.0.0.1', TIMESTAMP '2020-06-19 09:50:52', '1', 'admin', '114.242.249.111', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (9, 'group_user_type', '分组用户类型', 12, 4, NULL, '[{"id": 0, "label": "领导", "value": "leader", "deleted": false}, {"id": 1, "label": "员工", "value": "member", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:05', '1', 'admin', '127.0.0.1', TIMESTAMP '2020-06-19 09:50:59', '1', 'admin', '114.242.249.111', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (10, 'common_area', '地区', 0, 1, NULL, '[]', TIMESTAMP '2022-12-10 09:56:07', '1', 'admin', '127.0.0.1', TIMESTAMP '2020-11-05 15:21:38', '1', 'admin', '120.243.220.191', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (11, 'common_area_level', '层级', 10, 1, NULL, '[{"id": 0, "label": "省", "value": "0", "deleted": false}, {"id": 1, "label": "市", "value": "1", "deleted": false}, {"id": 2, "label": "县", "value": "2", "deleted": false}, {"id": 3, "label": "乡", "value": "3", "deleted": false}, {"id": 4, "label": "村", "value": "4", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:08', '1', 'admin', '127.0.0.1', TIMESTAMP '2019-08-21 10:13:38', '1', 'admin', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12, 'common_user', '账户字典', 0, 0, NULL, '[]', TIMESTAMP '2022-12-10 09:56:09', '1', 'admin', '127.0.0.1', TIMESTAMP '2019-10-30 14:07:45', '1', 'admin', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (13, 'common_user_status', '账户状态', 12, 1, NULL, '[{"id": 0, "label": "有效", "value": "1", "deleted": false}, {"id": 1, "label": "冻结", "value": "0", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:10', '1', 'admin', '127.0.0.1', TIMESTAMP '2019-10-30 15:09:06', '1', 'admin', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (20, 'sys_file_download', '系统文件下载', 0, 4, '系统文件下载：包括文件模板、常用文件', '[]', TIMESTAMP '2022-12-10 18:29:15', '1', 'admin', '127.0.0.1', TIMESTAMP '2022-12-10 18:29:16', '1', '超级管理员', '192.168.58.1', 1);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (48, 'base_dict', '基础字典', 0, 2, NULL, '[]', TIMESTAMP '2022-12-10 09:56:12', '1', 'admin', '120.243.220.191', TIMESTAMP '2020-11-05 15:21:38', '1', 'admin', '120.243.220.191', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (49, 'base_dict_bool', '是否', 48, 0, NULL, '[{"id": 0, "label": "是", "value": "1", "deleted": false}, {"id": 1, "label": "否", "value": "0", "deleted": false}]', TIMESTAMP '2022-12-10 09:56:13', '1', 'admin', '120.243.220.191', TIMESTAMP '2021-03-25 11:40:35', '1', 'admin', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (54, 'system', '系统设置', 0, 5, NULL, '[{"id": 0, "label": "system:title", "value": "Fa Admin", "deleted": false}, {"id": 1, "label": "system:logo", "value": "818e4fdeb5a7a1e5cc0b492cc76a077a", "deleted": false}, {"id": 2, "label": "system:portal:logoWithText", "value": "0fea6b8a396a06a4c90776ded510bafe", "deleted": false}, {"id": 3, "label": "system:portal:link", "value": "http://xxx.xxx.com", "deleted": false}, {"id": 4, "label": "system:phpRedisAdmin", "value": "https://fa.dward.cn/phpRedisAdmin", "deleted": false}, {"id": 5, "label": "system:subTitle", "value": "简单、易维护的后台管理系统", "deleted": false}, {"id": 6, "label": "system:socketUrl", "value": "fa.socket.dward.cn", "deleted": false}]', TIMESTAMP '2022-12-13 13:40:44', '1', 'admin', '127.0.0.1', TIMESTAMP '2022-12-13 13:40:44', '1', '超级管理员', '221.231.188.211', 1);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, options, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (55, 'test', 'test', 0, 6, 'cccc', '[{"id": 1, "label": "昨天", "value": "1", "deleted": false}, {"id": 2, "label": "今天", "value": "2", "deleted": false}, {"id": 3, "label": "明天", "value": "3", "deleted": false}, {"id": 4, "label": "后天", "value": "4", "deleted": true}, {"id": 5, "label": "后天", "value": "4", "deleted": false}]', TIMESTAMP '2022-12-11 20:41:13', '1', '超级管理员', '192.168.58.1', TIMESTAMP '2022-12-11 20:41:16', '1', '超级管理员', '192.168.58.1', 1);

INSERT INTO base_job (id, job_name, cron, status, clazz_path, job_desc, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (1, '测试任务1', '0 0/5 * * * ?', 0, 'com.faber.config.quartz.customer.JobDemo1', '测试任务111111', TIMESTAMP '2022-09-29 15:46:31', '1', 'admin', '127.0.0.1', TIMESTAMP '2022-09-07 17:22:54', '1', 'admin', '127.0.0.1', 0);

INSERT INTO base_rbac_role (id, name, remarks, status, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (1, '超级管理员', '超级管理员', 1, TIMESTAMP '2022-09-19 17:34:00', '1', '超级管理员1', '127.0.0.1', TIMESTAMP '2022-09-19 17:34:14', '1', '超级管理员1', '127.0.0.1', 0);

INSERT INTO base_rbac_role (id, name, remarks, status, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (2, '默认用户角色', '默认用户角色，用户注册后会分配该角色。注：请不要修改此角色名称。', 1, TIMESTAMP '2023-02-06 11:34:53', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2023-02-06 11:34:52', NULL, NULL, NULL, 0);

INSERT INTO base_rbac_user_role (id, user_id, role_id, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (1, '1', 1, TIMESTAMP '2023-02-20 11:04:04', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_user (id, department_id, username, password, name, tel, birthday, sex, address, email, status, admin_enabled, role_names, description, img, api_token, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES ('1', 'c1eafdca1d4bd02b90c4cd15e3528e66', 'admin', '$2a$12$MAibDd3RbrSyB7i5m8bzMubLmBcoH/vBqSJiIElmZgalMiT9iuj6C', '超级管理员', '13811112222', DATE '2000-01-01', 1, '南京市', 'faberxu@gmail.com', 1, 1, '超级管理员', '', '4dd5c89a66725f5ede372b6bb116ae3a', 'd1d6e6d1ebcb4437bd082c3046671582', TIMESTAMP '2023-02-03 19:34:30', '1', 'admin', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (10000000, 0, '首页', 0, 0, 'mdi:house-outline', 1, 1, '/admin/home', TIMESTAMP '2023-01-03 16:11:35', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12000000, 0, '系统设置', 4, 0, 'mdi:cog', 1, 1, '/admin/system', TIMESTAMP '2022-09-19 16:56:23', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (10010000, 10000000, '工作台', 0, 1, 'desktop', 1, 1, '/admin/home/desktop', TIMESTAMP '2023-01-03 16:07:53', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12010000, 12000000, '智能人事', 0, 1, 'mdi:users-group', 1, 1, '/admin/system/hr', TIMESTAMP '2022-09-19 16:58:28', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020000, 12000000, '系统管理', 1, 1, 'mdi:cogs', 1, 1, '/admin/system/base', TIMESTAMP '2022-09-19 16:58:56', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12030000, 12000000, '个人中心', 2, 1, 'mdi:ticket-user', 1, 1, '/admin/system/account', TIMESTAMP '2022-09-19 16:59:40', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12040000, 12000000, '系统监控', 3, 1, 'mdi:monitor-eye', 1, 1, '/admin/system/monitor', TIMESTAMP '2022-10-17 15:16:48', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12010100, 12010000, '用户管理', 0, 1, 'mdi:users-tick', 1, 1, '/admin/system/hr/user', TIMESTAMP '2022-09-19 17:02:08', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12010300, 12010000, '角色权限管理', 2, 1, 'mdi:clipboard-user', 1, 1, '/admin/system/hr/role', TIMESTAMP '2022-09-19 17:12:26', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020100, 12020000, '菜单管理', 0, 1, 'mdi:hamburger-menu', 1, 1, '/admin/system/base/menuV2', TIMESTAMP '2022-09-19 17:14:17', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020200, 12020000, '字典管理', 1, 1, 'mdi:dictionary', 1, 1, '/admin/system/base/dict', TIMESTAMP '2022-09-19 17:14:44', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020300, 12020000, '中国地区管理', 3, 1, 'mdi:map-outline', 1, 1, '/admin/system/base/area', TIMESTAMP '2022-09-19 17:15:00', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020400, 12020000, '定时任务', 4, 1, 'mdi:calendar-task', 1, 1, '/admin/system/base/job', TIMESTAMP '2022-09-19 17:15:14', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020500, 12020000, '请求日志', 5, 1, 'mdi:math-log', 1, 1, '/admin/system/base/logApi', TIMESTAMP '2022-09-19 17:15:33', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020600, 12020000, '系统公告', 7, 1, 'mdi:notice-board', 1, 1, '/admin/system/base/notice', TIMESTAMP '2022-09-19 17:16:13', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020700, 12020000, '登录日志', 6, 1, 'mdi:login', 1, 1, '/admin/system/base/logLogin', TIMESTAMP '2022-09-19 17:16:36', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020800, 12020000, '系统配置', 2, 1, 'mdi:application-cog-outline', 1, 1, '/admin/system/base/config', TIMESTAMP '2022-12-11 22:39:02', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12020900, 12020000, '附件管理', 8, 1, 'mdi:file-cog-outline', 1, 1, '/admin/system/base/fileSave', TIMESTAMP '2023-02-03 11:02:48', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12021000, 12020000, '版本日志', 9, 1, 'mdi:blog', 1, 1, '/admin/system/base/systemUpdateLog', TIMESTAMP '2023-02-03 11:02:48', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12030100, 12030000, '基本信息', 0, 1, 'mdi:user-badge', 1, 1, '/admin/system/account/base', TIMESTAMP '2022-09-19 17:17:05', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12030200, 12030000, '更新密码', 1, 1, 'mdi:password-secure', 1, 1, '/admin/system/account/security', TIMESTAMP '2022-09-19 17:17:52', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12030300, 12030000, '消息中心', 2, 1, 'mdi:message-bulleted', 1, 1, '/admin/system/account/msg', TIMESTAMP '2022-09-19 17:18:06', '1', '超级管理员1', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12030400, 12030000, 'Token管理', 3, 1, 'mdi:api', 1, 1, '/admin/system/account/token', TIMESTAMP '2023-01-24 20:25:33', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12040100, 12040000, '数据监控', 0, 1, 'mdi:database-eye', 1, 1, '/admin/system/monitor/druid', TIMESTAMP '2022-10-17 15:17:29', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12040200, 12040000, '服务监控', 1, 1, 'mdi:server', 1, 1, '/admin/system/monitor/server', TIMESTAMP '2022-10-17 15:23:40', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);

INSERT INTO base_rbac_menu (id, parent_id, name, sort, "LEVEL", icon, status, link_type, link_url, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (12040300, 12040000, 'Redis管理', 2, 1, 'mdi:data-settings', 1, 1, '/admin/system/monitor/redis', TIMESTAMP '2022-11-29 17:33:43', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
