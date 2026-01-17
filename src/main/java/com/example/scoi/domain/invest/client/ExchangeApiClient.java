package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

public interface ExchangeApiClient {
    InvestResDTO.MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType);
}