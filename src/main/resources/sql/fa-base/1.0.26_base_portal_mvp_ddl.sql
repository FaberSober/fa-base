-- ------------------------- info -------------------------
-- @@ver: 1_000_026
-- @@info: Portal MVP 用户准入与咨询记录
-- ------------------------- info -------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `base_user`
  ADD COLUMN `admin_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许访问后台管理端' AFTER `status`;

-- 升级前的存量用户均视为既有后台用户，避免升级后失去 Admin 访问能力。
UPDATE `base_user` SET `admin_enabled` = 1 WHERE `deleted` = 0;

CREATE TABLE IF NOT EXISTS `portal_contact_inquiry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(100) NOT NULL COMMENT '联系人',
  `company` varchar(200) DEFAULT NULL COMMENT '公司名称',
  `tel` varchar(32) NOT NULL COMMENT '联系电话',
  `email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
  `subject` varchar(200) NOT NULL COMMENT '咨询主题',
  `message` text NOT NULL COMMENT '咨询内容',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '处理状态：0待处理/1处理中/2已完成',
  `source` varchar(32) NOT NULL DEFAULT 'PORTAL' COMMENT '来源',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_portal_contact_inquiry_status` (`status`, `crt_time`) USING BTREE,
  KEY `idx_portal_contact_inquiry_tel` (`tel`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Portal-官网咨询';

SET FOREIGN_KEY_CHECKS = 1;
