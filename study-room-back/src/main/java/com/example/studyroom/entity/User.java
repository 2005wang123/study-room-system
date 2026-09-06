package com.example.studyroom.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    private Long id;
    private String username;
    private String password;
    private Integer role; // 0-学生 1-管理员
    private Integer status; // 0-禁用 1-启用

    // ====== 新增字段 ======
    private String idCard; // 身份证号
    private Integer isFirstLogin; // 0-已登录过 1-首次登录
    private LocalDateTime passwordUpdatedAt; // 密码最后更新时间

    /** 信用积分（初始500，违约一次扣100） */
    private Integer points;

    /** 积分扣至0后禁止预约的截止时间（24小时禁约，null表示未禁约） */
    private LocalDateTime bookBanUntil;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
