-- ------------------------- info -------------------------
-- @@ver: 1_000_012
-- @@info: 增加菜单：系统预警、系统新闻
-- ------------------------- info -------------------------

INSERT INTO "base_rbac_menu" ("id", "parent_id", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (12021200, 12020000, '告警信息', 4, 1, NULL, true, 1, '/admin/system/base/alert', '2023-12-16 11:49:53', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, false);
INSERT INTO "base_rbac_menu" ("id", "parent_id", "name", "sort", "level", "icon", "status", "link_type", "link_url", "crt_time", "crt_user", "crt_name", "crt_host", "upd_time", "upd_user", "upd_name", "upd_host", "deleted") VALUES (12021300, 12020000, '系统新闻', 3, 1, NULL, true, 1, '/admin/system/base/sysNews', '2023-12-18 10:22:31', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, false);

SELECT setval(pg_get_serial_sequence('base_rbac_menu', 'id'), COALESCE((SELECT MAX("id") FROM "base_rbac_menu"), 1), true);
