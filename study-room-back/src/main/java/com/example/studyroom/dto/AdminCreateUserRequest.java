package com.example.studyroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员新增用户请求
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 身份证号（可选，用于生成初始密码=后6位） */
    private String idCard;

    /** 角色：0-学生 1-管理员，默认0 */
    private Integer role;

    /** 状态：0-禁用 1-启用，默认1 */
    private Integer status;

    /** 是否重建已被删除的同名用户，默认false */
    private Boolean rebuild;
}
