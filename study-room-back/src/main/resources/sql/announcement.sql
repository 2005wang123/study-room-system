-- ============================================================
-- 公告表 (announcement)
-- 说明：在 study_room_db 数据库中执行本脚本
-- 已发布公告(status=1) 会展示在前端「公告」页面
-- 本脚本可重复执行（建表与示例数据均有幂等保护）
-- ============================================================

-- 保证客户端以 utf8mb4 连接，避免中文默认值报错
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `announcement` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`        VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content`      TEXT         NOT NULL COMMENT '公告内容',
  `publisher`    VARCHAR(64)  DEFAULT '管理员' COMMENT '发布人',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-草稿/下架 1-已发布',
  `publish_time` DATETIME     DEFAULT NULL COMMENT '发布时间',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================================
-- 示例数据（仅当标题不存在时插入，避免重复执行产生重复数据）
-- ============================================================
INSERT INTO `announcement` (`title`, `content`, `publisher`, `status`, `publish_time`)
SELECT '自习室开放时间调整通知',
'各位同学：

为了给大家提供更充足的学习时间，自本周起自习室开放时间调整为每日 08:00 - 22:00，请合理安排预约时间。

温馨提示：预约成功后请按时到馆签到，逾期未签到将记为违约。',
'管理员', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `announcement` WHERE `title` = '自习室开放时间调整通知' AND `is_deleted` = 0);

INSERT INTO `announcement` (`title`, `content`, `publisher`, `status`, `publish_time`)
SELECT '关于规范使用自习室座位的通知',
'近期发现部分同学存在长时间占座不使用的情况。为保障座位资源公平使用，特此重申以下规则：

1. 单次预约时长不超过4小时；
2. 预约后请按时到馆签到；
3. 如无法到馆，请提前在「预约记录」中取消预约；
4. 连续多次违约将限制预约功能。

感谢大家的理解与配合。',
'管理员', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `announcement` WHERE `title` = '关于规范使用自习室座位的通知' AND `is_deleted` = 0);

INSERT INTO `announcement` (`title`, `content`, `publisher`, `status`, `publish_time`)
SELECT '欢迎使用自习室预约系统',
'欢迎各位同学使用自习室座位预约系统！

本系统支持在线选座、预约时段、查看预约记录等功能。首次登录请使用学号和初始密码（身份证号后6位），登录后请及时修改密码。

使用过程中如有问题，可查看「帮助中心」或联系管理员。',
'管理员', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `announcement` WHERE `title` = '欢迎使用自习室预约系统' AND `is_deleted` = 0);