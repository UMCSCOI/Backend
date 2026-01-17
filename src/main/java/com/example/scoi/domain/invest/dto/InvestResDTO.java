package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestResDTO {
    // 응답 DTO 정의
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaxOrderInfoDTO {
        private String balance;  // 최대 주문 가능 수량/금액
    }
}
