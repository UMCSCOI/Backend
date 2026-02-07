package com.example.scoi.global.client.dto;

import lombok.Builder;

public class BithumbReqDTO {

    // 원화 입금
    @Builder
    public record ChargeKrw(
            String amount,
            String two_factor_type
    ){}

    // 입금 주소 생성 요청 & JWT 테스트용
    @Builder
    public record CreateDepositAddress(
            String currency,
            String net_type
    ){}
}
