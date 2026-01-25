package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 업비트 API 응답 DTO
 * 
 * 역할:
 * - 업비트 API의 원본 응답 형식을 그대로 받아오는 DTO
 * - 공식 문서의 모든 필드를 포함
 * - Converter에서 이 DTO를 ChargeResDTO.BalanceDTO로 변환
 */
public class UpbitResDTO {

    /**
     * 업비트 계정 잔고 조회 응답 (배열)
     * 
     * 공식 문서: https://docs.upbit.com/kr/reference/get-balance
     * 엔드포인트: GET /v1/accounts
     * 
     * 공식 문서의 모든 필드를 포함:
     * - currency: 화폐를 의미하는 영문 대문자 코드
     * - balance: 주문가능 금액/수량
     * - locked: 주문 중 묶여있는 금액/수량
     * - avg_buy_price: 매수평균가
     * - avg_buy_price_modified: 매수평균가 수정 여부
     * - unit_currency: 평단가 기준 화폐
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {
        
        private String currency;  // 화폐를 의미하는 영문 대문자 코드
        
        private String balance;  // 주문가능 금액/수량
        
        private String locked;  // 주문 중 묶여있는 금액/수량
        
        @JsonProperty("avg_buy_price")
        private String avgBuyPrice;  // 매수평균가
        
        @JsonProperty("avg_buy_price_modified")
        private Boolean avgBuyPriceModified;  // 매수평균가 수정 여부
        
        @JsonProperty("unit_currency")
        private String unitCurrency;  // 평단가 기준 화폐
    }
}
