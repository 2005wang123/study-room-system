package com.example.studyroom.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置：来源白名单由 cors.allowed-origins 配置（逗号分隔），
 * 生产环境请改为正式前端域名。
 * 过滤器注册优先级高于 Spring Security 过滤链，确保预检请求和 401/403 响应
 * 都能正确携带 CORS 响应头。
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5137,http://localhost:5138}")
    private String allowedOrigins;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        // 兜底：Vite dev server 端口被占用时会自动顺延（5137 -> 5138 -> ...），
        // 这里放行本机 localhost/127.0.0.1 的任意端口，避免开发环境因端口漂移被 403。
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*", "https://localhost:*",
                "http://127.0.0.1:*", "https://127.0.0.1:*"));
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        CorsFilter filter = new CorsFilter(source);
        // 来源不在白名单时，返回可读的 JSON 而不是裸文本 "Invalid CORS request"，便于定位问题
        filter.setCorsProcessor(new JsonRejectCorsProcessor(origins));

        log.info("CORS 允许来源白名单: {}", origins);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 跨域被拒绝时输出结构化 JSON，明确提示是来源不在白名单，而非认证/密码问题。
     */
    private static class JsonRejectCorsProcessor extends DefaultCorsProcessor {

        private final List<String> allowedOrigins;

        JsonRejectCorsProcessor(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        @Override
        protected void rejectRequest(ServerHttpResponse response) {
            if (response instanceof ServletServerHttpResponse servletResponse) {
                var res = servletResponse.getServletResponse();
                if (!res.isCommitted()) {
                    try {
                        res.setStatus(403);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"code\":403,\"message\":\"CORS 拒绝：请求来源不在白名单，"
                                + "请检查后端 cors.allowed-origins 配置。当前允许: "
                                + String.join(", ", allowedOrigins) + "\"}");
                        res.flushBuffer();
                        return;
                    } catch (Exception ignored) {
                        // 写入失败则回退到默认处理
                    }
                }
            }
            try {
                super.rejectRequest(response);
            } catch (Exception ignored) {
                // no-op
            }
        }
    }
}