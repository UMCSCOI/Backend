package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestReqDTO {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO {
        private String exchangeType;  // 거래소 타입 (Bithumb/Upbit/Binance)
        private String market;        // 마켓 타입 (ex. KRW-BTC)
        private String side;          // 주문 타입 (bid: 매수, ask: 매도)
        private String orderType;     // 주문 방식 (limit: 지정가, price: 시장가 매수, market: 시장가 매도)
        private String price;         // 주문 가격 (지정가 주문 -> 호가, 시장가 -> 매수 총액)(매수시 필수)
        private String volume;        // 주문 수량 (매도시 필수)
        private String password;      // 간편 비밀번호
    }
}
