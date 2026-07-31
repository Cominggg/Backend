package com.Coming.Backend.auth.jwt;

import com.Coming.Backend.auth.repository.BlacklistRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MDC_USER_ID_KEY = "userId";

    private final JwtProvider jwtProvider;
    private final BlacklistRepository blacklistRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = jwtProvider.parseClaims(token);
                if (!blacklistRepository.isBlacklisted(token)) {
                    setAuthentication(claims);
                }
            } catch (Exception e) {
                log.debug("JWT 파싱 실패 — 인증 없이 통과: {}", e.getMessage());
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_USER_ID_KEY);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void setAuthentication(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put(MDC_USER_ID_KEY, userId.toString());
    }
}
