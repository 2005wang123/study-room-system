package com.example.studyroom.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyroom.dto.*;
import com.example.studyroom.entity.User;

public interface IUserService extends IService<User> {

    // 登录
    LoginResponse login(LoginRequest request);

    // 修改密码
    void changePassword(ChangePasswordRequest request, Long userId);

    // 获取用户信息
    UserInfoResponse getUserInfo(Long userId);

    // 通过用户名查询用户
    User findByUsername(String username);

    /**
     * 创建用户（管理员建号），返回初始明文密码
     *
     * @param rebuildDeleted 当存在已被逻辑删除的同名用户时，是否重建该用户
     */
    String createUser(User user, boolean rebuildDeleted);

    // 自助注册
    void register(RegisterRequest request);

    /**
     * 检查指定用户账户是否处于激活状态
     */
    boolean isUserActive(Long userId);

    Integer getUserRole(Long userId);

    /** 检查用户当前是否允许预约（积分为0且在24小时禁约期内时禁止），不允许时抛出业务异常 */
    void checkBookingAllowed(Long userId);

    /** 记录一次违约：扣除信用积分（默认每次100），扣至0时触发24小时禁约 */
    void applyViolationPenalty(Long userId);

    /** 管理员调整用户信用积分（delta>0加分，delta<0减分），扣至0时同样触发24小时禁约，返回调整后积分 */
    int adjustPoints(Long userId, int delta);

    // ===== 管理员 =====

    /** 分页查询用户列表（管理员） */
    IPage<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword);

    /** 启用/禁用用户（管理员） */
    void updateUserStatus(Long id, Integer status);

    /** 重置密码（管理员），返回临时密码 */
    String resetPassword(Long id);

    /** 删除用户（逻辑删除，管理员） */
    void deleteUser(Long id, Long operatorId);
}
