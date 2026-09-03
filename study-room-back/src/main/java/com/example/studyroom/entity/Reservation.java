//Reservation.java
package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reservation")
public class Reservation extends BaseEntity {

    /** 关联用户ID */
    private Long userId;

    /** 关联座位ID */
    private Long seatId;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /**
     * 履约状态
     * 0: 待签到, 1: 使用中, 2: 已完成, 3: 已违约(未签到), 4: 已取消
     */
    private Integer status;

    /** ⭐ 补上逻辑删除/维修状态字段 */
    @TableLogic(value = "0", delval = "1") // 如果NULL有特殊业务含义，不建议用@TableLogic，手动管理更安全
    private Integer isDeleted;

    /*
    public void setReservationTime(LocalDateTime now) {

    }*/
}