-- ------------------------- info -------------------------
-- @@ver: 1_000_015
-- @@info: base_notice系统公告content字段格式修改为text，不限制长度
-- ------------------------- info -------------------------
-- ALTER TABLE base_notice MODIFY (content CLOB);

-- ALTER TABLE base_notice MODIFY (content NOT NULL);

COMMENT ON COLUMN base_notice.content IS '内容';
