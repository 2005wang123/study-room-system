-- ============================================================
-- 升级脚本：信用积分体系（对已存在的数据库执行一次即可）
-- 作用：sys_user 表新增积分与禁约截止时间字段
--   1) 默认信用积分 500（已有用户会自动补为500）
--   2) 违约一次扣 100 积分（由后端逻辑执行）
--   3) 积分扣至0后触发24小时禁止预约（book_ban_until）
-- 执行：mysql -uroot -p study_room_db < upgrade_points.sql
-- ============================================================
ALTER TABLE `sys_user`
    ADD COLUMN `points`         INT      NOT NULL DEFAULT 500 COMMENT '信用积分(初始500，违约一次扣100)' AFTER `password_updated_at`,
    ADD COLUMN `book_ban_until` DATETIME DEFAULT NULL COMMENT '积分扣至0后的禁止预约截止时间(24小时)' AFTER `points`;
