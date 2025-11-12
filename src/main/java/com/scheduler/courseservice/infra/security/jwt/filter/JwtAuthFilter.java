package com.scheduler.courseservice.infra.security.jwt.filter;

import com.scheduler.courseservice.infra.security.jwt.component.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain

    ) throws ServletException, IOException {

        String accessToken = request.getHeader(AUTHORIZATION);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!accessToken.startsWith("Bearer ")) {
            log.info("start with Bearer");
            response.setStatus(SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT Token");
            return;
        }

        accessToken = accessToken.replace("Bearer ", "").trim();

        try {
            Authentication authentication = jwtUtils.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
            response.setStatus(SC_UNAUTHORIZED);
            response.getWriter().write("Invalid JWT Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
