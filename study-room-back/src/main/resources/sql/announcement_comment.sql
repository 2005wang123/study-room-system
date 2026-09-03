-- ============================================================
-- 公告评论与点赞表 (announcement_comment / announcement_comment_like)
-- 说明：在 study_room_db 数据库中执行本脚本（可重复执行，建表均有幂等保护）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 公告评论表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `announcement_comment` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `announcement_id` BIGINT        NOT NULL COMMENT '所属公告ID',
  `user_id`         BIGINT        NOT NULL COMMENT '评论用户ID',
  `content`         VARCHAR(500)  NOT NULL COMMENT '评论内容',
  `like_count`      INT           NOT NULL DEFAULT 0 COMMENT '点赞数',
  `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_announcement` (`announcement_id`, `is_deleted`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告评论表';

-- ------------------------------------------------------------
-- 公告评论点赞表（物理删除：取消点赞即删除记录，保证唯一约束可重复点赞）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `announcement_comment_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id`  BIGINT   NOT NULL COMMENT '评论ID',
  `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告评论点赞表';
