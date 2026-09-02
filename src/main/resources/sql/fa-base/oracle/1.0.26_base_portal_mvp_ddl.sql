-- ------------------------- info -------------------------
-- @@ver: 1_000_026
-- @@info: Portal MVP 用户准入与咨询记录
-- ------------------------- info -------------------------
-- ALTER TABLE base_user ADD admin_enabled NUMBER(1) DEFAULT 0 NOT NULL;

-- COMMENT ON COLUMN base_user.admin_enabled IS '是否允许访问后台管理端';

UPDATE base_user SET admin_enabled = 1 WHERE deleted = 0;
