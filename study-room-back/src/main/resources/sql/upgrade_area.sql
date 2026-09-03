-- ============================================================
-- 升级脚本：为已存在的数据库增加「区域(房间)」层级
-- 在已有 study_room_db 上执行：  source upgrade_area.sql;
-- 幂等：可重复执行
-- ============================================================
SET NAMES utf8mb4;

-- 1. 创建区域表
CREATE TABLE IF NOT EXISTS `area` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `floor_id`    BIGINT      NOT NULL COMMENT '所属楼层ID',
  `area_name`   VARCHAR(32) NOT NULL COMMENT '区域名称(如A区/101自习室)',
  `sort_order`  INT         NOT NULL DEFAULT 1 COMMENT '排序号',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_floor` (`floor_id`),
  UNIQUE KEY `uk_floor_area` (`floor_id`, `area_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域(房间)表';

-- 2. 为 seat 表增加 area_id 列（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，用存储过程方式判断）
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'seat' AND COLUMN_NAME = 'area_id'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `seat` ADD COLUMN `area_id` BIGINT DEFAULT NULL COMMENT ''所属区域ID'' AFTER `floor_id`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 给每个楼层初始化 A区/B区 两个区域
INSERT INTO `area` (`floor_id`, `area_name`, `sort_order`, `status`, `remark`)
SELECT f.id, 'A区', 1, 1, '普通自习区'
FROM `floor` f
WHERE NOT EXISTS (SELECT 1 FROM `area` WHERE `floor_id` = f.id AND `area_name` = 'A区');

INSERT INTO `area` (`floor_id`, `area_name`, `sort_order`, `status`, `remark`)
SELECT f.id, 'B区', 2, 1, '安静自习区'
FROM `floor` f
WHERE NOT EXISTS (SELECT 1 FROM `area` WHERE `floor_id` = f.id AND `area_name` = 'B区');

-- 4. 把已有座位按座位号前半/后半分配到 A区/B区（只处理尚未分配区域的座位）
UPDATE `seat` s
JOIN (
  SELECT id, floor_id,
         ROW_NUMBER() OVER (PARTITION BY floor_id ORDER BY seat_no) AS rn,
         COUNT(*) OVER (PARTITION BY floor_id) AS cnt
  FROM `seat`
  WHERE is_deleted = 0
) t ON t.id = s.id
JOIN `area` a ON a.floor_id = s.floor_id
SET s.area_id = a.id
WHERE s.area_id IS NULL
  AND a.area_name = IF(t.rn <= CEIL(t.cnt / 2.0), 'A区', 'B区');
