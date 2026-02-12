package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MemberReqDTO {

    // 간편 비밀번호 변경
    public record ChangePassword(
            @NotNull(message = "기존 간편 비밀번호는 필수입니다.")
            @NotBlank(message = "기존 간편 비밀번호는 빈칸일 수 없습니다.")
            String oldPassword,
            @NotNull(message = "신규 간편 비밀번호는 필수입니다.")
            @NotBlank(message = "신규 간편 비밀번호는 빈칸일 수 없습니다.")
            String newPassword
    ){}

    // 간편 비밀번호 재설정
    public record ResetPassword(
            @NotNull(message = "SMS 인증 토큰은 필수입니다.")
            @NotBlank(message = "SMS 인증 토큰은 빈칸일 수 없습니다.")
            String verificationCode,
            @NotNull(message = "신규 간편 비밀번호는 필수입니다.")
            @NotBlank(message = "신규 간편 비밀번호는 빈칸일 수 없습니다.")
            @Schema(description = "AES 암호화된 새 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
            String newPassword
    ){}

    // API키 등록 및 수정
    public record PostPatchApiKey(
            @NotNull(message = "거래소 타입은 빈칸일 수 없습니다.")
            ExchangeType exchangeType,
            @NotNull(message = "거래소 퍼블릭 키는 필수입니다.")
            @NotBlank(message = "거래소 퍼블릭 키는 빈칸일 수 없습니다.")
            String publicKey,
            @NotNull(message = "거래소 시크릿 키는 필수입니다.")
            @NotBlank(message = "거래소 시크릿 키는 빈칸일 수 없습니다.")
            String secretKey
    ){}

    // API키 삭제
    public record DeleteApiKey(
            @NotNull(message = "거래소 타입은 빈칸일 수 없습니다.")
            ExchangeType exchangeType
    ){}

    // FCM 토큰 등록
    public record PostFcmToken(
            @NotNull(message = "FCM 토큰은 필수입니다.")
            @NotBlank(message = "FCM 토큰은 빈칸일 수 없습니다.")
            String token
    ){}
}
