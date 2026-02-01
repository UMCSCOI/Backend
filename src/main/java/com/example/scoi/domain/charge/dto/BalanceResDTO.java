package com.example.scoi.domain.charge.dto;

import lombok.Builder;

public class BalanceResDTO {

    //보유자산 조회
    @Builder
    public record BalanceDTO(
            String currency,
            String balance,
            String locked
    ) {}
}
