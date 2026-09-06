package com.example.studyroom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoResponse {
    private Long id;
    private String username;
    private Integer role;
    private Integer status;
    private Boolean isFirstLogin;
    private Integer points;
    private LocalDateTime bookBanUntil;
}
