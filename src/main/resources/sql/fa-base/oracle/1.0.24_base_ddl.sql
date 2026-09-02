-- ------------------------- info -------------------------
-- @@ver: 1_000_024
-- @@info: 更新base_rbac_menu菜单icon为mdi格式图标
-- ------------------------- info -------------------------
UPDATE base_rbac_menu SET icon = 'mdi:house-outline' WHERE link_url = '/admin/home';

UPDATE base_rbac_menu SET icon = 'mdi:cog' WHERE link_url = '/admin/system';

UPDATE base_rbac_menu SET icon = 'mdi:cogs' WHERE link_url = '/admin/system/base';

UPDATE base_rbac_menu SET icon = 'mdi:users-group' WHERE link_url = '/admin/system/hr';

UPDATE base_rbac_menu SET icon = 'mdi:users-tick' WHERE link_url = '/admin/system/hr/user';

UPDATE base_rbac_menu SET icon = 'mdi:clipboard-user' WHERE link_url = '/admin/system/hr/role';

UPDATE base_rbac_menu SET icon = 'mdi:hamburger-menu' WHERE link_url = '/admin/system/base/menu';

UPDATE base_rbac_menu SET icon = 'mdi:dictionary' WHERE link_url = '/admin/system/base/dict';

UPDATE base_rbac_menu SET icon = 'mdi:map-outline' WHERE link_url = '/admin/system/base/area';

UPDATE base_rbac_menu SET icon = 'mdi:calendar-task' WHERE link_url = '/admin/system/base/job';

UPDATE base_rbac_menu SET icon = 'mdi:math-log' WHERE link_url = '/admin/system/base/logApi';

UPDATE base_rbac_menu SET icon = 'mdi:notice-board' WHERE link_url = '/admin/system/base/notice';

UPDATE base_rbac_menu SET icon = 'mdi:login' WHERE link_url = '/admin/system/base/logLogin';

UPDATE base_rbac_menu SET icon = 'mdi:application-cog-outline' WHERE link_url = '/admin/system/base/config';

UPDATE base_rbac_menu SET icon = 'mdi:file-cog-outline' WHERE link_url = '/admin/system/base/fileSave';

UPDATE base_rbac_menu SET icon = 'mdi:blog' WHERE link_url = '/admin/system/base/systemUpdateLog';

UPDATE base_rbac_menu SET icon = 'mdi:code-block-tags' WHERE link_url = '/admin/system/base/generator';

UPDATE base_rbac_menu SET icon = 'mdi:newspaper' WHERE link_url = '/admin/system/base/sysNews';

UPDATE base_rbac_menu SET icon = 'mdi:alert-octagon' WHERE link_url = '/admin/system/base/alert';

UPDATE base_rbac_menu SET icon = 'mdi:ticket-user' WHERE link_url = '/admin/system/account';

UPDATE base_rbac_menu SET icon = 'mdi:user-badge' WHERE link_url = '/admin/system/account/base';

UPDATE base_rbac_menu SET icon = 'mdi:password-secure' WHERE link_url = '/admin/system/account/security';

UPDATE base_rbac_menu SET icon = 'mdi:message-bulleted' WHERE link_url = '/admin/system/account/msg';

UPDATE base_rbac_menu SET icon = 'mdi:api' WHERE link_url = '/admin/system/account/token';

UPDATE base_rbac_menu SET icon = 'mdi:monitor-eye' WHERE link_url = '/admin/system/monitor';

UPDATE base_rbac_menu SET icon = 'mdi:database-eye' WHERE link_url = '/admin/system/monitor/druid';

UPDATE base_rbac_menu SET icon = 'mdi:server' WHERE link_url = '/admin/system/monitor/server';

UPDATE base_rbac_menu SET icon = 'mdi:data-settings' WHERE link_url = '/admin/system/monitor/redis';

UPDATE base_rbac_menu SET icon = 'mdi:math-log' WHERE link_url = '/admin/system/monitor/logmonitor';
