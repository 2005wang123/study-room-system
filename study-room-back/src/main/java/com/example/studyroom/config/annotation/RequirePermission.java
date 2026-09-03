package com.example.studyroom.config.annotation;

import java.lang.annotation.*;

/**
 * 需要登录权限的注解
 * 标注在Controller方法上，表示需要登录后才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    // 可以扩展，如是否需要特定权限
    String value() default "";
}