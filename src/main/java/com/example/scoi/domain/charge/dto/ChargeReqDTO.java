package com.example.scoi.domain.charge.dto;

import com.example.scoi.domain.charge.enums.MFAType;
import com.example.scoi.domain.member.enums.ExchangeType;

public class ChargeReqDTO {

    // 원화 입금
    public record ChargeKrw(
            ExchangeType exchangeType,
            Long amount,
            MFAType MFA
    ){}

    // 특정 주문 확인하기
    public record GetOrder(
            ExchangeType exchangeType,
            String uuid
    ){}
}
