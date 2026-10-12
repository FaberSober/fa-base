-- ------------------------- info -------------------------
-- @@ver: 1_000_011
-- @@info: 增加系统新闻表
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_sys_news';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_sys_news (
    id int IDENTITY(1,1) NOT NULL,
    title varchar(50) NOT NULL,
    content CLOB,
    cover varchar(32) DEFAULT NULL,
    author varchar(255) DEFAULT NULL,
    pub_time timestamp NULL DEFAULT NULL,
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
