package com.example.scoi.domain.auth.dto;

public class AuthResDTO {

    // SMS 검증 응답
    public record SmsVerifyResponse(
            // result: null
    ) {}

    // 로그인 응답
    public record LoginResponse(
            String accessToken,
            String refreshToken
    ) {}

    // 토큰 재발급 응답
    public record ReissueResponse(
            String accessToken,
            String refreshToken
    ) {}
}