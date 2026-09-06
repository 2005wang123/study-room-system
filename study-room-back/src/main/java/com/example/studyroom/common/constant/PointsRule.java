package com.example.studyroom.common.constant;

/**
 * 信用积分规则
 */
public final class PointsRule {

    /** 新用户默认信用积分 */
    public static final int DEFAULT_POINTS = 500;

    /** 违约一次扣除的积分 */
    public static final int VIOLATION_DEDUCTION = 100;

    /** 积分为0后的禁止预约时长（小时） */
    public static final int BAN_HOURS = 24;

    private PointsRule() {
    }
}
