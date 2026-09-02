-- ------------------------- info -------------------------
-- @@ver: 1_000_022
-- @@info: 字典base_dict表增加字段：type数值类型：1-关联列表，2-关联树，3-字符串，4-选择列表；value字典值。增加表base_dict_data。
-- ------------------------- info -------------------------
ALTER TABLE base_dict ADD type NUMBER(5) NULL;

COMMENT ON COLUMN base_dict.type IS '数值类型：1-关联列表，2-关联树，3-字符串，4-选择列表';

UPDATE base_dict SET type = 4 WHERE type IS NULL;

ALTER TABLE base_dict ADD value VARCHAR2(255) NULL;

COMMENT ON COLUMN base_dict.value IS '字典值';

CREATE TABLE base_dict_data (
  id NUMBER(10) NOT NULL,
  parent_id NUMBER(10),
  dict_id NUMBER(10) NOT NULL,
  sort_id NUMBER(10) DEFAULT '0',
  label VARCHAR2(255),
  value VARCHAR2(255),
  is_default NUMBER(1),
  valid NUMBER(1),
  description VARCHAR2(255),
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

CREATE SEQUENCE base_dict_data_seq START WITH 100000000 INCREMENT BY 1 NOCACHE;

CREATE OR REPLACE TRIGGER trg_base_dict_data_bi BEFORE INSERT ON base_dict_data FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN SELECT base_dict_data_seq.NEXTVAL INTO :NEW.id FROM DUAL; END;

COMMENT ON TABLE base_dict_data IS 'BASE-字典值';

COMMENT ON COLUMN base_dict_data.id IS 'ID';

COMMENT ON COLUMN base_dict_data.parent_id IS '上级节点';

COMMENT ON COLUMN base_dict_data.dict_id IS '字典分类ID';

COMMENT ON COLUMN base_dict_data.sort_id IS '排序ID';

COMMENT ON COLUMN base_dict_data.label IS '字典键';

COMMENT ON COLUMN base_dict_data.value IS '字典值';

COMMENT ON COLUMN base_dict_data.is_default IS '是否默认值：0否 1是';

COMMENT ON COLUMN base_dict_data.valid IS '是否生效：0否 1是';

COMMENT ON COLUMN base_dict_data.description IS '描述';

COMMENT ON COLUMN base_dict_data.crt_time IS '创建时间';

COMMENT ON COLUMN base_dict_data.crt_user IS '创建用户ID';

COMMENT ON COLUMN base_dict_data.crt_name IS '创建用户';

COMMENT ON COLUMN base_dict_data.crt_host IS '创建IP';

COMMENT ON COLUMN base_dict_data.upd_time IS '更新时间';

COMMENT ON COLUMN base_dict_data.upd_user IS '更新用户ID';

COMMENT ON COLUMN base_dict_data.upd_name IS '更新用户';

COMMENT ON COLUMN base_dict_data.upd_host IS '更新IP';

COMMENT ON COLUMN base_dict_data.deleted IS '是否删除';

CREATE OR REPLACE TRIGGER trg_base_dict_data_upd BEFORE UPDATE ON base_dict_data FOR EACH ROW BEGIN :NEW.upd_time := CURRENT_TIMESTAMP; END;

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, type, options, value, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (56, 'base_dict_test', '测试字典分组', 0, 3, NULL, 4, '[]', NULL, TIMESTAMP '2025-07-09 15:40:52', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 11:41:37', NULL, NULL, NULL, 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, type, options, value, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (57, 'base_dict_test_options', '测试字典-选择列表', 56, 0, NULL, 4, '[{"id": 1, "label": "选项1", "value": "1", "deleted": false}, {"id": 2, "label": "选项2", "value": "2", "deleted": false}]', NULL, TIMESTAMP '2025-07-09 15:41:47', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 11:41:37', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, type, options, value, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (58, 'base_dict_test_text', '测试字典-字符串', 56, 1, NULL, 3, '[]', '12', TIMESTAMP '2025-07-09 15:44:16', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 11:41:37', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, type, options, value, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (59, 'base_dict_test_link_options', '测试字典-关联列表', 56, 2, NULL, 1, '[]', NULL, TIMESTAMP '2025-07-10 10:47:00', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 11:41:37', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict (id, code, name, parent_id, sort_id, description, type, options, value, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (60, 'base_dict_test_link_tree', '测试字典-关联树', 56, 3, NULL, 2, '[]', NULL, TIMESTAMP '2025-07-10 10:47:20', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 11:41:37', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (1, 0, 59, 0, '选项1', '1', 0, 1, '12', TIMESTAMP '2025-07-10 16:46:44', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:31:10', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (2, 0, 59, 1, '选项2', '2', 0, 1, NULL, TIMESTAMP '2025-07-10 16:46:54', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:32:19', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (3, 0, 60, 2, '选项1', '1', 1, 1, NULL, TIMESTAMP '2025-07-10 16:54:34', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:24:32', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (4, 0, 59, 3, '3', '3', 1, 1, '3', TIMESTAMP '2025-07-11 16:24:18', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:24:32', '1', '超级管理员', '127.0.0.1', 1);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (5, 0, 59, 3, '选项3', '3', NULL, 1, '3', TIMESTAMP '2025-07-11 16:32:05', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:24:32', '1', '超级管理员', '127.0.0.1', 1);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (6, 0, 59, 2, '选项3', '3', 0, 1, '3', TIMESTAMP '2025-07-11 16:33:23', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:32:47', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (7, 3, 60, 0, '选项1-1', '1-1', 0, 1, NULL, TIMESTAMP '2025-07-11 17:30:03', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:24:32', '1', '超级管理员', '127.0.0.1', 0);

INSERT INTO base_dict_data (id, parent_id, dict_id, sort_id, label, value, is_default, valid, description, crt_time, crt_user, crt_name, crt_host, upd_time, upd_user, upd_name, upd_host, deleted) VALUES (8, 0, 60, 3, '选项2', '2', 0, 1, NULL, TIMESTAMP '2025-07-17 17:04:21', '1', '超级管理员', '127.0.0.1', TIMESTAMP '2025-07-18 15:33:11', '1', '超级管理员', '127.0.0.1', 0);
