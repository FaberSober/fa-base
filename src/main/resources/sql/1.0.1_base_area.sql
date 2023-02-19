DROP TABLE IF EXISTS `base_area`;
CREATE TABLE `base_area`  (
  `id` mediumint(7) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `level` tinyint(1) UNSIGNED NOT NULL COMMENT '层级',
  `parent_code` bigint(14) UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级行政代码',
  `area_code` bigint(14) UNSIGNED NOT NULL DEFAULT 0 COMMENT '行政代码',
  `zip_code` mediumint(6) UNSIGNED ZEROFILL NOT NULL DEFAULT 000000 COMMENT '邮政编码',
  `city_code` char(6) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '区号',
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `short_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '简称',
  `merger_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '组合名',
  `pinyin` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '拼音',
  `lng` decimal(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '经度',
  `lat` decimal(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '纬度',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx`(`area_code`) USING BTREE,
  INDEX `pidx`(`parent_code`) USING BTREE,
  INDEX `level`(`level`) USING BTREE,
  INDEX `name`(`name`) USING BTREE,
  INDEX `level_name`(`level`, `name`) USING BTREE,
  INDEX `short_name`(`short_name`) USING BTREE,
  INDEX `level_short_name`(`level`, `short_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '中国行政地区表' ROW_FORMAT = Dynamic;
