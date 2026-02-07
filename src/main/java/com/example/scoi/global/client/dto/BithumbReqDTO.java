package com.example.scoi.global.client.dto;

import lombok.Builder;

public class BithumbReqDTO {

    // 원화 입금
    @Builder
    public record ChargeKrw(
            String amount,
            String two_factor_type
    ){}

    // 주문 생성
    @Builder
    public record CreateOrder(
            String market,
            String side,      // bid: 매수, ask: 매도
            String ord_type,  // limit: 지정가, price: 시장가 매수, market: 시장가 매도
            String price,     // 지정가 주문 시 필수
            String volume     // 주문 수량
    ){}
}
