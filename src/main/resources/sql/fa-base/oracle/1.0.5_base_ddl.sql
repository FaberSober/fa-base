-- ------------------------- info -------------------------
-- @@ver: 1_000_005
-- @@info: base_file_biz字段调整：biz_id非必填、type必填
-- ------------------------- info -------------------------
ALTER TABLE base_file_biz MODIFY (biz_id VARCHAR2(32));

ALTER TABLE base_file_biz MODIFY (biz_id NULL);

COMMENT ON COLUMN base_file_biz.biz_id IS '业务ID';

ALTER TABLE base_file_biz MODIFY (type VARCHAR2(32));

ALTER TABLE base_file_biz MODIFY (type NOT NULL);

COMMENT ON COLUMN base_file_biz.type IS '业务类型';
