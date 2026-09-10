package com.todo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public static final List<String> PUBLIC_PATHS =
            List.of("/api/register", "/api/login");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        try {
            Long userId = jwtUtil.parseUserId(header.substring(7));
            CurrentUser.set(userId);
            chain.doFilter(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } finally {
            CurrentUser.clear();
        }
    }
}
