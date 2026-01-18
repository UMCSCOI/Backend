package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class InvestResDTO {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO {
        private String uuid;          // 주문 UUID
        private String txid;          // 주문 TXID -거래소별 체크크
        private String market;        // 마켓 타입
        private String side;          // 주문 타입 (bid/ask)
        private String orderType;     // 주문 방식 (limit/price/market)
        private LocalDateTime createdAt; // 주문 생성 시간
    }
}
