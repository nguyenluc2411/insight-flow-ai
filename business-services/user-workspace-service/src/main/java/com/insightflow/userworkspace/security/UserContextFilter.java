package com.insightflow.userworkspace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    private static final String MOCK_USER_ID = "test-user-123";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Cố gắng lấy ID từ Header (Chuẩn bị sẵn cho module Auth sau này)
        String userId = request.getHeader("X-User-Id");

        // 2. Fallback: Nếu không có Header, tự động dùng Mock User thay vì ném lỗi 401
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("⚠️ [DEV MODE] Không tìm thấy Header 'X-User-Id'. Đang sử dụng Mock User ID: {}", MOCK_USER_ID);
            userId = MOCK_USER_ID;
        }

        try {
            UserContext.setCurrentUserId(userId);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}