-- ------------------------- info -------------------------
-- @@ver: 1_000_022
-- @@info: 字典base_dict表增加字段：type数值类型：1-关联列表，2-关联树，3-字符串，4-选择列表；value字典值。增加表base_dict_data。
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_dict' AND LOWER(COLUMN_NAME) = 'type';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_dict ADD type tinyint NULL';
    END IF;
END;

COMMENT ON COLUMN base_dict.type IS '数值类型：1-关联列表，2-关联树，3-字符串，4-选择列表';

UPDATE base_dict SET type = 4 WHERE type IS NULL;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TAB_COLUMNS
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_dict' AND LOWER(COLUMN_NAME) = 'value';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE base_dict ADD value varchar(255) NULL';
    END IF;
END;

COMMENT ON COLUMN base_dict.value IS '字典值';

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_dict_data';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_dict_data (
    id int IDENTITY(1,1) NOT NULL,
    parent_id int DEFAULT NULL,
    dict_id int NOT NULL,
    sort_id int DEFAULT ''0'',
    label varchar(255) DEFAULT NULL,
    value varchar(255) DEFAULT NULL,
    is_default tinyint DEFAULT NULL,
    valid tinyint DEFAULT NULL,
    description varchar(255) DEFAULT NULL,
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
