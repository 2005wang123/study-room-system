package com.example.studyroom.common.constant;

/**
 * 座位状态常量（统一前后端及各层对座位状态的定义）
 * 0-空闲 1-已预约 2-使用中 3-维修中
 */
public final class SeatStatus {

    /** 空闲 */
    public static final int FREE = 0;

    /** 已预约（尚未签到） */
    public static final int BOOKED = 1;

    /** 使用中（已签到） */
    public static final int IN_USE = 2;

    /** 维修中 */
    public static final int MAINTENANCE = 3;

    private SeatStatus() {
    }
}