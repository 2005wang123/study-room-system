//AuthInterceptor.java
package com.example.studyroom.config;

import com.example.studyroom.config.annotation.RequirePermission;
import com.example.studyroom.config.annotation.RequireRole;
import com.example.studyroom.service.IUserService;
import com.example.studyroom.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是Controller方法，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查是否需要登录权限
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        RequireRole requireRole = method.getAnnotation(RequireRole.class);

        // 如果不需要任何权限，直接放行
        if (requirePermission == null && requireRole == null) {
            return true;
        }

        // 获取Token
        String token = getTokenFromRequest(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }

        // 解析Token获取用户信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);

        // 将用户信息存入请求属性，供后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        // 查询并缓存用户角色，供控制器判断权限使用
        Integer userRole = userService.getUserRole(userId);
        request.setAttribute("userRole", userRole);

        // 检查是否需要特定角色
        if (requireRole != null) {
            int requiredRole = requireRole.role();
            if (userRole == null || userRole != requiredRole) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足，需要管理员权限\"}");
                return false;
            }
        }

        return true;
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }
}
