package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import lombok.Builder;

public class MemberReqDTO {

    // 휴대폰 번호 변경
    public record ChangePhone(
            String phoneNumber
    ){}

    // 간편 비밀번호 변경
    public record ChangePassword(
            String oldPassword,
            String newPassword
    ){}

    // 간편 비밀번호 재설정
    public record ResetPassword(
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

    // JWTAPIUtil 테스트
    @Builder
    public record Test(
            String currency,
            String net_type
    ){}
}
