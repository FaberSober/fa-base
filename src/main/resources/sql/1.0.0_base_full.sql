/*
 Navicat Premium Data Transfer

 Source Server         : localhost-dev-root
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : localhost:3306
 Source Schema         : file_web

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 20/02/2023 14:11:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for base_area
-- ----------------------------
DROP TABLE IF EXISTS `base_area`;
CREATE TABLE `base_area` (
  `id` mediumint(7) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `level` tinyint(1) unsigned NOT NULL COMMENT '层级',
  `parent_code` bigint(14) unsigned NOT NULL DEFAULT '0' COMMENT '父级行政代码',
  `area_code` bigint(14) unsigned NOT NULL DEFAULT '0' COMMENT '行政代码',
  `zip_code` mediumint(6) unsigned zerofill NOT NULL DEFAULT '000000' COMMENT '邮政编码',
  `city_code` char(6) NOT NULL DEFAULT '' COMMENT '区号',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '名称',
  `short_name` varchar(50) NOT NULL DEFAULT '' COMMENT '简称',
  `merger_name` varchar(50) NOT NULL DEFAULT '' COMMENT '组合名',
  `pinyin` varchar(30) NOT NULL DEFAULT '' COMMENT '拼音',
  `lng` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT '经度',
  `lat` decimal(10,6) NOT NULL DEFAULT '0.000000' COMMENT '纬度',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx` (`area_code`) USING BTREE,
  KEY `pidx` (`parent_code`) USING BTREE,
  KEY `level` (`level`) USING BTREE,
  KEY `name` (`name`) USING BTREE,
  KEY `level_name` (`level`,`name`) USING BTREE,
  KEY `short_name` (`short_name`) USING BTREE,
  KEY `level_short_name` (`level`,`short_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='中国行政地区表';

-- ----------------------------
-- Records of base_area
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_config
-- ----------------------------
DROP TABLE IF EXISTS `base_config`;
CREATE TABLE `base_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `biz` varchar(255) NOT NULL COMMENT '业务模块',
  `type` varchar(255) NOT NULL COMMENT '配置类型',
  `data` json NOT NULL COMMENT '配置JSON',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-配置-通用';

-- ----------------------------
-- Records of base_config
-- ----------------------------
BEGIN;
INSERT INTO `base_config` (`id`, `biz`, `type`, `data`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, 'base_log_api', '1', '[{\"sort\": 0, \"width\": 70, \"dataIndex\": \"id\", \"tcChecked\": true, \"tcRequired\": false}, {\"sort\": 1, \"width\": 100, \"dataIndex\": \"crud\", \"tcChecked\": true}, {\"sort\": 2, \"width\": 100, \"dataIndex\": \"biz\", \"tcChecked\": true}, {\"sort\": 3, \"width\": 0, \"dataIndex\": \"opr\", \"tcChecked\": true}, {\"sort\": 4, \"dataIndex\": \"url\", \"tcChecked\": false}, {\"sort\": 5, \"width\": 70, \"dataIndex\": \"method\", \"tcChecked\": true}, {\"sort\": 6, \"width\": 100, \"dataIndex\": \"agent\", \"tcChecked\": false}, {\"sort\": 7, \"width\": 100, \"dataIndex\": \"os\", \"tcChecked\": false}, {\"sort\": 8, \"width\": 100, \"dataIndex\": \"browser\", \"tcChecked\": true}, {\"sort\": 9, \"width\": 120, \"dataIndex\": \"version\", \"tcChecked\": true}, {\"sort\": 10, \"width\": 60, \"dataIndex\": \"mobile\", \"tcChecked\": true}, {\"sort\": 11, \"width\": 80, \"dataIndex\": \"faFrom\", \"tcChecked\": true}, {\"sort\": 12, \"width\": 80, \"dataIndex\": \"versionCode\", \"tcChecked\": true}, {\"sort\": 13, \"width\": 80, \"dataIndex\": \"versionName\", \"tcChecked\": true}, {\"sort\": 14, \"width\": 90, \"dataIndex\": \"reqSize\", \"tcChecked\": true}, {\"sort\": 15, \"width\": 90, \"dataIndex\": \"retSize\", \"tcChecked\": true}, {\"sort\": 16, \"width\": 90, \"dataIndex\": \"duration\", \"tcChecked\": true}, {\"sort\": 17, \"width\": 70, \"dataIndex\": \"pro\", \"tcChecked\": true}, {\"sort\": 18, \"width\": 70, \"dataIndex\": \"city\", \"tcChecked\": true}, {\"sort\": 19, \"width\": 150, \"dataIndex\": \"addr\", \"tcChecked\": true}, {\"sort\": 20, \"width\": 80, \"dataIndex\": \"retStatus\", \"tcChecked\": true}, {\"sort\": 21, \"width\": 160, \"dataIndex\": \"crtTime\", \"tcChecked\": true}, {\"sort\": 22, \"width\": 120, \"dataIndex\": \"crtUser\"}, {\"sort\": 23, \"width\": 100, \"dataIndex\": \"crtName\", \"tcChecked\": false}, {\"sort\": 24, \"width\": 150, \"dataIndex\": \"crtHost\"}, {\"sort\": 25, \"width\": 120, \"dataIndex\": \"menu\", \"tcRequired\": true}]', '2023-01-30 14:30:16', '1', '超级管理员', '192.168.0.102', '2023-01-30 14:30:16', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_config` (`id`, `biz`, `type`, `data`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (2, 'HOME_LAYOUT', 'LAYOUT', '[{\"h\": 3, \"i\": \"HelloBanner\", \"w\": 16, \"x\": 0, \"y\": 0, \"moved\": false, \"static\": false}]', '2023-02-14 17:08:17', '1', '超级管理员', '127.0.0.1', '2023-02-14 17:08:18', '1', '超级管理员', '127.0.0.1', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_config_scene
-- ----------------------------
DROP TABLE IF EXISTS `base_config_scene`;
CREATE TABLE `base_config_scene` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `biz` varchar(255) NOT NULL COMMENT '业务模块',
  `name` varchar(255) NOT NULL COMMENT '场景名称',
  `data` json NOT NULL COMMENT '配置JSON',
  `system` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统',
  `default_scene` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认',
  `hide` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `sort` int(11) NOT NULL COMMENT '排序ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-配置-查询场景';

-- ----------------------------
-- Records of base_config_scene
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_config_sys
-- ----------------------------
DROP TABLE IF EXISTS `base_config_sys`;
CREATE TABLE `base_config_sys` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `data` json NOT NULL COMMENT '配置JSON',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-配置-系统配置';

-- ----------------------------
-- Records of base_config_sys
-- ----------------------------
BEGIN;
INSERT INTO `base_config_sys` (`id`, `data`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (2, '{\"cop\": \"Dward\", \"logo\": \"44314b38e94a661402fedc5087bc47f9\", \"title\": \"文件管理平台\", \"subTitle\": \"文件管理平台\", \"portalLink\": null, \"logoWithText\": \"7799d8522d7d0fc0df91190ee31c7005\", \"safeCaptchaOn\": true, \"storeLocalPath\": \"/Users/xupengfei/tmp/file\", \"safePasswordType\": 1, \"safePasswordLenMax\": 30, \"safePasswordLenMin\": 3, \"safeRegistrationOn\": true}', '2023-02-06 09:59:40', '1', '超级管理员', '127.0.0.1', '2023-02-06 09:59:41', '1', '超级管理员', '127.0.0.1', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_department
-- ----------------------------
DROP TABLE IF EXISTS `base_department`;
CREATE TABLE `base_department` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '部门名称',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `parent_id` varchar(32) NOT NULL COMMENT '父部门ID',
  `sort` int(11) DEFAULT '1' COMMENT '排序',
  `type` varchar(255) DEFAULT NULL COMMENT '类型',
  `manager_id` varchar(32) DEFAULT NULL COMMENT '负责人ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-部门';

-- ----------------------------
-- Records of base_department
-- ----------------------------
BEGIN;
INSERT INTO `base_department` (`id`, `name`, `description`, `parent_id`, `sort`, `type`, `manager_id`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES ('1', '办公室', '', '0', 1, NULL, '1', '2020-06-26 16:07:35', '1', 'admin', '123.116.43.116', '2020-06-26 16:07:35', '1', 'admin', '123.116.43.116', 0);
INSERT INTO `base_department` (`id`, `name`, `description`, `parent_id`, `sort`, `type`, `manager_id`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES ('c1eafdca1d4bd02b90c4cd15e3528e66', '部门1', NULL, '1', 0, NULL, NULL, '2022-12-08 17:19:03', '1', '超级管理员', '127.0.0.1', '2022-12-08 21:49:02', '1', '超级管理员', '221.231.169.192', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_dict
-- ----------------------------
DROP TABLE IF EXISTS `base_dict`;
CREATE TABLE `base_dict` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `code` varchar(255) NOT NULL COMMENT '编码',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `parent_id` int(11) NOT NULL COMMENT '上级节点',
  `sort_id` int(11) DEFAULT '0' COMMENT '排序ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `options` json DEFAULT NULL COMMENT '字典数组{value:1,label:名称,deleted:是否删除}',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-字典分类';

-- ----------------------------
-- Records of base_dict
-- ----------------------------
BEGIN;
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (2, 'common', '常用字典', 0, 3, NULL, '[]', '2022-12-10 18:29:12', '1', 'admin', '127.0.0.1', '2022-12-10 18:29:13', '1', '超级管理员', '192.168.58.1', 1);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (6, 'common_sex', '性别', 12, 0, NULL, '[{\"id\": 1, \"label\": \"女\", \"value\": \"0\", \"deleted\": false}, {\"id\": 2, \"label\": \"男\", \"value\": \"1\", \"deleted\": false}, {\"id\": 3, \"label\": \"保密\", \"value\": \"2\", \"deleted\": false}]', '2022-12-10 09:56:02', '1', 'admin', '127.0.0.1', '2020-06-19 09:50:36', '1', 'admin', '114.242.249.111', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (7, 'common_education', '学历', 12, 2, NULL, '[{\"id\": 1, \"label\": \"小学\", \"value\": \"1\", \"deleted\": false}, {\"id\": 2, \"label\": \"中学\", \"value\": \"2\", \"deleted\": false}, {\"id\": 3, \"label\": \"高中\", \"value\": \"3\", \"deleted\": false}, {\"id\": 4, \"label\": \"大学\", \"value\": \"4\", \"deleted\": false}, {\"id\": 5, \"label\": \"博士\", \"value\": \"6\", \"deleted\": false}, {\"id\": 6, \"label\": \"研究生\", \"value\": \"5\", \"deleted\": false}]', '2022-12-10 09:56:03', '1', 'admin', '127.0.0.1', '2020-06-19 09:50:45', '1', 'admin', '114.242.249.111', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (8, 'common_politics', '政治面貌', 12, 3, NULL, '[{\"id\": 0, \"label\": \"群众\", \"value\": \"1\", \"deleted\": false}, {\"id\": 1, \"label\": \"团员\", \"value\": \"2\", \"deleted\": false}, {\"id\": 2, \"label\": \"党员\", \"value\": \"3\", \"deleted\": false}]', '2022-12-10 09:56:05', '1', 'admin', '127.0.0.1', '2020-06-19 09:50:52', '1', 'admin', '114.242.249.111', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (9, 'group_user_type', '分组用户类型', 12, 4, NULL, '[{\"id\": 0, \"label\": \"领导\", \"value\": \"leader\", \"deleted\": false}, {\"id\": 1, \"label\": \"员工\", \"value\": \"member\", \"deleted\": false}]', '2022-12-10 09:56:05', '1', 'admin', '127.0.0.1', '2020-06-19 09:50:59', '1', 'admin', '114.242.249.111', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (10, 'common_area', '地区', 0, 1, NULL, '[]', '2022-12-10 09:56:07', '1', 'admin', '127.0.0.1', '2020-11-05 15:21:38', '1', 'admin', '120.243.220.191', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (11, 'common_area_level', '层级', 10, 1, NULL, '[{\"id\": 0, \"label\": \"省\", \"value\": \"0\", \"deleted\": false}, {\"id\": 1, \"label\": \"市\", \"value\": \"1\", \"deleted\": false}, {\"id\": 2, \"label\": \"县\", \"value\": \"2\", \"deleted\": false}, {\"id\": 3, \"label\": \"乡\", \"value\": \"3\", \"deleted\": false}, {\"id\": 4, \"label\": \"村\", \"value\": \"4\", \"deleted\": false}]', '2022-12-10 09:56:08', '1', 'admin', '127.0.0.1', '2019-08-21 10:13:38', '1', 'admin', '127.0.0.1', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (12, 'common_user', '账户字典', 0, 0, NULL, '[]', '2022-12-10 09:56:09', '1', 'admin', '127.0.0.1', '2019-10-30 14:07:45', '1', 'admin', '127.0.0.1', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (13, 'common_user_status', '账户状态', 12, 1, NULL, '[{\"id\": 0, \"label\": \"有效\", \"value\": \"1\", \"deleted\": false}, {\"id\": 1, \"label\": \"冻结\", \"value\": \"0\", \"deleted\": false}]', '2022-12-10 09:56:10', '1', 'admin', '127.0.0.1', '2019-10-30 15:09:06', '1', 'admin', '127.0.0.1', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (20, 'sys_file_download', '系统文件下载', 0, 4, '系统文件下载：包括文件模板、常用文件', '[]', '2022-12-10 18:29:15', '1', 'admin', '127.0.0.1', '2022-12-10 18:29:16', '1', '超级管理员', '192.168.58.1', 1);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (48, 'base_dict', '基础字典', 0, 2, NULL, '[]', '2022-12-10 09:56:12', '1', 'admin', '120.243.220.191', '2020-11-05 15:21:38', '1', 'admin', '120.243.220.191', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (49, 'base_dict_bool', '是否', 48, 0, NULL, '[{\"id\": 0, \"label\": \"是\", \"value\": \"1\", \"deleted\": false}, {\"id\": 1, \"label\": \"否\", \"value\": \"0\", \"deleted\": false}]', '2022-12-10 09:56:13', '1', 'admin', '120.243.220.191', '2021-03-25 11:40:35', '1', 'admin', '127.0.0.1', 0);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (54, 'system', '系统设置', 0, 5, NULL, '[{\"id\": 0, \"label\": \"system:title\", \"value\": \"Fa Admin\", \"deleted\": false}, {\"id\": 1, \"label\": \"system:logo\", \"value\": \"818e4fdeb5a7a1e5cc0b492cc76a077a\", \"deleted\": false}, {\"id\": 2, \"label\": \"system:portal:logoWithText\", \"value\": \"0fea6b8a396a06a4c90776ded510bafe\", \"deleted\": false}, {\"id\": 3, \"label\": \"system:portal:link\", \"value\": \"http://xxx.xxx.com\", \"deleted\": false}, {\"id\": 4, \"label\": \"system:phpRedisAdmin\", \"value\": \"https://fa.dward.cn/phpRedisAdmin\", \"deleted\": false}, {\"id\": 5, \"label\": \"system:subTitle\", \"value\": \"简单、易维护的后台管理系统\", \"deleted\": false}, {\"id\": 6, \"label\": \"system:socketUrl\", \"value\": \"fa.socket.dward.cn\", \"deleted\": false}]', '2022-12-13 13:40:44', '1', 'admin', '127.0.0.1', '2022-12-13 13:40:44', '1', '超级管理员', '221.231.188.211', 1);
INSERT INTO `base_dict` (`id`, `code`, `name`, `parent_id`, `sort_id`, `description`, `options`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (55, 'test', 'test', 0, 6, 'cccc', '[{\"id\": 1, \"label\": \"昨天\", \"value\": \"1\", \"deleted\": false}, {\"id\": 2, \"label\": \"今天\", \"value\": \"2\", \"deleted\": false}, {\"id\": 3, \"label\": \"明天\", \"value\": \"3\", \"deleted\": false}, {\"id\": 4, \"label\": \"后天\", \"value\": \"4\", \"deleted\": true}, {\"id\": 5, \"label\": \"后天\", \"value\": \"4\", \"deleted\": false}]', '2022-12-11 20:41:13', '1', '超级管理员', '192.168.58.1', '2022-12-11 20:41:16', '1', '超级管理员', '192.168.58.1', 1);
COMMIT;

-- ----------------------------
-- Table structure for base_entity_log
-- ----------------------------
DROP TABLE IF EXISTS `base_entity_log`;
CREATE TABLE `base_entity_log` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `biz_type` varchar(255) NOT NULL COMMENT '业务类型',
  `biz_id` varchar(255) NOT NULL COMMENT '业务ID',
  `action` tinyint(4) NOT NULL COMMENT '动作1新增/2更新/3删除',
  `content` text COMMENT '动作内容',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-实体变更日志';

-- ----------------------------
-- Records of base_entity_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_file_save
-- ----------------------------
DROP TABLE IF EXISTS `base_file_save`;
CREATE TABLE `base_file_save` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `url` varchar(512) NOT NULL COMMENT '文件访问地址',
  `size` bigint(20) NOT NULL COMMENT '文件大小，单位字节',
  `filename` varchar(255) NOT NULL COMMENT '文件名',
  `original_filename` varchar(255) NOT NULL COMMENT '原始文件名',
  `base_path` varchar(255) NOT NULL COMMENT '基础存储路径',
  `path` varchar(255) NOT NULL COMMENT '存储路径',
  `ext` varchar(32) NOT NULL COMMENT '文件扩展名',
  `content_type` varchar(255) NOT NULL COMMENT 'MIME类型',
  `platform` varchar(32) NOT NULL COMMENT '存储平台',
  `th_url` varchar(512) DEFAULT NULL COMMENT '缩略图访问路径',
  `th_filename` varchar(255) DEFAULT NULL COMMENT '缩略图名称',
  `th_size` bigint(20) DEFAULT NULL COMMENT '缩略图大小，单位字节',
  `th_content_type` varchar(32) DEFAULT NULL COMMENT '缩略图MIME类型',
  `object_id` varchar(32) DEFAULT NULL COMMENT '文件所属对象id',
  `object_type` varchar(32) DEFAULT NULL COMMENT '文件所属对象类型，例如用户头像，评价图片',
  `attr` text COMMENT '附加属性',
  `md5` varchar(32) DEFAULT NULL COMMENT '文件MD5',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-用户文件表';

-- ----------------------------
-- Records of base_file_save
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_job
-- ----------------------------
DROP TABLE IF EXISTS `base_job`;
CREATE TABLE `base_job` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `job_name` varchar(255) NOT NULL COMMENT '任务名称',
  `cron` varchar(255) NOT NULL COMMENT 'cron表达式',
  `status` tinyint(1) NOT NULL COMMENT '状态:0未启动false/1启动true',
  `clazz_path` varchar(255) NOT NULL COMMENT '任务执行方法',
  `job_desc` varchar(255) DEFAULT NULL COMMENT '任务描述',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-系统定时任务';

-- ----------------------------
-- Records of base_job
-- ----------------------------
BEGIN;
INSERT INTO `base_job` (`id`, `job_name`, `cron`, `status`, `clazz_path`, `job_desc`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, '测试任务1', '0 0/5 * * * ?', 0, 'com.faber.config.quartz.customer.JobDemo1', '测试任务111111', '2022-09-29 15:46:31', '1', 'admin', '127.0.0.1', '2022-09-07 17:22:54', '1', 'admin', '127.0.0.1', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_job_log
-- ----------------------------
DROP TABLE IF EXISTS `base_job_log`;
CREATE TABLE `base_job_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `job_id` int(11) NOT NULL COMMENT '任务ID',
  `begin_time` datetime NOT NULL COMMENT '创建时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint(4) NOT NULL COMMENT '执行结果：1-执行中/2-成功/9-失败',
  `duration` int(11) unsigned NOT NULL COMMENT '执行花费时间',
  `err_msg` text COMMENT '错误日志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-定时任务日志';

-- ----------------------------
-- Records of base_job_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_log_api
-- ----------------------------
DROP TABLE IF EXISTS `base_log_api`;
CREATE TABLE `base_log_api` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `biz` varchar(255) DEFAULT NULL COMMENT '模块',
  `opr` varchar(255) DEFAULT NULL COMMENT '操作',
  `crud` char(1) DEFAULT NULL COMMENT 'CRUD类型',
  `url` text NOT NULL COMMENT '请求URL',
  `method` varchar(10) NOT NULL COMMENT '请求类型',
  `agent` text COMMENT '访问客户端',
  `os` varchar(255) DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `version` varchar(255) DEFAULT NULL COMMENT '浏览器版本',
  `fa_from` varchar(255) DEFAULT NULL COMMENT '客户端来源',
  `version_code` bigint(20) DEFAULT NULL COMMENT '客户端版本号',
  `version_name` varchar(255) DEFAULT NULL COMMENT '客户端版本名',
  `mobile` tinyint(1) DEFAULT NULL COMMENT '是否为移动终端',
  `duration` int(11) NOT NULL COMMENT '请求花费时间',
  `pro` varchar(10) DEFAULT NULL COMMENT '省',
  `city` varchar(10) DEFAULT NULL COMMENT '市',
  `addr` varchar(255) DEFAULT NULL COMMENT '地址',
  `request` longtext COMMENT '请求内容',
  `req_size` int(11) DEFAULT NULL COMMENT '请求体大小',
  `response` longtext COMMENT '返回内容',
  `ret_size` int(11) DEFAULT NULL COMMENT '返回内容大小',
  `ret_status` int(11) NOT NULL COMMENT '返回码',
  `crt_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) DEFAULT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) DEFAULT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-URL请求日志';

-- ----------------------------
-- Records of base_log_api
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_log_login
-- ----------------------------
DROP TABLE IF EXISTS `base_log_login`;
CREATE TABLE `base_log_login` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agent` text COMMENT '访问客户端',
  `os` varchar(255) DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `version` varchar(255) DEFAULT NULL COMMENT '浏览器版本',
  `mobile` tinyint(1) DEFAULT NULL COMMENT '是否为移动终端',
  `pro` varchar(10) DEFAULT NULL COMMENT '省',
  `city` varchar(10) DEFAULT NULL COMMENT '市',
  `addr` varchar(255) DEFAULT NULL COMMENT '地址',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-登录日志';

-- ----------------------------
-- Records of base_log_login
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_msg
-- ----------------------------
DROP TABLE IF EXISTS `base_msg`;
CREATE TABLE `base_msg` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `from_user_name` varchar(255) NOT NULL COMMENT '来源用户',
  `from_user_id` varchar(32) NOT NULL COMMENT '来源用户ID',
  `to_user_name` varchar(255) NOT NULL COMMENT '接收用户',
  `to_user_id` varchar(32) NOT NULL COMMENT '接收用户ID',
  `content` longtext COMMENT '消息内容',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `buzz_type` int(11) DEFAULT NULL COMMENT '业务类型',
  `buzz_id` varchar(255) DEFAULT NULL COMMENT '业务ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-消息';

-- ----------------------------
-- Records of base_msg
-- ----------------------------
BEGIN;
INSERT INTO `base_msg` (`id`, `from_user_name`, `from_user_id`, `to_user_name`, `to_user_id`, `content`, `is_read`, `read_time`, `buzz_type`, `buzz_id`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, '1', '1', '1', '1', '1', 1, '2022-12-30 13:55:55', 1, '1', '2022-12-30 13:59:05', '1', '1', '1', NULL, NULL, NULL, NULL, 1);
COMMIT;

-- ----------------------------
-- Table structure for base_notice
-- ----------------------------
DROP TABLE IF EXISTS `base_notice`;
CREATE TABLE `base_notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(50) NOT NULL COMMENT '标题',
  `content` varchar(255) NOT NULL COMMENT '内容',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有效',
  `strong_notice` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否强提醒',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-通知与公告';

-- ----------------------------
-- Records of base_notice
-- ----------------------------
BEGIN;
INSERT INTO `base_notice` (`id`, `title`, `content`, `status`, `strong_notice`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, 'test', 'testtesttesttesttest', 1, 1, '2022-09-25 10:24:32', '1', '超级管理员1', '192.168.58.1', '2022-12-05 17:24:59', '1', '超级管理员', '127.0.0.1', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_rbac_menu
-- ----------------------------
DROP TABLE IF EXISTS `base_rbac_menu`;
CREATE TABLE `base_rbac_menu` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `parent_id` int(11) NOT NULL COMMENT '父级ID',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `sort` int(11) NOT NULL COMMENT '排序',
  `level` tinyint(4) NOT NULL COMMENT '菜单等级：0-模块/1-菜单/9-按钮',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标标识',
  `status` tinyint(1) NOT NULL COMMENT '是否启用0-禁用/1-启用',
  `link_type` tinyint(4) NOT NULL COMMENT '链接类型【1-内部链接(默认)2-外部链接】',
  `link_url` varchar(255) NOT NULL COMMENT '链接地址【pathinfo#method】',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-菜单表';

-- ----------------------------
-- Records of base_rbac_menu
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, 0, '组件示例', 1, 0, 'icons', 1, 1, '/admin/demo', '2023-01-03 16:07:53', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (2, 52, '工作台', 0, 1, 'desktop', 1, 1, '/admin/home/desktop', '2023-01-03 16:07:53', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (3, 0, '系统管理', 4, 0, 'fa-solid fa-gear', 1, 1, '/admin/system', '2022-09-19 16:56:23', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (4, 3, '智能人事', 0, 1, 'fa-solid fa-users', 1, 1, '/admin/system/hr', '2022-09-19 16:58:28', '1', '超级管理员1', '192.168.1.107', '2022-09-19 20:48:43', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (5, 3, '系统管理', 1, 1, 'fa-solid fa-gears', 1, 1, '/admin/system/base', '2022-09-19 16:58:56', '1', '超级管理员1', '192.168.1.107', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (6, 3, '个人中心', 2, 1, 'fa-solid fa-user', 1, 1, '/admin/system/account', '2022-09-19 16:59:40', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (7, 4, '用户管理', 0, 1, '', 1, 1, '/admin/system/hr/user', '2022-09-19 17:02:08', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:43', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (8, 4, '部门管理', 1, 1, '', 1, 1, '/admin/system/hr/department', '2022-09-19 17:02:30', '1', '超级管理员1', '127.0.0.1', '2022-09-25 09:49:26', '1', '超级管理员1', '192.168.58.1', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (9, 4, '角色权限管理', 2, 1, '', 1, 1, '/admin/system/hr/role', '2022-09-19 17:12:26', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:43', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (10, 5, '菜单管理', 0, 1, NULL, 1, 1, '/admin/system/base/menuV2', '2022-09-19 17:14:17', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (11, 5, '字典管理', 1, 1, NULL, 1, 1, '/admin/system/base/dict', '2022-09-19 17:14:44', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (12, 5, '中国地区管理', 3, 1, NULL, 1, 1, '/admin/system/base/area', '2022-09-19 17:15:00', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (13, 5, '定时任务', 4, 1, NULL, 1, 1, '/admin/system/base/job', '2022-09-19 17:15:14', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (14, 5, '请求日志', 5, 1, NULL, 1, 1, '/admin/system/base/logApi', '2022-09-19 17:15:33', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (15, 5, '系统公告', 7, 1, NULL, 1, 1, '/admin/system/base/notice', '2022-09-19 17:16:13', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (16, 5, '登录日志', 6, 1, NULL, 1, 1, '/admin/system/base/logLogin', '2022-09-19 17:16:36', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (17, 6, '基本信息', 0, 1, NULL, 1, 1, '/admin/system/account/base', '2022-09-19 17:17:05', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (18, 6, '更新密码', 1, 1, NULL, 1, 1, '/admin/system/account/security', '2022-09-19 17:17:52', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (19, 6, '消息中心', 2, 1, NULL, 1, 1, '/admin/system/account/msg', '2022-09-19 17:18:06', '1', '超级管理员1', '127.0.0.1', '2022-09-19 20:48:33', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (20, 23, '图标', 0, 1, '', 1, 1, '/admin/demo/biz/icon', '2023-01-03 16:07:53', '1', '超级管理员1', '192.168.58.1', '2022-09-24 21:56:00', '1', '超级管理员1', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (21, 23, '权限测试', 1, 1, NULL, 1, 1, '/admin/demo/biz/authTest', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2023-01-03 16:10:01', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (22, 21, '权限按钮', 0, 9, NULL, 1, 1, '/admin/demo/biz/authTest@button1', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2023-01-03 16:17:17', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (23, 1, '通用组件', 0, 1, 'fa-solid fa-cubes', 1, 1, '/admin/demo/biz', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2023-01-03 16:11:38', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (24, 23, '地址选择器', 2, 1, NULL, 1, 1, '/admin/demo/biz/areaCascader', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2023-01-03 16:10:01', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (25, 23, '富文本编辑器', 3, 1, NULL, 1, 1, '/admin/demo/biz/tinymce', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2023-01-03 16:10:01', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (26, 3, '系统监控', 3, 1, 'fa-solid fa-computer', 1, 1, '/admin/system/monitor', '2022-10-17 15:16:48', '1', '超级管理员', '114.222.120.178', '2022-10-17 15:19:11', '1', '超级管理员', '114.222.120.178', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (27, 26, '数据监控', 0, 1, NULL, 1, 1, '/admin/system/monitor/druid', '2022-10-17 15:17:29', '1', '超级管理员', '114.222.120.178', '2022-10-17 15:17:41', '1', '超级管理员', '114.222.120.178', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (28, 26, '服务监控', 1, 1, NULL, 1, 1, '/admin/system/monitor/server', '2022-10-17 15:23:40', '1', '超级管理员', '114.222.120.178', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (29, 23, '文件上传', 4, 1, NULL, 1, 1, '/admin/demo/biz/fileUploader', '2023-01-03 16:07:53', '1', '超级管理员', '114.222.229.224', '2023-01-03 16:10:01', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (30, 26, 'Redis管理', 2, 1, NULL, 1, 1, '/admin/system/monitor/redis', '2022-11-29 17:33:43', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (31, 23, '级联选择', 6, 1, NULL, 1, 1, '/admin/demo/biz/cascader', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 21:52:00', '1', '超级管理员', '221.231.192.204', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (32, 23, '字典选择器', 5, 1, NULL, 1, 1, '/admin/demo/biz/dict', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 21:52:00', '1', '超级管理员', '221.231.192.204', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (33, 23, '拖动排序', 9, 1, NULL, 1, 1, '/admin/demo/biz/drag', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 21:51:43', '1', '超级管理员', '221.231.192.204', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (34, 23, '远程数据选择', 8, 1, NULL, 1, 1, '/admin/demo/biz/select', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 17:28:54', '1', '超级管理员', '221.231.188.211', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (35, 39, '表格查询', 0, 1, NULL, 1, 1, '/admin/demo/table/table', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 17:30:06', '1', '超级管理员', '221.231.188.211', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (36, 23, '远程数据Tree', 7, 1, NULL, 1, 1, '/admin/demo/biz/tree', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 17:30:06', '1', '超级管理员', '221.231.188.211', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (37, 39, '组合查询', 1, 1, NULL, 1, 1, '/admin/demo/table/conditionQuery', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2022-11-30 17:30:53', '1', '超级管理员', '221.231.188.211', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (38, 23, 'PDF预览', 11, 1, NULL, 1, 1, '/admin/demo/biz/pdfView', '2023-01-03 16:07:53', '1', '超级管理员', '221.231.188.211', '2023-01-03 16:10:02', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (39, 1, '表格查询', 1, 1, 'fa-solid fa-table', 1, 1, '/admin/demo/table', '2023-01-03 16:07:53', '1', '超级管理员', '192.168.58.1', '2022-11-30 21:54:57', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (40, 1, '进阶功能', 2, 0, 'fa-solid fa-rocket', 1, 1, '/admin/demo/advance', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', '2022-12-06 13:54:47', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (41, 40, 'socket连接', 0, 1, NULL, 1, 1, '/admin/demo/advance/socket', '2023-01-03 16:07:53', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (42, 40, 'redis缓存', 1, 1, NULL, 1, 1, '/admin/demo/advance/redis', '2023-01-03 16:07:53', '1', '超级管理员', '192.168.58.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (43, 5, '系统配置', 2, 1, NULL, 1, 1, '/admin/system/base/config', '2022-12-11 22:39:02', '1', '超级管理员', '192.168.58.1', '2022-12-11 22:40:21', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (44, 0, '文档管理', 2, 0, 'floppy-disk', 1, 1, '/admin/disk', '2022-12-22 09:48:01', '1', '超级管理员', '192.168.58.1', '2022-12-22 09:48:03', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (45, 44, '文件网盘', 1, 1, 'laptop-file', 1, 1, '/admin/disk/store', '2022-12-22 09:49:00', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:33:29', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (46, 45, '全部文件', 0, 1, 'box-archive', 1, 1, '/admin/disk/store/alls', '2022-12-27 11:10:58', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:10:59', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (47, 45, '最近文件', 1, 1, 'clock', 1, 1, '/admin/disk/store/recent', '2022-12-27 11:11:01', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:01', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (48, 45, '星标文件', 2, 1, 'star', 1, 1, '/admin/disk/store/starred', '2022-12-27 11:11:03', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:03', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (49, 45, '回收站', 3, 1, 'trash-can', 1, 1, '/admin/disk/store/recycle', '2022-12-27 11:11:04', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:05', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (50, 46, '新增库', 0, 9, NULL, 1, 1, '/admin/disk/store/alls/bucket@add', '2022-12-23 10:22:37', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:22:39', '1', '超级管理员', '192.168.58.1', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (51, 44, '文件仓库', 0, 1, 'store', 1, 1, '/admin/disk/buckets', '2022-12-23 10:23:29', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:23:33', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (52, 0, '首页', 0, 0, 'house', 1, 1, '/admin/home', '2023-01-03 16:11:35', '1', '超级管理员', '192.168.0.111', '2023-01-03 16:11:38', '1', '超级管理员', '192.168.0.111', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (53, 23, '路由选择', 10, 1, NULL, 1, 1, '/admin/demo/biz/route', '2023-01-04 14:53:10', '1', '超级管理员', '127.0.0.1', '2023-01-04 14:53:16', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (54, 23, 'tailwindcss', 12, 1, NULL, 1, 1, '/admin/demo/biz/tailwindcss', '2023-01-06 14:20:51', '1', '超级管理员', '127.0.0.1', '2023-01-06 14:23:50', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (55, 23, '日期选择', 13, 1, NULL, 1, 1, '/admin/demo/biz/datepicker', '2023-01-06 16:44:21', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (56, 0, 'APP管理', 3, 0, 'mobile-retro', 1, 1, '/admin/app', '2023-01-20 14:22:45', '1', '超级管理员', '127.0.0.1', '2023-01-20 14:22:44', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (57, 56, 'APP版本管理', 0, 1, 'store', 1, 1, '/admin/app/app/apk', '2023-01-20 14:23:14', '1', '超级管理员', '127.0.0.1', '2023-01-20 14:23:13', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (58, 56, 'APP崩溃日志', 1, 1, 'burst', 1, 1, '/admin/app/crash/apkCrash', '2023-01-20 14:23:29', '1', '超级管理员', '127.0.0.1', '2023-01-20 14:23:29', '1', '超级管理员', '127.0.0.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (59, 6, 'Token管理', 3, 1, NULL, 1, 1, '/admin/system/account/token', '2023-01-24 20:25:33', '1', '超级管理员', '127.0.0.1', '2023-01-24 20:25:33', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (60, 5, '附件管理', 8, 1, NULL, 1, 1, '/admin/system/base/fileSave', '2023-02-03 11:02:48', '1', '超级管理员', '127.0.0.1', '2023-02-03 11:02:47', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (61, 23, '页面标签', 14, 1, NULL, 1, 1, '/admin/demo/biz/openTabs', '2023-02-13 13:58:35', '1', '超级管理员', '127.0.0.1', '2023-02-13 13:58:34', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (62, 23, '右键菜单', 15, 1, NULL, 1, 1, '/admin/demo/biz/contextMenu', '2023-02-13 14:01:04', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:04', NULL, NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for base_rbac_role
-- ----------------------------
DROP TABLE IF EXISTS `base_rbac_role`;
CREATE TABLE `base_rbac_role` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '角色名称',
  `remarks` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(1) NOT NULL COMMENT '是否启用',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-角色表';

-- ----------------------------
-- Records of base_rbac_role
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_role` (`id`, `name`, `remarks`, `status`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, '超级管理员', '超级管理员', 1, '2022-09-19 17:34:00', '1', '超级管理员1', '127.0.0.1', '2022-09-19 17:34:14', '1', '超级管理员1', '127.0.0.1', 0);
INSERT INTO `base_rbac_role` (`id`, `name`, `remarks`, `status`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (2, '业务管理员', '业务管理员', 1, '2022-09-28 15:04:59', '1', '超级管理员', '127.0.0.1', '2022-12-05 08:34:10', '1', '超级管理员', '14.205.11.151', 0);
INSERT INTO `base_rbac_role` (`id`, `name`, `remarks`, `status`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (3, '默认用户角色', '默认用户角色', 1, '2023-02-06 11:34:53', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:34:52', NULL, NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for base_rbac_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `base_rbac_role_menu`;
CREATE TABLE `base_rbac_role_menu` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  `menu_id` int(11) NOT NULL COMMENT '权限ID',
  `half_checked` tinyint(1) NOT NULL COMMENT '是否半勾选',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1378 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-角色权限对应表';

-- ----------------------------
-- Records of base_rbac_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (870, 2, 1, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (871, 2, 2, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (872, 2, 23, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (873, 2, 39, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (874, 2, 40, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (875, 2, 20, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (876, 2, 21, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (877, 2, 24, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (878, 2, 25, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (879, 2, 29, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (880, 2, 32, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (881, 2, 31, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (882, 2, 36, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (883, 2, 34, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (884, 2, 33, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (885, 2, 38, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (886, 2, 35, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (887, 2, 37, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (888, 2, 41, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (889, 2, 42, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (890, 2, 22, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (891, 2, 44, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (892, 2, 51, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (893, 2, 45, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (894, 2, 6, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (895, 2, 17, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (896, 2, 18, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (897, 2, 19, 0, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (898, 2, 3, 1, '2022-12-28 17:09:59', '1', '超级管理员', '192.168.0.101', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1256, 3, 6, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1257, 3, 17, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1258, 3, 18, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1259, 3, 19, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1260, 3, 59, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1261, 3, 52, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1262, 3, 2, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1263, 3, 44, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1264, 3, 51, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1265, 3, 45, 0, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1266, 3, 3, 1, '2023-02-06 11:35:19', '1', '超级管理员', '127.0.0.1', '2023-02-06 11:35:18', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1322, 1, 52, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1323, 1, 2, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1324, 1, 44, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1325, 1, 51, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1326, 1, 45, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1327, 1, 56, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1328, 1, 57, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1329, 1, 58, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1330, 1, 3, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1331, 1, 4, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1332, 1, 5, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1333, 1, 6, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1334, 1, 26, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1335, 1, 7, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1336, 1, 9, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1337, 1, 10, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1338, 1, 11, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1339, 1, 43, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1340, 1, 12, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1341, 1, 13, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1342, 1, 14, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1343, 1, 16, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1344, 1, 15, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1345, 1, 60, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1346, 1, 17, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1347, 1, 18, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1348, 1, 19, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1349, 1, 59, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1350, 1, 27, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1351, 1, 28, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1352, 1, 30, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1353, 1, 1, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1354, 1, 23, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:15', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1355, 1, 39, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1356, 1, 40, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1357, 1, 20, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1358, 1, 21, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1359, 1, 24, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1360, 1, 25, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1361, 1, 29, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1362, 1, 32, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1363, 1, 31, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1364, 1, 36, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1365, 1, 34, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1366, 1, 33, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1367, 1, 53, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1368, 1, 38, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1369, 1, 54, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1370, 1, 55, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1371, 1, 61, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1372, 1, 62, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1373, 1, 35, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1374, 1, 37, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1375, 1, 41, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1376, 1, 42, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_role_menu` (`id`, `role_id`, `menu_id`, `half_checked`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1377, 1, 22, 0, '2023-02-13 14:01:16', '1', '超级管理员', '127.0.0.1', '2023-02-13 14:01:16', NULL, NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for base_rbac_user_role
-- ----------------------------
DROP TABLE IF EXISTS `base_rbac_user_role`;
CREATE TABLE `base_rbac_user_role` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='BASE-用户角色关联表';

-- ----------------------------
-- Records of base_rbac_user_role
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_user_role` (`id`, `user_id`, `role_id`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (1, '1', 1, '2023-02-20 11:04:04', '1', '超级管理员', '127.0.0.1', '2023-02-20 11:04:04', NULL, NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for base_sms_code
-- ----------------------------
DROP TABLE IF EXISTS `base_sms_code`;
CREATE TABLE `base_sms_code` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `phone` varchar(15) NOT NULL COMMENT '手机号',
  `code` varchar(6) NOT NULL COMMENT '短信验证码',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-短信验证码';

-- ----------------------------
-- Records of base_sms_code
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_system_update_log
-- ----------------------------
DROP TABLE IF EXISTS `base_system_update_log`;
CREATE TABLE `base_system_update_log` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `no` varchar(255) NOT NULL COMMENT '模块编码',
  `name` varchar(255) NOT NULL COMMENT '模块名称',
  `ver` int(11) NOT NULL COMMENT '版本号',
  `ver_no` varchar(255) CHARACTER SET utf8 NOT NULL COMMENT '版本编码',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT '备注信息',
  `log` longtext COMMENT 'SQL执行内容',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-系统版本更新日志表';

-- ----------------------------
-- Records of base_system_update_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for base_user
-- ----------------------------
DROP TABLE IF EXISTS `base_user`;
CREATE TABLE `base_user` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `department_id` varchar(32) NOT NULL COMMENT '部门ID',
  `username` varchar(255) NOT NULL COMMENT '账户',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `tel` varchar(20) DEFAULT NULL COMMENT '手机号',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `sex` tinyint(4) DEFAULT NULL COMMENT '性别0-女1-男2-未知',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(1) NOT NULL COMMENT '状态：1-有效/0-锁定',
  `role_names` varchar(255) DEFAULT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `img` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `api_token` varchar(255) DEFAULT NULL COMMENT 'api token',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-用户';

-- ----------------------------
-- Records of base_user
-- ----------------------------
BEGIN;
INSERT INTO `base_user` (`id`, `department_id`, `username`, `password`, `name`, `tel`, `birthday`, `sex`, `address`, `email`, `status`, `role_names`, `description`, `img`, `api_token`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES ('1', 'c1eafdca1d4bd02b90c4cd15e3528e66', 'admin', '$2a$12$MAibDd3RbrSyB7i5m8bzMubLmBcoH/vBqSJiIElmZgalMiT9iuj6C', '超级管理员', '13811112222', '2000-01-01', 1, '南京市、江宁区、将军大道', 'faberxu@gmail.com', 1, '超级管理员', '干活12', '4dd5c89a66725f5ede372b6bb116ae3a', 'd1d6e6d1ebcb4437bd082c3046671582', '2023-02-03 19:34:30', '1', 'admin', '127.0.0.1', '2023-02-03 19:34:30', '1', 'admin', '127.0.0.1', 0);
COMMIT;

-- ----------------------------
-- Table structure for base_user_token
-- ----------------------------
DROP TABLE IF EXISTS `base_user_token`;
CREATE TABLE `base_user_token` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `valid` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有效',
  `remark` varchar(255) NOT NULL COMMENT '备注',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-用户token';

-- ----------------------------
-- Records of base_user_token
-- ----------------------------
BEGIN;
INSERT INTO `base_user_token` (`id`, `user_id`, `valid`, `remark`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES ('2392534297648a87dd4d915285d9c7ce', '1', 1, 'android', '2023-01-24 20:33:24', '1', '超级管理员', '127.0.0.1', '2023-01-24 20:33:23', '1', '超级管理员', '127.0.0.1', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
