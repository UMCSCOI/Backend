package com.example.scoi.domain.charge.dto;

import lombok.Builder;

import java.util.List;

public class BalanceResDTO {

    //보유자산 조회
    @Builder
    public record BalanceDTO(
            String currency,
            String balance,
            String locked
    ) {}

    //보유자산 목록 조회 응답
    @Builder
    public record BalanceListDTO(
            List<BalanceDTO> balances
    ) {}
}
