package com.example.studyroom.common;

import com.example.studyroom.common.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录防爆破：基于 Caffeine 内存缓存实现
 * 1) 用户名维度：连续失败 N 次后临时锁定
 * 2) IP 维度：每分钟请求次数限制
 */
@Component
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_IP_REQUESTS_PER_MINUTE = 20;

    /** 用户名 -> 连续失败次数（15 分钟无新失败后自动清除） */
    private final Cache<String, Integer> failedAttempts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(10000)
            .build();

    /** IP -> 最近 1 分钟内的登录请求次数 */
    private final Cache<String, Integer> ipRequestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10000)
            .build();

    /** 校验账号是否被临时锁定 */
    public void checkNotLocked(String username) {
        Integer failed = failedAttempts.getIfPresent(username);
        if (failed != null && failed >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessException("失败次数过多，账号已被临时锁定，请15分钟后再试");
        }
    }

    /** 记录一次登录失败 */
    public void loginFailed(String username) {
        failedAttempts.asMap().merge(username, 1, Integer::sum);
    }

    /** 登录成功后清除失败记录 */
    public void loginSucceeded(String username) {
        failedAttempts.invalidate(username);
    }

    /** 记录一次 IP 请求并检查是否超过限流阈值 */
    public void checkIpRate(String ip) {
        int count = ipRequestCounts.asMap().merge(ip, 1, Integer::sum);
        if (count > MAX_IP_REQUESTS_PER_MINUTE) {
            throw new BusinessException("请求过于频繁，请稍后再试");
        }
    }
}