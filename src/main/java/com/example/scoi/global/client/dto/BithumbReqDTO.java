package com.example.scoi.global.client.dto;

import lombok.Builder;

public class BithumbReqDTO {

    // 원화 입금
    @Builder
    public record ChargeKrw(
            String amount,
            String two_factor_type
    ){}
}
