-- ------------------------- info -------------------------
-- @@ver: 1_000_035
-- @@info: 增加统一日历维护菜单
-- ------------------------- info -------------------------

INSERT INTO `base_rbac_menu` (
  `id`, `parent_id`, `scope`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`,
  `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`
)
SELECT 12021500, 12020000, 1, '统一日历', 12, 1, 'mdi:calendar-clock-outline', 1, 1,
       '/admin/system/base/calendar', CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', 0
WHERE NOT EXISTS (
  SELECT 1 FROM `base_rbac_menu`
  WHERE `id` = 12021500 OR `link_url` = '/admin/system/base/calendar'
);

INSERT INTO `base_rbac_role_menu`
    (`role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`)
SELECT 1, 12021500, 0, CURRENT_TIMESTAMP, '1', '超级管理员', '127.0.0.1', 0
WHERE EXISTS (
    SELECT 1 FROM `base_rbac_role_menu` WHERE `role_id` = 1 AND `deleted` = 0
  )
  AND EXISTS (
    SELECT 1 FROM `base_rbac_menu` WHERE `id` = 12021500 AND `deleted` = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM `base_rbac_role_menu`
    WHERE `role_id` = 1 AND `menu_id` = 12021500 AND `deleted` = 0
  );
