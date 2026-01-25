package com.example.scoi.domain.member.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import lombok.Builder;

import java.time.LocalDateTime;

public class MemberResDTO {

    // 내 정보 조회
    @Builder
    public record MemberInfo(
            Long memberId,
            String koreanName,
            String englishName,
            String residentNumber,
            String phoneNumber,
            MemberType memberType,
            String profileImageUrl,
            Boolean isBioRegistered,
            LocalDateTime createdAt
    ){}

    // 휴대폰 번호 변경
    @Builder
    public record ChangePhone(
            String phoneNumber
    ){}

    // 거래소 목록 조회
    @Builder
    public record ExchangeList(
            String exchangeType,
            Boolean isLinked
    ){}

    // API키 목록 조회
    @Builder
    public record ApiKeyList(
            ExchangeType exchangeType,
            String publicKey,
            String secretKey
    ){}
}
