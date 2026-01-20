package com.example.scoi.domain.auth.dto;

import java.time.LocalDateTime;

public class AuthResDTO {

    // SMS 발송 응답
    public record SmsSendResponse(
            LocalDateTime expiredAt  // 인증번호 만료 시간 (발송 시간 + 3분)
    ) {}

    // SMS 검증 응답
    public record SmsVerifyResponse(
            String verificationToken  // 인증 성공 시 발급되는 일회용 토큰 (유효시간: 10분)
    ) {}

    // 회원가입 응답
    public record SignupResponse(
            Long memberId,
            String koreanName
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