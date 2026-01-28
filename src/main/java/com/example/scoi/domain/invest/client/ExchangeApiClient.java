package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

//여러 거래소 어댑터들을 한 타입으로 묶기 위한 인터페이스
public interface ExchangeApiClient {
    MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String price);
    
    // 주문 가능 여부 확인 (주문 가능하면 정상 반환, 불가능하면 예외)
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