// ReservationVO.java
package com.example.studyroom.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预约记录视图对象：在预约记录基础上补充座位编号、楼层、区域等展示信息
 */
@Data
public class ReservationVO {

    /** 预约记录ID */
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 关联座位ID */
    private Long seatId;

    /** 座位编号 (例如 "A-01") */
    private String seatNo;

    /** 楼层ID */
    private Long floorId;

    /** 楼层名称 (例如 "二楼") */
    private String floorName;

    /** 区域ID */
    private Long areaId;

    /** 区域名称 (例如 "A区") */
    private String areaName;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /**
     * 履约状态
     * 0: 待签到, 1: 使用中, 2: 已完成, 3: 已违约(未签到), 4: 已取消
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
