package com.example.scoi.domain.member.converter;

import com.example.scoi.domain.member.dto.MemberResDTO;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberApiKey;
import com.example.scoi.domain.member.entity.MemberFcm;
import com.example.scoi.domain.member.enums.ExchangeType;

public class MemberConverter {

    // 내 정보 조회
    // Member -> MemberInfo
    public static MemberResDTO.MemberInfo toMemberInfo(
            Member member
    ){
        return MemberResDTO.MemberInfo.builder()
                .memberId(member.getId())
                .koreanName(member.getKoreanName())
                .englishName(member.getEnglishName())
                .residentNumber(member.getResidentNumber())
                .phoneNumber(member.getPhoneNumber())
                .memberType(member.getMemberType())
                .profileImageUrl(member.getProfileImageUrl())
                .isBioRegistered(member.getIsBioRegistered())
                .createdAt(member.getCreatedAt())
                .build();
    }

    // 거래소 목록 조회
    public static MemberResDTO.ExchangeList toExchangeList(
            String exchangeType,
            Boolean isLinked
    ){
        return MemberResDTO.ExchangeList.builder()
                .exchangeType(exchangeType)
                .isLinked(isLinked)
                .build();
    }

    // API키 목록 조회
    public static MemberResDTO.ApiKeyList toApiKeyList(
            MemberApiKey memberApiKey
    ){
        return MemberResDTO.ApiKeyList.builder()
                .exchangeType(memberApiKey.getExchangeType())
                .publicKey(memberApiKey.getPublicKey())
                .secretKey(memberApiKey.getSecretKey())
                .build();
    }

    // MemberApiKey
    public static MemberApiKey toMemberApiKey(
            ExchangeType exchangeType,
            String publicKey,
            String secretKey,
            Member member
    ){
        return MemberApiKey.builder()
                .exchangeType(exchangeType)
                .publicKey(publicKey)
                .secretKey(secretKey)
                .member(member)
                .build();
    }

    // MemberFcm
    public static MemberFcm toMemberFcm(
            String fcmToken,
            Member member
    ){
        return MemberFcm.builder()
                .fcmToken(fcmToken)
                .member(member)
                .build();
    }
}
