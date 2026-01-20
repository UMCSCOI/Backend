package com.example.scoi.global.security.filter;

import com.example.scoi.global.redis.RedisService;
import com.example.scoi.global.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰이 없으면 필터 통과 (인증 불필요 경로용)
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Redis 블랙리스트 확인
        if (redisService.isBlacklisted(token)) {
            request.setAttribute("authErrorCode", AuthErrorCode.BLACKLISTED_TOKEN);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 토큰 만료 확인
        if (jwtUtil.isTokenExpired(token)) {
            request.setAttribute("authErrorCode", AuthErrorCode.EXPIRED_TOKEN);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 토큰 유효성 검증
        if (!jwtUtil.validateToken(token)) {
            request.setAttribute("authErrorCode", AuthErrorCode.INVALID_TOKEN);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 유효한 토큰이면 SecurityContext에 인증 정보 저장
        String phoneNumber = jwtUtil.getPhoneNumberFromToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(phoneNumber, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
