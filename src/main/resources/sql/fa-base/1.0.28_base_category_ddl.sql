-- ------------------------- info -------------------------
-- @@ver: 1_000_028
-- @@info: 增加通用业务分类树
-- ------------------------- info -------------------------

CREATE TABLE IF NOT EXISTS `base_category` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `module` varchar(32) NOT NULL COMMENT '所属模块',
  `parent_id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '上级分类ID，0为根节点',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `description` varchar(256) DEFAULT NULL COMMENT '描述',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
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
  UNIQUE KEY `uk_base_category_module_parent_name` (`module`, `parent_id`, `name`, `deleted`) USING BTREE,
  KEY `idx_base_category_module_tree` (`module`, `parent_id`, `sort`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用业务分类';
