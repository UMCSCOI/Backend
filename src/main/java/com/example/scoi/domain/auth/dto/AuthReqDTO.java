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
            @Schema(description = "휴대폰 번호", example = "01012345678")
            String phoneNumber
    ) {}

    // SMS 검증 요청
    public record SmsVerifyRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            @Schema(description = "휴대폰 번호", example = "01012345678")
            String phoneNumber,

            @NotBlank(message = "인증번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
            @Schema(description = "SMS 인증번호 6자리", example = "123456")
            String verificationCode
    ) {}

    // 회원가입 요청
    public record SignupRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            @Schema(description = "휴대폰 번호", example = "01012345678")
            String phoneNumber,

            @NotBlank(message = "인증 토큰은 필수입니다.")
            @Schema(description = "SMS 인증 완료 후 발급된 verificationToken")
            String verificationToken,

            @NotBlank(message = "영문 이름은 필수입니다.")
            @Pattern(regexp = "^[A-Z ]+$", message = "영문 대문자와 공백만 입력 가능합니다.")
            @Size(max = 50, message = "영문 이름은 50자 이내입니다.")
            @Schema(description = "영문 이름 (대문자)", example = "JANG MYONGJUN")
            String englishName,

            @NotBlank(message = "한글 이름은 필수입니다.")
            @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다.")
            @Size(min = 2, max = 5, message = "한글 이름은 2~5자입니다.")
            @Schema(description = "한글 이름", example = "장명준")
            String koreanName,

            @NotBlank(message = "주민등록번호는 필수입니다.")
            @Pattern(regexp = "^\\d{7}$", message = "올바른 주민등록번호 형식이 아닙니다.")
            @Schema(description = "주민등록번호 앞 7자리 (생년월일 6자리 + 성별코드 1자리)", example = "0306203")
            String residentNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Schema(description = "AES 암호화된 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
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
            @Schema(description = "거래소 API 퍼블릭 키", example = "abcdef1234567890abcdef12")
            String publicKey,

            @NotBlank(message = "시크릿 키는 필수입니다.")
            @Schema(description = "거래소 API 시크릿 키 (AES 암호화된 Base64)", example = "abcdef1234567890abcdef1234567890")
            String secretKey
    ) {}

    // 로그인 요청
    public record LoginRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            @Schema(description = "휴대폰 번호", example = "01012345678")
            String phoneNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Schema(description = "AES 암호화된 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
            String simplePassword,

            @Schema(description = "SMS 재인증 토큰 (계정 잠금/RT 만료 시 필수, 일반 로그인 시 생략)", nullable = true)
            String verificationToken
    ) {}

    // 비인증 간편비밀번호 재설정 요청 (계정 잠금 후 SMS 재인증 flow)
    public record PasswordResetRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            @Schema(description = "휴대폰 번호", example = "01012345678")
            String phoneNumber,

            @NotBlank(message = "인증 토큰은 필수입니다.")
            @Schema(description = "SMS 인증 완료 후 발급된 verificationToken")
            String verificationToken,

            @NotBlank(message = "새 간편비밀번호는 필수입니다.")
            @Schema(description = "AES 암호화된 새 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
            String newPassword
    ) {}

    // 토큰 재발급 요청
    public record ReissueRequest(
            @NotBlank(message = "Refresh Token은 필수입니다.")
            String refreshToken
    ) {}

    // 간편 비밀번호 재설정
    public record ResetPassword(
            @NotNull(message = "SMS 인증 토큰은 필수입니다.")
            @NotBlank(message = "SMS 인증 토큰은 빈칸일 수 없습니다.")
            String verificationCode,
            @NotNull(message = "휴대전화 번호는 필수입니다.")
            @NotBlank(message = "휴대전화 번호는 빈칸일 수 없습니다.")
            String phoneNumber,
            @NotNull(message = "신규 간편 비밀번호는 필수입니다.")
            @NotBlank(message = "신규 간편 비밀번호는 빈칸일 수 없습니다.")
            @Schema(description = "AES 암호화된 새 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
            String newPassword
    ){}
}
