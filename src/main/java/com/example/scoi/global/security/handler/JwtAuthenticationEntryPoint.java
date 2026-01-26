package com.example.scoi.global.security.handler;

import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    
    private static final String ERROR_CODE_ATTRIBUTE = "authErrorCode";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        AuthErrorCode errorCode = (AuthErrorCode) request.getAttribute(ERROR_CODE_ATTRIBUTE);

        if (errorCode == null) {
            errorCode = AuthErrorCode.UNAUTHORIZED;
            log.warn("인증 실패 (에러 코드 없음): {} - {}", request.getRequestURI(), authException.getMessage());
        } else {
            log.warn("인증 실패: {} - {} ({})", request.getRequestURI(), errorCode.getMessage(), errorCode.getCode());
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.onFailure(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
