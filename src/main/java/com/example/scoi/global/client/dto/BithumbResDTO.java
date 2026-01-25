package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 빗썸 API 응답 DTO
 * 
 * 역할:
 * - 빗썸 API의 원본 응답 형식을 그대로 받아오는 DTO
 * - 공식 문서의 모든 필드를 포함
 * - Converter에서 이 DTO를 ChargeResDTO.BalanceDTO로 변환
 */
public class BithumbResDTO {

    /**
     * 빗썸 전체 계좌 조회 응답 (배열)
     * 
     * 공식 문서: https://apidocs.bithumb.com/reference/전체-계좌-조회
     * 엔드포인트: GET /v1/accounts
     * 
     * 공식 문서 응답 형식 (Response 200):
     * array of objects
     * [
     *   {
     *     "currency": "KRW",
     *     "balance": "1000000",
     *     "locked": "50000",
     *     "avg_buy_price": "0",
     *     "avg_buy_price_modified": true,
     *     "unit_currency": "KRW"
     *   },
     *   ...
     * ]
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {
        
        private String currency; 
        
        private String balance;  
        
        private String locked;  
        
        @JsonProperty("avg_buy_price")
        private String avgBuyPrice;
        
        @JsonProperty("avg_buy_price_modified")
        private Boolean avgBuyPriceModified;  
        
        @JsonProperty("unit_currency")
        private String unitCurrency; 
    }
}
