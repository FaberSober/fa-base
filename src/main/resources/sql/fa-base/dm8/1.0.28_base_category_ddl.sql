-- ------------------------- info -------------------------
-- @@ver: 1_000_028
-- @@info: 增加通用业务分类树
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_category';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_category (
    id int IDENTITY(1,1) NOT NULL,
    module varchar(32) NOT NULL,
    parent_id int DEFAULT ''0'' NOT NULL,
    name varchar(64) NOT NULL,
    description varchar(256) DEFAULT NULL,
    sort int DEFAULT ''0'' NOT NULL,
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

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_category' AND LOWER(INDEX_NAME) = 'uk_base_category_module_parent_name';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_base_category_module_parent_name ON base_category (module, parent_id, name, deleted)';
    END IF;
END;

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_INDEXES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_category' AND LOWER(INDEX_NAME) = 'idx_base_category_module_tree';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_base_category_module_tree ON base_category (module, parent_id, sort, deleted)';
    END IF;
END;

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
