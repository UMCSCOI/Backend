package com.example.scoi.domain.transfer.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.transfer.enums.TradeType;
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

        private TradeType tradeType;
        private ExchangeType exchangeType;

        private String network;

        private Boolean isFavorite;
    }
}
