package com.example.scoi.domain.invest.dto;

import java.time.LocalDateTime;

public class InvestResDTO {

    // 주문 가능 여부 응답
    public record OrderAvailabilityResponse(
            Boolean canOrder,        // 주문 가능 여부
            String balance,          // 현재 잔고
            String requiredAmount,   // 주문에 필요한 금액 (매수 시) 또는 수량 (매도 시)
            String shortage          // 부족한 금액/수량 (주문 불가능한 경우)
    ) {}

    // 주문 응답
    public record OrderDTO(
            String uuid,            // 주문 UUID
            String txid,            // 주문 TXID - 거래소별 체크
            String market,          // 마켓 타입
            String side,            // 주문 타입 (bid/ask)
            String orderType,       // 주문 방식 (limit/price/market)
            LocalDateTime createdAt // 주문 생성 시간
    ) {}
}