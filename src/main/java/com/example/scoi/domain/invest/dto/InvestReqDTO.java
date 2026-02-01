package com.example.scoi.domain.invest.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class InvestReqDTO {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO {
        private ExchangeType exchangeType;  
        private String market;        
        private String side;          // bid: 매수, ask: 매도
        private String orderType;     // limit: 지정가 매수/매도, price: 시장가 매수, market: 시장가 매도
        private String price;          
        private String volume;         
    }

}
