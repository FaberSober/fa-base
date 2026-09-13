-- ------------------------- info -------------------------
-- @@ver: 1_000_034
-- @@info: 增加统一工作日与交易日历基础表
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_calendar` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `calendar_code` varchar(32) NOT NULL COMMENT '日历编码',
  `name` varchar(64) NOT NULL COMMENT '日历名称',
  `calendar_type` varchar(16) NOT NULL COMMENT '日历类型：OA/交易',
  `market` varchar(16) DEFAULT NULL COMMENT '对应市场',
  `timezone` varchar(64) NOT NULL COMMENT '日期解释时区',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_base_calendar_code` (`calendar_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一工作日与交易日历定义';

CREATE TABLE IF NOT EXISTS `base_calendar_day` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `calendar_code` varchar(32) NOT NULL COMMENT '日历编码',
  `calendar_date` date NOT NULL COMMENT '日历日期',
  `day_type` varchar(24) NOT NULL COMMENT '日期类型',
  `is_open` tinyint(1) NOT NULL COMMENT '对所属日历是否开放',
  `holiday_name` varchar(128) DEFAULT NULL COMMENT '节假日或特殊日期名称',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源',
  `source_version` varchar(64) DEFAULT NULL COMMENT '来源版本或导入批次',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_base_calendar_day_code_date` (`calendar_code`, `calendar_date`) USING BTREE,
  KEY `idx_base_calendar_day_lookup` (`calendar_code`, `calendar_date`, `is_open`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一日历单日事实';

INSERT INTO `base_calendar`
    (`calendar_code`, `name`, `calendar_type`, `market`, `timezone`, `enabled`,
     `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`)
SELECT 'CN_OA', '中国 OA 工作日历', 'OA', NULL, 'Asia/Shanghai', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
WHERE NOT EXISTS (SELECT 1 FROM `base_calendar` WHERE `calendar_code` = 'CN_OA');

INSERT INTO `base_calendar`
    (`calendar_code`, `name`, `calendar_type`, `market`, `timezone`, `enabled`,
     `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`)
SELECT 'CN_A_SHARE', '沪深 A 股交易日历', 'TRADING', 'SHSZ', 'Asia/Shanghai', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
WHERE NOT EXISTS (SELECT 1 FROM `base_calendar` WHERE `calendar_code` = 'CN_A_SHARE');

INSERT INTO `base_calendar`
    (`calendar_code`, `name`, `calendar_type`, `market`, `timezone`, `enabled`,
     `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`)
SELECT 'HK_STOCK', '港股交易日历', 'TRADING', 'HK', 'Asia/Hong_Kong', 1,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
WHERE NOT EXISTS (SELECT 1 FROM `base_calendar` WHERE `calendar_code` = 'HK_STOCK');

INSERT INTO `base_calendar`
    (`calendar_code`, `name`, `calendar_type`, `market`, `timezone`, `enabled`,
     `crt_time`, `crt_user`, `crt_name`, `crt_host`, `deleted`)
SELECT 'US_STOCK', '美股交易日历（预留）', 'TRADING', 'US', 'America/New_York', 0,
       CURRENT_TIMESTAMP, '1', '系统初始化', '127.0.0.1', 0
WHERE NOT EXISTS (SELECT 1 FROM `base_calendar` WHERE `calendar_code` = 'US_STOCK');
