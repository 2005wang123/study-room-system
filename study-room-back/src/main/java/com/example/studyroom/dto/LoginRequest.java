package com.example.studyroom.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;


@Data
public class LoginRequest {

    @NotBlank(message = "学号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}