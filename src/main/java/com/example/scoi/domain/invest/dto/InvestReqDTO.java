package com.example.scoi.domain.invest.dto;

import com.example.scoi.domain.member.enums.ExchangeType;

public class InvestReqDTO {

    // 주문 요청 (실제 주문 생성용)
    public record OrderDTO(
            ExchangeType exchangeType,  // 거래소 타입 (Bithumb/Upbit/Binance)
            String market,        // 마켓 타입 (ex. KRW-BTC)
            String side,          // 주문 타입 (bid: 매수, ask: 매도)
            String orderType,     // 주문 방식 (limit: 지정가, price: 시장가 매수, market: 시장가 매도)
            String price,         // 주문 가격 (지정가 주문 -> 호가, 시장가 -> 매수 총액)(매수시 필수)
            String volume,        // 주문 수량 (매도시 필수)
            String password       // 간편 비밀번호 (실제 주문 생성 시 필수)
    ) {}

    // 주문 테스트 요청 (주문 생성 테스트용, password 불필요)
    public record TestOrderDTO(
            ExchangeType exchangeType,  // 거래소 타입 (Bithumb/Upbit/Binance)
            String market,        // 마켓 타입 (ex. KRW-BTC)
            String side,          // 주문 타입 (bid: 매수, ask: 매도)
            String orderType,     // 주문 방식 (limit: 지정가, price: 시장가 매수, market: 시장가 매도)
            String price,         // 주문 가격 (지정가 주문 -> 호가, 시장가 -> 매수 총액)(매수시 필수)
            String volume         // 주문 수량 (매도시 필수)
    ) {}

    // 주문 취소 요청
    public record CancelOrderDTO(
            ExchangeType exchangeType,  // 거래소 타입 (BITHUMB/UPBIT)
            String uuid,                // 주문 UUID
            String txid                 // 주문 TXID (선택적)
    ) {}
}
