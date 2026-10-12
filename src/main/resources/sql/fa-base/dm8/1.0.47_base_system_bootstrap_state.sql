-- ------------------------- info -------------------------
-- @@ver: 1_000_047
-- @@info: 增加系统首次启动初始化状态
-- ------------------------- info -------------------------

DECLARE n INT;
BEGIN
    SELECT COUNT(*) INTO n FROM ALL_TABLES
    WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND LOWER(TABLE_NAME) = 'base_system_bootstrap_state';
    IF n = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE base_system_bootstrap_state (
    state_key varchar(64) NOT NULL,
    completed tinyint DEFAULT 0 NOT NULL,
    PRIMARY KEY (state_key)
)';
    END IF;
END;

COMMENT ON TABLE base_system_bootstrap_state IS '系统首次启动初始化状态';
COMMENT ON COLUMN base_system_bootstrap_state.state_key IS '初始化状态标识';
COMMENT ON COLUMN base_system_bootstrap_state.completed IS '是否已完成';

INSERT INTO base_system_bootstrap_state (state_key, completed)
SELECT 'admin_role_menu',
       CASE WHEN EXISTS (
           SELECT 1 FROM base_rbac_role_menu WHERE role_id = 1 AND deleted = 0
       ) THEN 1 ELSE 0 END
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_system_bootstrap_state WHERE state_key = 'admin_role_menu');

INSERT INTO base_system_bootstrap_state (state_key, completed)
SELECT 'default_tenant_access',
       CASE WHEN EXISTS (
           SELECT 1
             FROM tn_tenant t
             JOIN tn_tenant_user tu ON tu.tenant_id = t.id
             JOIN base_rbac_role r ON r.tenant_id = t.id AND r.type = 3
             JOIN base_rbac_user_role ur ON ur.role_id = r.id AND ur.user_id = '1'
             JOIN base_rbac_role_menu rm ON rm.role_id = r.id
            WHERE t.code = 'DEFAULT'
              AND t.deleted = 0
              AND tu.user_id = '1' AND tu.is_admin = 1 AND tu.status = 1 AND tu.deleted = 0
              AND r.deleted = 0
              AND ur.deleted = 0
              AND rm.deleted = 0
       ) THEN 1 ELSE 0 END
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM base_system_bootstrap_state WHERE state_key = 'default_tenant_access');
