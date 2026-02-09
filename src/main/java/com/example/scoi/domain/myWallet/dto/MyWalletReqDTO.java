package com.example.scoi.domain.myWallet.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.myWallet.enums.MFAType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MyWalletReqDTO {

    /**
     * 원화 출금 요청 DTO
     */
    public record WithdrawKrwRequest(
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeType,

            @NotNull(message = "출금 금액은 필수입니다.")
            @Min(value = 1, message = "출금 금액은 1 이상이어야 합니다.")
            Long amount,

            @NotNull(message = "2차 인증 수단은 필수입니다.")
            MFAType MFA
    ) {}
}
