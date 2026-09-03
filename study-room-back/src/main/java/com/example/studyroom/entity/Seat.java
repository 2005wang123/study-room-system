package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.studyroom.dto.SeatReservationVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seat")
public class Seat extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联楼层ID */
    private Long floorId;

    /** 关联区域(房间)ID */
    private Long areaId;

    /** 座位编号 (例如: "A-01") */
    @TableField("seat_no")
    private String seatNo;

    /** X轴坐标 (用于前端绝对定位) */
    @TableField("x_coord")
    private Integer xCoord;

    /** Y轴坐标 (用于前端绝对定位) */
    @TableField("y_coord")
    private Integer yCoord;

    /**
     * 座位类型
     * 1: 普通座, 2: 靠窗座, 3: 插座座
     */
    private Integer seatType;

    /**
     * 状态（与 SeatStatus 常量保持一致）
     * 0: 空闲, 1: 已预约, 2: 使用中, 3: 维修中
     */
    private Integer status;

    /** 用于接收关联查询出的当前占用者用户ID（是否本人预约） */
    @TableField(exist = false)
    private Long userId;

    /** 用于接收关联查询出的当前占用者用户名 */
    @TableField(exist = false)
    private String userName;

    /** 区域名称（关联查询） */
    @TableField(exist = false)
    private String areaName;

    /** 楼层名称（关联查询） */
    @TableField(exist = false)
    private String floorName;

    /** 指定日期内的进行中预约时段列表 */
    @TableField(exist = false)
    private List<SeatReservationVO> reservations;

    /** 指定日期内已预约时长占开放时段(08:00-21:30)的比例，0~1 */
    @TableField(exist = false)
    private Double bookedRatio;

    /** 当前用户在该座位指定日期内的进行中预约ID（用于取消） */
    @TableField(exist = false)
    private Long myReservationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
