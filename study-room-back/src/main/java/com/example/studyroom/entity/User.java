package com.example.studyroom.entity;

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

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}