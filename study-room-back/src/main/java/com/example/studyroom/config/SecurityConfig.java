package com.example.studyroom.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：
 * 采用 deny-by-default（默认所有请求都需要认证），
 * JwtAuthenticationFilter 负责解析 Bearer Token 并写入 SecurityContext，
 * 角色/权限的二次校验由 AuthInterceptor（@RequireRole / @RequirePermission）负责。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + JWT，关闭 CSRF
                .csrf(csrf -> csrf.disable())

                // 无状态会话
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 请求授权规则：默认全部需要认证，公开接口显式放行
                .authorizeHttpRequests(auth -> auth
                        // CORS 预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 接口文档（Knife4j/Swagger）与静态资源
                        .requestMatchers("/doc.html", "/webjars/**", "/swagger-ui/**",
                                "/v3/api-docs/**", "/swagger-resources/**",
                                "/favicon.ico", "/", "/index.html", "/static/**", "/assets/**",
                                "/error").permitAll()
                        // 公开接口：登录、注册、登出
                        .requestMatchers("/api/user/login", "/api/user/register", "/api/user/logout").permitAll()
                        // 公开接口：座位/楼层/公告查询
                        .requestMatchers(HttpMethod.GET,
                                "/api/study-room/seats",
                                "/api/study-room/seats/**",
                                "/api/study-room/floors",
                                "/api/study-room/floors/**",
                                "/api/study-room/floor-layout",
                                "/api/study-room/floor-layout/**",
                                "/api/announcements",
                                "/api/announcements/**").permitAll()
                        // 其余所有请求都需要登录（deny by default）
                        .anyRequest().authenticated())

                // 未认证返回 401 JSON（与前端 axios 拦截器约定一致）
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
                }))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 禁用默认的表单登录和 HTTP Basic 认证
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
