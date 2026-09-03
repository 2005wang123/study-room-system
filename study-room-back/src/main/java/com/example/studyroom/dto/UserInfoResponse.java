package com.example.studyroom.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoResponse {
    private Long id;
    private String username;
    private Integer role;
    private Integer status;
    private Boolean isFirstLogin;
}