package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

public interface ExchangeApiClient {
    MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType);
    
    // 주문 가능 여부 확인
   
    void checkOrderAvailability(
            String phoneNumber,
            ExchangeType exchangeType,
            String market,
            String side,
            String orderType,
            String price,
            String volume //주문 수량
    );
}