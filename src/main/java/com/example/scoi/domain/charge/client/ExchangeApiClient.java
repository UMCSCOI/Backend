package com.example.scoi.domain.charge.client;

import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

public interface ExchangeApiClient {
    ChargeResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType);
}