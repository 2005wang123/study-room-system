//提取身份证后六位
package com.example.studyroom.utils;

import org.springframework.util.StringUtils;

public class IdCardUtil {

    /**
     * 从身份证号提取后6位作为初始密码
     */
    public static String getDefaultPasswordFromIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 6) {
            return "123456"; // 默认密码
        }
        return idCard.substring(idCard.length() - 6);
    }

    /**
     * 简单验证身份证号格式（15位或18位）
     */
    public static boolean isValidIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return false;
        }
        // 15位或18位，最后一位可能是X
        return idCard.matches("^\\d{15}$|^\\d{17}[0-9Xx]$");
    }
}