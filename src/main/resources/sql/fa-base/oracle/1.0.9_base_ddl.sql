-- ------------------------- info -------------------------
-- @@ver: 1_000_009
-- @@info: 用户增加工作状态字段
-- ------------------------- info -------------------------
ALTER TABLE base_user ADD work_status NUMBER(5);

COMMENT ON COLUMN base_user.work_status IS '工作状态：0-在职/1-请假/2-离职';
