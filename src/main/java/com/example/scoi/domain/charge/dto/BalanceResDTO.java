package com.example.scoi.domain.charge.dto;

public class BalanceResDTO {

    //보유자산 조회
    public record BalanceDTO(
            String currency,
            String balance,
            String locked
    ) {}
}
