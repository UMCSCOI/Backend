package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

public interface ExchangeApiClient {
    MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType);
}