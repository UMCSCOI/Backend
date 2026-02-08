package com.example.scoi.domain.myWallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 거래소 계좌 잔고 조회 API 원본 응답 DTO
 * GET /v1/accounts
 */
public class BalanceClientDTO {

    /**
     * 빗썸/업비트 계좌 정보
     * 두 거래소 모두 동일한 응답 구조
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountInfo(
            String currency,        // 화폐 코드 (USDT, USDC 등)
            String balance,         // 주문 가능 수량
            String locked           // 주문 중 묶여있는 수량
    ) {}
}
