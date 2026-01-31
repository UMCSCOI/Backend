package com.example.scoi.domain.transfer.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.transfer.enums.CoinType;
import lombok.*;

import java.util.List;

public class TransferResDTO {

    // 최근 수취인 목록 조회의 result 부분을 담당하는 DTO
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class RecipientListDTO {
        private List<RecipientDTO> items;
        private String nextCursor;
        private Boolean hasNext;
    }

    // items 리스트 안에 들어갈 개별 수취인 정보 DTO
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class RecipientDTO {
        private Long recipientId;

        private MemberType recipientType;

        private String recipientName;

        // 법인일 경우 필수, 개인일 경우 null
        private String corpKoreanName;
        private String corpEnglishName;

        private String walletAddress;

        private Boolean isFavorite;
    }

    // 수취인 정보 + 출금 가능 잔액
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class CheckRecipientResDTO {
        private RecipientDetailDTO recipient;
        private BalanceDTO balance;
    }

    // 상세 수취인 정보 (응답 구조에 맞춤)
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class RecipientDetailDTO {
        private MemberType recipientType;
        private String recipientName;
        private String corpKoreanName; // 법인일 경우 필수
        private String corpEnglishName; // 법인일 경우 필수
        private String walletAddress;
    }

    // 출금 가능 잔액 정보
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class BalanceDTO {
        private ExchangeType exchangeType;
        private CoinType coinType; // 예: "USDT"
        private String network;     // 예: "TRON"
        private String networkFee; // 네트워크 수수료
        private String availableAmount; // 출금 가능 금액
        private String updatedAt;   // 날짜 형식 문자열
    }

}
