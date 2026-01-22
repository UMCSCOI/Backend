package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestReqDTO {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO {
        private String exchangeType;  
        private String market;        // 마켓 타입 
        private String side;          // 주문 타입 
        private String orderType;     // 주문 방식 (지정가, 시장가 매수, 시장가 매도)
        private String price;          
        private String volume;        // 주문 수량 
        private String password;      // 간편 비밀번호
    }

}
