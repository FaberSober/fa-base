-- ------------------------- info -------------------------
-- @@ver: 1_000_008
-- @@info: 用户增加微信小程序信息字段
-- ------------------------- info -------------------------
ALTER TABLE base_user ADD wx_union_id VARCHAR2(255);

COMMENT ON COLUMN base_user.wx_union_id IS '开放平台的唯一标识符';

ALTER TABLE base_user ADD wx_ma_openid VARCHAR2(255);

COMMENT ON COLUMN base_user.wx_ma_openid IS '微信小程序用户唯一标识';
