package com.example.scoi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
            @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
            String verificationCode
    ) {}

    // 회원가입 요청
    public record SignupRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber,

            @NotBlank(message = "영문 이름은 필수입니다.")
            @Pattern(regexp = "^[A-Z ]+$", message = "영문 대문자와 공백만 입력 가능합니다.")
            @Size(max = 50, message = "영문 이름은 50자 이내입니다.")
            String englishName,

            @NotBlank(message = "한글 이름은 필수입니다.")
            @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다.")
            @Size(max = 5, message = "한글 이름은 5자 이내입니다.")
            String koreanName,

            @NotBlank(message = "주민등록번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}-\\d{7}$", message = "올바른 주민등록번호 형식이 아닙니다.")
            String residentNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "간편비밀번호는 6자리 숫자입니다.")
            String simplePassword
    ) {}

    // 로그인 요청
    public record LoginRequest(
            @NotBlank(message = "휴대폰 번호는 필수입니다.")
            @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
            String phoneNumber,

            @NotBlank(message = "간편비밀번호는 필수입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "간편비밀번호는 6자리 숫자입니다.")
            String simplePassword
    ) {}

    // 토큰 재발급 요청
    public record ReissueRequest(
            @NotBlank(message = "Refresh Token은 필수입니다.")
            String refreshToken
    ) {}
}