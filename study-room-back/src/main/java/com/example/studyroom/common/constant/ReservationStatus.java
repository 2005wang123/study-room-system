package com.example.studyroom.common.constant;

/**
 * 预约状态常量
 * 0-待签到 1-使用中 2-已完成 3-违约(未签到) 4-已取消
 */
public final class ReservationStatus {

    /** 待签到 */
    public static final int PENDING = 0;

    /** 使用中（已签到） */
    public static final int IN_USE = 1;

    /** 已完成（已签退/自然结束） */
    public static final int COMPLETED = 2;

    /** 违约（到期未签到） */
    public static final int VIOLATED = 3;

    /** 已取消 */
    public static final int CANCELLED = 4;

    private ReservationStatus() {
    }
}