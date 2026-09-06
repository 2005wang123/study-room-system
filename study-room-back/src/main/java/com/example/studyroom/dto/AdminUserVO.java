package com.example.studyroom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的用户信息（不含密码等敏感字段）
 */
@Data
@Builder
public class AdminUserVO {

    private Long id;

    private String username;

    /** 0-学生 1-管理员 */
    private Integer role;

    /** 0-禁用 1-启用 */
    private Integer status;

    private Boolean isFirstLogin;

    /** 信用积分（初始500） */
    private Integer points;

    /** 积分扣至0后的禁约截止时间 */
    private LocalDateTime bookBanUntil;

    private LocalDateTime createTime;
}
