package com.example.studyroom.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位在某日期的预约时段视图对象
 */
@Data
public class SeatReservationVO {

    /** 预约记录ID */
    private Long id;

    /** 预约用户ID */
    private Long userId;

    /** 预约用户姓名/学号 */
    private String userName;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /** 履约状态: 0-待签到 1-使用中 2-已完成 3-违约 4-已取消 */
    private Integer status;
}
