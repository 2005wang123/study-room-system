package com.example.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyroom.common.LoginAttemptService;
import com.example.studyroom.common.exception.BusinessException;
import com.example.studyroom.dto.*;
import com.example.studyroom.common.constant.ReservationStatus;
import com.example.studyroom.entity.Reservation;
import com.example.studyroom.entity.User;
import com.example.studyroom.mapper.ReservationMapper;
import com.example.studyroom.mapper.UserMapper;
import com.example.studyroom.service.IUserService;
import com.example.studyroom.utils.IdCardUtil;
import com.example.studyroom.utils.JwtUtil;
import com.example.studyroom.utils.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordValidator passwordValidator;
    private final LoginAttemptService loginAttemptService;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 0. 防爆破：检查账号是否被临时锁定
        loginAttemptService.checkNotLocked(request.getUsername());

        // 1. 查询用户
        User user = findByUsername(request.getUsername());
        if (user == null) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new BusinessException("学号或密码错误");
        }

        // 2. 检查账户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new BusinessException("学号或密码错误");
        }

        // 4. 登录成功，清除失败记录
        loginAttemptService.loginSucceeded(request.getUsername());

        // 5. 检查是否首次登录
        boolean isFirstLogin = user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1;

        // 6. 检查密码是否过于简单
        boolean needChangePassword = passwordValidator.isWeakPassword(request.getPassword());

        // 7. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 8. 构建响应
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .role(user.getRole())
                .isFirstLogin(isFirstLogin)
                .needChangePassword(needChangePassword)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request, Long userId) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("当前密码错误");
        }

        // 3. 验证新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 4. 检查密码强度
        if (passwordValidator.isWeakPassword(request.getNewPassword())) {
            throw new BusinessException("密码过于简单，请使用包含大小写字母、数字和特殊字符的密码，长度不少于"
                    + passwordValidator.getMinLength() + "位");
        }

        // 5. 不允许与当前密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        // 6. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setIsFirstLogin(0);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isFirstLogin(user.getIsFirstLogin() == 1)
                .build();
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createUser(User user, boolean rebuildDeleted) {
        // 1. 验证用户名唯一
        User existUser = findByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException("学号已存在");
        }
        // 存在已被逻辑删除的同名用户：未确认重建时提示，确认后走重建
        User deletedUser = userMapper.selectDeletedByUsername(user.getUsername());
        if (deletedUser != null && !rebuildDeleted) {
            throw new BusinessException(409, "该用户名已被删除，是否重建该用户？");
        }

        // 2. 处理身份证号/初始密码：
        //    - 完整15/18位身份证号 -> 初始密码取后6位
        //    - 仅输入6位数字 -> 直接作为初始密码（不写入身份证号字段）
        //    - 未填写 -> 随机生成初始密码（返回给管理员转告用户）
        String initialPassword;
        if (StringUtils.hasText(user.getIdCard())) {
            user.setIdCard(user.getIdCard().trim());
            if (IdCardUtil.isValidIdCard(user.getIdCard())) {
                initialPassword = IdCardUtil.getDefaultPasswordFromIdCard(user.getIdCard());
            } else if (user.getIdCard().matches("^\\d{6}$")) {
                initialPassword = user.getIdCard();
                user.setIdCard(null); // 6位数字只是初始密码，不是身份证号
            } else {
                throw new BusinessException("身份证号格式不正确：请填写完整15/18位身份证号（初始密码为后6位），或直接填写6位数字作为初始密码");
            }
            user.setPassword(passwordEncoder.encode(initialPassword));
        } else {
            // 没有身份证号，使用随机初始密码
            initialPassword = generateTempPassword();
            user.setPassword(passwordEncoder.encode(initialPassword));
        }

        // 3. 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getIsFirstLogin() == null) {
            user.setIsFirstLogin(1);
        }

        // 4. 设置时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setPasswordUpdatedAt(now);
        user.setIsDeleted(0);

        // 5. 保存：存在被删除的同名用户则重建（恢复原记录并重置信息），否则新增
        if (deletedUser != null) {
            user.setId(deletedUser.getId());
            userMapper.rebuildUser(user);
        } else {
            userMapper.insert(user);
        }
        return initialPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 1. 用户名唯一性校验
        User existUser = findByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 密码强度校验
        if (passwordValidator.isWeakPassword(request.getPassword())) {
            throw new BusinessException("密码过于简单，请使用包含大小写字母、数字和特殊字符的密码，长度不少于"
                    + passwordValidator.getMinLength() + "位");
        }

        // 3. 构建用户（默认学生角色、启用状态，密码为用户自设）
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setIsFirstLogin(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setPasswordUpdatedAt(now);

        // 4. 保存
        userMapper.insert(user);
    }

    @Override
    public boolean isUserActive(Long userId) {
        User user = this.getById(userId);
        return user != null && user.getStatus() == 1;
    }

    @Override
    public Integer getUserRole(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getRole();
    }

    // ===== 管理员 =====

    @Override
    public IPage<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), User::getUsername, keyword)
                .eq(User::getIsDeleted, 0)
                .orderByDesc(User::getCreateTime);
        IPage<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        IPage<AdminUserVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(user -> AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .isFirstLogin(user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1)
                .createTime(user.getCreateTime())
                .build()).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setIsFirstLogin(1);
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return tempPassword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id, Long operatorId) {
        // 1. 不能删除当前登录的管理员自己
        if (operatorId != null && operatorId.equals(id)) {
            throw new BusinessException("不能删除当前登录的管理员账号");
        }

        // 2. 用户存在性校验
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 3. 存在进行中预约（待签到/使用中）时不允许删除，避免产生悬空预约
        Long activeCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, id)
                        .in(Reservation::getStatus, ReservationStatus.PENDING, ReservationStatus.IN_USE));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException("该用户存在进行中的预约，请先取消其预约后再删除");
        }

        // 4. 逻辑删除（MyBatis-Plus @TableLogic 自动改写为 UPDATE is_deleted=1）
        userMapper.deleteById(id);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}


