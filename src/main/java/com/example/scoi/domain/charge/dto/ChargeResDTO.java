package com.example.scoi.domain.charge.dto;

import lombok.Builder;

public class ChargeResDTO {

    // 원화 충전 요청하기
    @Builder
    public record ChargeKrw(
            String currency,
            String uuid,
            String txid
    ){}
}
