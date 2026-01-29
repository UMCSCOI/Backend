package com.example.scoi.global.security.handler;

import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security AccessDeniedHandler 구현체
 * 인증은 되었으나 권한이 없는 경우(403) 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("접근 거부: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

        AuthErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.onFailure(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
