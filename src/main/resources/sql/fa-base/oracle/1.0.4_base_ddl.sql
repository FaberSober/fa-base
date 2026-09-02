-- ------------------------- info -------------------------
-- @@ver: 1_000_004
-- @@info: 修复crt_time字段更新自动更新
-- ------------------------- info -------------------------
ALTER TABLE base_config MODIFY (crt_time timestamp);

-- ALTER TABLE base_config MODIFY (crt_time NOT NULL);

ALTER TABLE base_config MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_config.crt_time IS '创建时间';

ALTER TABLE base_config_scene MODIFY (crt_time timestamp);

-- ALTER TABLE base_config_scene MODIFY (crt_time NOT NULL);

ALTER TABLE base_config_scene MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_config_scene.crt_time IS '创建时间';

ALTER TABLE base_config_sys MODIFY (crt_time timestamp);

-- ALTER TABLE base_config_sys MODIFY (crt_time NOT NULL);

ALTER TABLE base_config_sys MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_config_sys.crt_time IS '创建时间';

ALTER TABLE base_department MODIFY (crt_time timestamp);

-- ALTER TABLE base_department MODIFY (crt_time NOT NULL);

ALTER TABLE base_department MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_department.crt_time IS '创建时间';

ALTER TABLE base_dict MODIFY (crt_time timestamp);

-- ALTER TABLE base_dict MODIFY (crt_time NOT NULL);

ALTER TABLE base_dict MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_dict.crt_time IS '创建时间';

ALTER TABLE base_entity_log MODIFY (crt_time timestamp);

-- ALTER TABLE base_entity_log MODIFY (crt_time NOT NULL);

ALTER TABLE base_entity_log MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_entity_log.crt_time IS '创建时间';

ALTER TABLE base_file_biz MODIFY (crt_time timestamp);

-- ALTER TABLE base_file_biz MODIFY (crt_time NOT NULL);

ALTER TABLE base_file_biz MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_file_biz.crt_time IS '创建时间';

ALTER TABLE base_file_save MODIFY (crt_time timestamp);

-- ALTER TABLE base_file_save MODIFY (crt_time NOT NULL);

ALTER TABLE base_file_save MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_file_save.crt_time IS '创建时间';

ALTER TABLE base_job MODIFY (crt_time timestamp);

-- ALTER TABLE base_job MODIFY (crt_time NOT NULL);

ALTER TABLE base_job MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_job.crt_time IS '创建时间';

ALTER TABLE base_log_api MODIFY (crt_time timestamp);

-- ALTER TABLE base_log_api MODIFY (crt_time NOT NULL);

ALTER TABLE base_log_api MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_log_api.crt_time IS '创建时间';

ALTER TABLE base_log_login MODIFY (crt_time timestamp);

-- ALTER TABLE base_log_login MODIFY (crt_time NOT NULL);

ALTER TABLE base_log_login MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_log_login.crt_time IS '创建时间';

ALTER TABLE base_msg MODIFY (crt_time timestamp);

-- ALTER TABLE base_msg MODIFY (crt_time NOT NULL);

ALTER TABLE base_msg MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_msg.crt_time IS '创建时间';

ALTER TABLE base_notice MODIFY (crt_time timestamp);

-- ALTER TABLE base_notice MODIFY (crt_time NOT NULL);

ALTER TABLE base_notice MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_notice.crt_time IS '创建时间';

ALTER TABLE base_rbac_menu MODIFY (crt_time timestamp);

-- ALTER TABLE base_rbac_menu MODIFY (crt_time NOT NULL);

ALTER TABLE base_rbac_menu MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_rbac_menu.crt_time IS '创建时间';

ALTER TABLE base_rbac_role MODIFY (crt_time timestamp);

-- ALTER TABLE base_rbac_role MODIFY (crt_time NOT NULL);

ALTER TABLE base_rbac_role MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_rbac_role.crt_time IS '创建时间';

ALTER TABLE base_rbac_role_menu MODIFY (crt_time timestamp);

-- ALTER TABLE base_rbac_role_menu MODIFY (crt_time NOT NULL);

ALTER TABLE base_rbac_role_menu MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_rbac_role_menu.crt_time IS '创建时间';

ALTER TABLE base_rbac_user_role MODIFY (crt_time timestamp);

-- ALTER TABLE base_rbac_user_role MODIFY (crt_time NOT NULL);

ALTER TABLE base_rbac_user_role MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_rbac_user_role.crt_time IS '创建时间';

ALTER TABLE base_sms_code MODIFY (crt_time timestamp);

-- ALTER TABLE base_sms_code MODIFY (crt_time NOT NULL);

ALTER TABLE base_sms_code MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_sms_code.crt_time IS '创建时间';

ALTER TABLE base_system_update_log MODIFY (crt_time timestamp);

-- ALTER TABLE base_system_update_log MODIFY (crt_time NOT NULL);

ALTER TABLE base_system_update_log MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_system_update_log.crt_time IS '创建时间';

ALTER TABLE base_user MODIFY (crt_time timestamp);

-- ALTER TABLE base_user MODIFY (crt_time NOT NULL);

ALTER TABLE base_user MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_user.crt_time IS '创建时间';

ALTER TABLE base_user_token MODIFY (crt_time timestamp);

-- ALTER TABLE base_user_token MODIFY (crt_time NOT NULL);

ALTER TABLE base_user_token MODIFY (crt_time DEFAULT CURRENT_TIMESTAMP);

COMMENT ON COLUMN base_user_token.crt_time IS '创建时间';
