package com.example.studyroom.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 密码强度校验（规则由 application.yml 的 password.* 配置驱动）
 */
@Component
public class PasswordValidator {

    private final int minLength;
    private final boolean requireSpecialChar;
    private final boolean requireUpperLower;

    public PasswordValidator(
            @Value("${password.min-length:8}") int minLength,
            @Value("${password.require-special-char:true}") boolean requireSpecialChar,
            @Value("${password.require-upper-lower:true}") boolean requireUpperLower) {
        this.minLength = minLength;
        this.requireSpecialChar = requireSpecialChar;
        this.requireUpperLower = requireUpperLower;
    }

    public int getMinLength() {
        return minLength;
    }

    /**
     * 密码是否过于简单（弱）
     */
    public boolean isWeakPassword(String password) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        if (password.length() < minLength) {
            return true;
        }
        if (requireSpecialChar && !password.matches(".*[^a-zA-Z0-9].*")) {
            return true;
        }
        if (requireUpperLower && !(password.matches(".*[a-z].*") && password.matches(".*[A-Z].*"))) {
            return true;
        }
        return false;
    }

    /**
     * 密码强度评分
     *
     * @return 0-6分
     */
    public int checkStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }
        if (password.matches(".*[a-z].*")) {
            score++;
        }
        if (password.matches(".*[A-Z].*")) {
            score++;
        }
        if (password.matches(".*\\d.*")) {
            score++;
        }
        if (password.matches(".*[^a-zA-Z0-9].*")) {
            score++;
        }
        return score;
    }

    /**
     * 密码强度描述
     */
    public String getStrengthText(String password) {
        int score = checkStrength(password);
        if (score <= 2) {
            return "弱";
        } else if (score <= 3) {
            return "中";
        } else {
            return "强";
        }
    }

    /**
     * 获取密码强度建议
     */
    public String getSuggestion(String password) {
        if (isWeakPassword(password)) {
            return "密码过于简单，建议包含大小写字母、数字和特殊字符，长度不少于" + minLength + "位";
        }
        return "密码强度良好";
    }
}