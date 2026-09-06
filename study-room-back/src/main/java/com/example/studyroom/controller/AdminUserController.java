package com.example.studyroom.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.studyroom.common.Result;
import com.example.studyroom.config.annotation.RequireRole;
import com.example.studyroom.dto.AdminCreateUserRequest;
import com.example.studyroom.dto.AdminUserVO;
import com.example.studyroom.entity.User;
import com.example.studyroom.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理（仅管理员）
 */
@RestController
@RequestMapping("/api/user/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserService userService;

    /**
     * 用户分页列表（支持用户名模糊搜索）
     */
    @GetMapping("/users")
    @RequireRole(role = 1)
    public Result<IPage<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    /**
     * 新增用户（初始密码为身份证号后6位，未填身份证则使用随机密码）
     */
    @PostMapping("/users")
    @RequireRole(role = 1)
    public Result<String> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setIdCard(request.getIdCard());
        user.setRole(request.getRole() == null ? 0 : request.getRole());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        String initialPassword = userService.createUser(user, Boolean.TRUE.equals(request.getRebuild()));
        return Result.success("用户创建成功，初始密码：" + initialPassword + "（首次登录后请尽快修改）", initialPassword);
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/users/{id}/status")
    @RequireRole(role = 1)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success(status != null && status == 1 ? "用户已启用" : "用户已禁用");
    }

    /**
     * 重置密码，返回临时密码（仅返回一次，请管理员转告用户）
     */
    @PutMapping("/users/{id}/reset-password")
    @RequireRole(role = 1)
    public Result<String> resetPassword(@PathVariable Long id) {
        String tempPassword = userService.resetPassword(id);
        return Result.success("密码已重置", tempPassword);
    }

    /**
     * 调整用户信用积分（delta>0加分，delta<0减分；扣至0后触发24小时禁约）
     */
    @PutMapping("/users/{id}/points")
    @RequireRole(role = 1)
    public Result<Integer> adjustPoints(@PathVariable Long id, @RequestParam Integer delta) {
        int newPoints = userService.adjustPoints(id, delta);
        String action = delta > 0 ? "加分" : "扣分";
        return Result.success(action + "成功，当前积分：" + newPoints, newPoints);
    }

    /**
     * 删除用户（逻辑删除；存在进行中预约时不允许删除）
     */
    @DeleteMapping("/users/{id}")
    @RequireRole(role = 1)
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        userService.deleteUser(id, operatorId);
        return Result.success("用户已删除");
    }
}

