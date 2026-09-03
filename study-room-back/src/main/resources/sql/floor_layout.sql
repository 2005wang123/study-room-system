-- ============================================================
-- 楼层结构图（floor_layout）建表脚本
-- 说明：幂等，可重复执行。
--   source floor_layout.sql;
-- ============================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `floor_layout` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `floor_id`    BIGINT       NOT NULL COMMENT '所属楼层ID',
  `layout_json` LONGTEXT     NOT NULL COMMENT '楼层结构图JSON(区域/墙体/座位/文字等)',
  `version`     INT          NOT NULL DEFAULT 1 COMMENT '版本号(乐观锁)',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布',
  `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `update_by`   BIGINT       DEFAULT NULL COMMENT '最后更新人ID',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_floor` (`floor_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层结构图';
