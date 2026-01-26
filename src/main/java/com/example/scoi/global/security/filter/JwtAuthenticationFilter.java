package com.example.scoi.global.security.filter;

import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ERROR_CODE_ATTRIBUTE = "authErrorCode";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String token = resolveToken(request);

        // 토큰이 없으면 필터 통과 (인증 불필요 경로용)
        if (!StringUtils.hasText(token)) {
            log.debug("JWT 토큰이 없음: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 검증 (validateToken이 만료도 함께 체크하므로 먼저 호출)
        if (!jwtUtil.validateToken(token)) {
            log.warn("유효하지 않은 JWT 토큰: {}", requestURI);
            handleAuthenticationError(request, response, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        // Redis 블랙리스트 확인
        if (redisUtil.exists(BLACKLIST_PREFIX + token)) {
            log.warn("블랙리스트 토큰 접근 시도: {}", requestURI);
            handleAuthenticationError(request, response, AuthErrorCode.BLACKLISTED_TOKEN);
            return;
        }

        // 유효한 토큰이면 SecurityContext에 인증 정보 저장
        authenticateUser(request, token);
        log.debug("JWT 인증 성공: {}", requestURI);

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 인증 정보를 SecurityContext에 저장
     */
    private void authenticateUser(HttpServletRequest request, String token) {
        String phoneNumber = jwtUtil.getPhoneNumberFromToken(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(phoneNumber, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 인증 실패 처리 (중복 제거)
     */
    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response,
                                          AuthErrorCode errorCode) throws IOException {
        request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
