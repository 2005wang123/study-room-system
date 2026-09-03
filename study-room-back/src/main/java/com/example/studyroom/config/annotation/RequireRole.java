package com.example.studyroom.config.annotation;

import java.lang.annotation.*;

/**
 * 需要特定角色的注解
 * 标注在Controller方法上，表示需要特定角色才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    int role();  // 0-学生 1-管理员
}