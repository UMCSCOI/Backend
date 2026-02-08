package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

public class BithumbReqDTO {

    // 원화 입금
    @Builder
    public record ChargeKrw(
            String amount,
            String two_factor_type
    ){}

    // 입금 주소 생성 요청 & JWT 테스트용
    @Builder
    public record CreateDepositAddress(
            String currency,
            String net_type
    ){}

    // 주문 생성
    // @JsonInclude(NON_NULL): null 필드는 JSON 직렬화 시 자동으로 제외됨
    // - 시장가 매수(ord_type: "price")일 때: volume을 null로 설정하여 제외
    // - 시장가 매도(ord_type: "market")일 때: price를 null로 설정하여 제외
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateOrder(
            String market,
            String side,       // bid: 매수, ask: 매도
            @JsonProperty("ord_type") String order_type, // limit: 지정가, price: 시장가 주문(매수), market: 시장가 주문(매도)
            String price,      // 주문 가격 (지정가, 시장가 매수 시 필수, 시장가 매도 시 null)
            String volume      // 주문량 (지정가, 시장가 매도 시 필수, 시장가 매수 시 null)
    ){}
}
