package com.example.scoi.global.security.filter;

import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.security.jwt.JwtUtil;
import com.example.scoi.global.security.userdetails.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // 인증 없이 접근 가능한 경로 (SecurityConfig와 동일하게 유지)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/sms/",
            "/auth/signup",
            "/auth/login",
            "/auth/reissue",
            "/auth/password/reset",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/swagger-resources/",
            "/error"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // PUBLIC_ENDPOINTS는 토큰 검증 건너뛰기
        if (isPublicEndpoint(request)) {
            log.debug("PUBLIC_ENDPOINT 접근: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        // 토큰이 없으면 필터 통과 (인증 불필요 경로용)
        if (!StringUtils.hasText(token)) {
            log.debug("JWT 토큰이 없음: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 검증 (validateToken이 만료도 함께 체크하므로 먼저 호출)
        if (!jwtUtil.validateAccessToken(token)) {
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
     * PUBLIC_ENDPOINTS 여부 확인
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (requestURI.equals(endpoint) || requestURI.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
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
     * DB에서 회원 정보를 조회하여 UserDetails로 래핑합니다.
     */
    private void authenticateUser(HttpServletRequest request, String token) {
        String phoneNumber = jwtUtil.getPhoneNumberFromToken(token);

        // DB에서 회원 조회 후 UserDetails 생성
        UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 인증 실패 처리
     */
    private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response,
                                          AuthErrorCode errorCode) throws IOException {
        log.warn("JWT 인증 실패: {} - {} ({})", request.getRequestURI(), errorCode.getMessage(), errorCode.getCode());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.onFailure(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
