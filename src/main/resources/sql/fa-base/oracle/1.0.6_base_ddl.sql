-- ------------------------- info -------------------------
-- @@ver: 1_000_006
-- @@info: base_msg字段调整：buzz_type修改为字符串
-- ------------------------- info -------------------------
ALTER TABLE base_msg MODIFY (buzz_type VARCHAR2(255));

-- ALTER TABLE base_msg MODIFY (buzz_type NULL);

ALTER TABLE base_msg MODIFY (buzz_type DEFAULT NULL);

COMMENT ON COLUMN base_msg.buzz_type IS '业务类型';
