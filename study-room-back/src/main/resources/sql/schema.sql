-- ============================================================
-- 自习室管理系统 数据库初始化脚本
-- 说明：请先创建数据库，再执行本脚本（可重复执行，均为幂等建表/插入）：
--   CREATE DATABASE study_room_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   USE study_room_db;
--   source schema.sql;
-- 公告表见 announcement.sql
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`            VARCHAR(32)  NOT NULL COMMENT '用户名/学号',
  `password`            VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
  `role`                TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0-学生 1-管理员',
  `status`              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `id_card`             VARCHAR(18)  DEFAULT NULL COMMENT '身份证号(用于生成初始密码)',
  `is_first_login`      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否首次登录: 0-否 1-是',
  `password_updated_at` DATETIME     DEFAULT NULL COMMENT '密码最后更新时间',
  `points`              INT          NOT NULL DEFAULT 500 COMMENT '信用积分(初始500，违约一次扣100)',
  `book_ban_until`      DATETIME     DEFAULT NULL COMMENT '积分扣至0后的禁止预约截止时间(24小时)',
  `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 楼层表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `floor` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `floor_name`   VARCHAR(32) NOT NULL COMMENT '楼层名称',
  `bg_image_url` VARCHAR(255) DEFAULT NULL COMMENT '楼层底图URL',
  `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `floor_number` INT         NOT NULL DEFAULT 1 COMMENT '楼层编号/排序',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`   TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_floor_number` (`floor_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层表';

-- ------------------------------------------------------------
-- 区域(房间)表：楼层下的自习区域/房间
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 座位表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `seat` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `floor_id`    BIGINT      NOT NULL COMMENT '所属楼层ID',
  `area_id`     BIGINT      DEFAULT NULL COMMENT '所属区域(房间)ID',
  `seat_no`     VARCHAR(16) NOT NULL COMMENT '座位编号',
  `x_coord`     INT         DEFAULT NULL COMMENT 'X坐标(前端地图)',
  `y_coord`     INT         DEFAULT NULL COMMENT 'Y坐标(前端地图)',
  `seat_type`   TINYINT     NOT NULL DEFAULT 1 COMMENT '类型: 1-普通 2-靠窗 3-带插座',
  `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0-空闲 1-已预约 2-使用中 3-维修中',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_floor` (`floor_id`, `status`),
  KEY `idx_area` (`area_id`),
  UNIQUE KEY `uk_floor_seatno` (`floor_id`, `seat_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- ------------------------------------------------------------
-- 预约表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reservation` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `seat_id`     BIGINT   NOT NULL COMMENT '座位ID',
  `start_time`  DATETIME NOT NULL COMMENT '预约开始时间',
  `end_time`    DATETIME NOT NULL COMMENT '预约结束时间',
  `status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '状态: 0-待签到 1-使用中 2-已完成 3-违约 4-已取消',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`),
  KEY `idx_seat` (`seat_id`, `status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- ------------------------------------------------------------
-- 示例数据：楼层
-- ------------------------------------------------------------
INSERT INTO `floor` (`floor_name`, `status`, `floor_number`, `remark`)
SELECT '一楼', 1, 1, '普通自习区'
WHERE NOT EXISTS (SELECT 1 FROM `floor` WHERE `floor_number` = 1);

INSERT INTO `floor` (`floor_name`, `status`, `floor_number`, `remark`)
SELECT '二楼', 1, 2, '安静自习区'
WHERE NOT EXISTS (SELECT 1 FROM `floor` WHERE `floor_number` = 2);

-- ------------------------------------------------------------
-- 示例数据：区域（每个楼层 A区/B区 两个区域）
-- ------------------------------------------------------------
INSERT INTO `area` (`floor_id`, `area_name`, `sort_order`, `status`, `remark`)
SELECT f.id, 'A区', 1, 1, '普通自习区'
FROM `floor` f
WHERE NOT EXISTS (SELECT 1 FROM `area` WHERE `floor_id` = f.id AND `area_name` = 'A区');

INSERT INTO `area` (`floor_id`, `area_name`, `sort_order`, `status`, `remark`)
SELECT f.id, 'B区', 2, 1, '安静自习区'
FROM `floor` f
WHERE NOT EXISTS (SELECT 1 FROM `area` WHERE `floor_id` = f.id AND `area_name` = 'B区');

-- ------------------------------------------------------------
-- 示例数据：座位（一楼 A01-A08，二楼 B01-B08；前4个属于A区，后4个属于B区）
-- ------------------------------------------------------------
INSERT INTO `seat` (`floor_id`, `area_id`, `seat_no`, `x_coord`, `y_coord`, `seat_type`, `status`)
SELECT f.id, a.id, CONCAT('A', LPAD(n.n, 2, '0')), (n.n - 1) * 60 + 30, 60, 1, 0
FROM (
  SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
  UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8
) n
JOIN `floor` f ON f.floor_number = 1
JOIN `area` a ON a.floor_id = f.id AND a.area_name = IF(n.n <= 4, 'A区', 'B区')
WHERE NOT EXISTS (
  SELECT 1 FROM `seat` WHERE `floor_id` = f.id AND `seat_no` = CONCAT('A', LPAD(n.n, 2, '0'))
);

INSERT INTO `seat` (`floor_id`, `area_id`, `seat_no`, `x_coord`, `y_coord`, `seat_type`, `status`)
SELECT f.id, a.id, CONCAT('B', LPAD(n.n, 2, '0')), (n.n - 1) * 60 + 30, 60, 1, 0
FROM (
  SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
  UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8
) n
JOIN `floor` f ON f.floor_number = 2
JOIN `area` a ON a.floor_id = f.id AND a.area_name = IF(n.n <= 4, 'A区', 'B区')
WHERE NOT EXISTS (
  SELECT 1 FROM `seat` WHERE `floor_id` = f.id AND `seat_no` = CONCAT('B', LPAD(n.n, 2, '0'))
);

-- ------------------------------------------------------------
-- 说明：如何创建管理员账号
-- 方式1（推荐，最安全）：启动系统后先注册一个学生账号，然后执行：
--   UPDATE sys_user SET role = 1, is_deleted = 0 WHERE username = '你的学号';
-- 方式2：管理员后台「用户管理」页面可创建管理员（初始密码为身份证号后6位）。
-- 请务必修改默认密码后再投入使用。
-- ------------------------------------------------------------
