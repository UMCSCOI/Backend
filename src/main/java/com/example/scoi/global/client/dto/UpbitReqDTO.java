package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

public class UpbitReqDTO {

    // 원화 입금
    @Builder
    public record ChargeKrw(
            String amount,
            String two_factor_type
    ){}

    // 주문 생성
    // 업비트 API는 null 필드를 제외하고 query string을 생성하므로, JSON 직렬화 시 null 필드를 제외하도록 설정
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateOrder(
            String market,
            String side,      // bid: 매수, ask: 매도
            String ord_type,  // limit: 지정가, price: 시장가 매수, market: 시장가 매도
            String price,     // 지정가 주문 시 필수
            String volume     // 주문 수량
    ){}
}
