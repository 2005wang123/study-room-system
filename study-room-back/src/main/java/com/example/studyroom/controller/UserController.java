package com.example.studyroom.controller;

import com.example.studyroom.common.LoginAttemptService;
import com.example.studyroom.common.Result;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.config.annotation.RequirePermission;
import com.example.studyroom.dto.ChangePasswordRequest;
import com.example.studyroom.dto.LoginRequest;
import com.example.studyroom.dto.LoginResponse;
import com.example.studyroom.dto.RegisterRequest;
import com.example.studyroom.dto.UserInfoResponse;
import com.example.studyroom.service.IUserService;
import com.example.studyroom.utils.JwtUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    /**
     * 用户登录
     */
    @PermitAll
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // IP 维度限流（防爆破）
        loginAttemptService.checkIpRate(httpRequest.getRemoteAddr());
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户注册
     */
    @PermitAll
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功");
    }

    /**
     * 刷新Token（滑动续期，需携带有效Token）
     */
    @RequirePermission
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserInfoResponse userInfo = userService.getUserInfo(userId);
        if (userInfo.getStatus() != null && userInfo.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String username = (String) request.getAttribute("username");
        if (username == null || username.isBlank()) {
            username = userInfo.getUsername();
        }
        String newToken = jwtUtil.generateToken(userId, username);
        return Result.success(LoginResponse.builder()
                .id(userId)
                .username(userInfo.getUsername())
                .token(newToken)
                .role(userInfo.getRole())
                .isFirstLogin(userInfo.getIsFirstLogin())
                .build());
    }

    /**
     * 修改密码
     */
    @RequirePermission
    @PostMapping("/change-password")
    public Result<?> changePassword(
            @Validated @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getCurrentUserId(httpRequest);
        userService.changePassword(request, userId);
        return Result.success("密码修改成功");
    }

    /**
     * 获取用户信息
     */
    @RequirePermission
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.success(userService.getUserInfo(userId));
    }

    /**
     * 用户登出（JWT 无状态，前端清除 Token 即可）
     */
    @PermitAll
    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success("登出成功");
    }

    /**
     * 从请求头获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new AuthenticationCredentialsNotFoundException("未登录或Token已过期");
        }
        return userId;
    }
}