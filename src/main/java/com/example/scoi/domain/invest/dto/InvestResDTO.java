package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestResDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderAvailabilityResponse {
        private Boolean canOrder;
        private String balance;            // 현재 잔고
        private String requiredAmount;     // 주문에 필요한 금액 (매수 시) 또는 수량 (매도 시)
        private String shortage;           // 부족한 금액/수량 (주문 불가능한 경우)
    }
}