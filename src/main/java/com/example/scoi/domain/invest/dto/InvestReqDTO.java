package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestReqDTO {
    // 요청 DTO 정의
    @Getter  
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckOrderAvailabilityDTO {
        private String exchangeType;  // 거래소 타입
        private String market;        // 마켓 타입
        private String side;          // 주문 타입 
        private String orderType;     // 주문 방식 
        private String price;         // 주문 가격 
        private String volume;        // 주문 수량 
    }

}
