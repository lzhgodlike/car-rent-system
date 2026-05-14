package com.sdjzu.carrental.security;

import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.UserContext;
import com.sdjzu.carrental.model.dto.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicRequest(request)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("UNAUTHORIZED");
        }

        String token = authorization.substring(7);
        Claims claims = jwtUtil.parseToken(token);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(Long.valueOf(claims.get("userId").toString()));
        loginUser.setUsername(claims.get("username").toString());
        loginUser.setRole(claims.get("role").toString());
        UserContext.set(loginUser);
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getServletPath();
        return "/api/cars".equals(path)
                || "/api/cars/brands".equals(path)
                || "/api/cars/cities".equals(path)
                || "/api/car-types".equals(path)
                || path.matches("/api/cars/\\d+");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
