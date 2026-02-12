package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
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
