-- ------------------------- info -------------------------
-- @@ver: 1_000_048
-- @@info: 增加浏览器 Web Push 订阅表
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_browser_push_subscription` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `endpoint_hash` char(64) NOT NULL COMMENT '推送地址SHA-256',
  `endpoint` text NOT NULL COMMENT '浏览器推送地址',
  `p256dh` varchar(128) NOT NULL COMMENT '客户端公钥',
  `auth` varchar(64) NOT NULL COMMENT '客户端认证密钥',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_base_browser_push_subscription_endpoint_hash` (`endpoint_hash`) USING BTREE,
  KEY `idx_base_browser_push_subscription_user` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BASE-浏览器推送订阅';
