-- ------------------------- info -------------------------
-- @@ver: 1_000_047
-- @@info: 增加系统首次启动初始化状态
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_system_bootstrap_state` (
  `state_key` varchar(64) NOT NULL COMMENT '初始化状态标识',
  `completed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已完成',
  PRIMARY KEY (`state_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统首次启动初始化状态';

INSERT IGNORE INTO `base_system_bootstrap_state` (`state_key`, `completed`)
SELECT 'admin_role_menu',
       CASE WHEN EXISTS (
           SELECT 1 FROM `base_rbac_role_menu` WHERE `role_id` = 1 AND `deleted` = 0
       ) THEN 1 ELSE 0 END
FROM DUAL;

INSERT IGNORE INTO `base_system_bootstrap_state` (`state_key`, `completed`)
SELECT 'default_tenant_access',
       CASE WHEN EXISTS (
           SELECT 1
             FROM `tn_tenant` t
             JOIN `tn_tenant_user` tu ON tu.`tenant_id` = t.`id`
             JOIN `base_rbac_role` r ON r.`tenant_id` = t.`id` AND r.`type` = 3
             JOIN `base_rbac_user_role` ur ON ur.`role_id` = r.`id` AND ur.`user_id` = '1'
             JOIN `base_rbac_role_menu` rm ON rm.`role_id` = r.`id`
            WHERE t.`code` = 'DEFAULT'
              AND t.`deleted` = 0
              AND tu.`user_id` = '1' AND tu.`is_admin` = 1 AND tu.`status` = 1 AND tu.`deleted` = 0
              AND r.`deleted` = 0
              AND ur.`deleted` = 0
              AND rm.`deleted` = 0
       ) THEN 1 ELSE 0 END
FROM DUAL;
