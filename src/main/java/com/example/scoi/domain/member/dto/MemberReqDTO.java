package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import jakarta.validation.constraints.Pattern;

public class MemberReqDTO {

    // 간편 비밀번호 변경
    public record ChangePassword(
            String oldPassword,
            String newPassword
    ){}

    // 간편 비밀번호 재설정
    public record ResetPassword(
            @Pattern(regexp = "^\\d{11}$")
            String phoneNumber,
            String newPassword
    ){}

    // API키 등록 및 수정
    public record PostPatchApiKey(
            ExchangeType exchangeType,
            String publicKey,
            String secretKey
    ){}

    // API키 삭제
    public record DeleteApiKey(
            ExchangeType exchangeType
    ){}

    // FCM 토큰 등록
    public record PostFcmToken(
            String token
    ){}
}
