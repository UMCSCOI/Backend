package com.example.scoi.domain.auth.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AuthReqDTO {

    // SMS 발송 요청
    public record SmsSendRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber
    ) {}

    // SMS 검증 요청
    public record SmsVerifyRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber,

            @NotBlank(message = "인증번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
            String verificationCode
    ) {}

    // 회원가입 요청
    public record SignupRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber,

            @NotBlank(message = "인증 토큰은 필수입니다.")
            String verificationToken,

            @NotBlank(message = "영문 이름은 필수입니다.")
            @Pattern(regexp = "^[A-Z ]+$", message = "영문 대문자와 공백만 입력 가능합니다.")
            @Size(max = 50, message = "영문 이름은 50자 이내입니다.")
            String englishName,

            @NotBlank(message = "한글 이름은 필수입니다.")
            @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다.")
            @Size(min = 2, max = 5, message = "한글 이름은 2~5자입니다.")
            String koreanName,

            @NotBlank(message = "주민등록번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}-\\d{7}$", message = "올바른 주민등록번호 형식이 아닙니다.")
            String residentNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Schema(description = "AES 암호화된 6자리 간편비밀번호 (Base64)", example = "ItfrsoB1J0hl3O60mahB1A==")
            String simplePassword,

            @Schema(description = "회원 타입 (미입력 시 INDIVIDUAL 기본값)",
                    example = "INDIVIDUAL",
                    allowableValues = {"INDIVIDUAL", "CORPORATION"})
            MemberType memberType,

            @Schema(description = "바이오 인증 등록 여부 (true: 등록, false: 나중에)", example = "false")
            Boolean isBioRegistered,

            @Schema(description = "거래소 API 키 목록 (선택사항)")
            List<ApiKeyRequest> apiKeys
    ) {}

    // API 키 요청 (회원가입용)
    public record ApiKeyRequest(
            @NotNull(message = "거래소 타입은 필수입니다.")
            @Schema(description = "거래소 타입", example = "UPBIT", allowableValues = {"BITHUMB", "UPBIT", "BINANCE"})
            ExchangeType exchangeType,

            @NotBlank(message = "퍼블릭 키는 필수입니다.")
            @Schema(description = "거래소 API 퍼블릭 키", example = "your-public-key")
            String publicKey,

            @NotBlank(message = "시크릿 키는 필수입니다.")
            @Schema(description = "거래소 API 시크릿 키 (AES 암호화된 Base64)", example = "asdadsasdasd...")
            String secretKey
    ) {}

    // 로그인 요청
    public record LoginRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Schema(description = "AES 암호화된 6자리 간편비밀번호 (Base64)", example = "ItfrsoB1J0hl3O60mahB1A==")
            String simplePassword,

            @Schema(description = "SMS 재인증 토큰 (계정 잠금/RT 만료 시 필수, 일반 로그인 시 생략)", nullable = true)
            String verificationToken
    ) {}

    // 토큰 재발급 요청
    public record ReissueRequest(
            @NotBlank(message = "Refresh Token은 필수입니다.")
            String refreshToken
    ) {}
}