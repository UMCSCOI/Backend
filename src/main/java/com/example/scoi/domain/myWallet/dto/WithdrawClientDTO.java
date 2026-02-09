package com.example.scoi.domain.myWallet.dto;

import lombok.Builder;

/**
 * 거래소 원화 출금 API 호출용 DTO
 */
public class WithdrawClientDTO {

    /**
     * 거래소 원화 출금 요청 Body (POST /v1/withdraws/krw)
     * - amount: 출금 금액 (문자열)
     * - two_factor_type: 2차 인증 수단 (kakao, naver, hana)
     */
    @Builder
    public record WithdrawKrwRequest(
            String amount,
            String two_factor_type
    ) {}

    /**
     * 거래소 원화 출금 응답 (빗썸/업비트 공통)
     */
    @Builder
    public record WithdrawKrwResponse(
            String type,
            String uuid,
            String currency,
            String txid,
            String state,
            String created_at,
            String done_at,
            String amount,
            String fee,
            String transaction_type
    ) {}
}
