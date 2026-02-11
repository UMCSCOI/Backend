package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import io.swagger.v3.oas.annotations.media.Schema;
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
            String verificationToken,
            @Schema(description = "AES 암호화된 새 6자리 간편비밀번호 (Base64)", example = "6v4RsQ+gOGi1NtheSTiA1w==")
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
