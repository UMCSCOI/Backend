package com.example.scoi.domain.charge.dto;

import com.example.scoi.domain.charge.enums.DepositType;
import com.example.scoi.domain.charge.enums.MFAType;
import com.example.scoi.domain.member.enums.ExchangeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ChargeReqDTO {

    // 원화 입금
    public record ChargeKrw(
            @NotNull(message = "거래소 타입은 필수입니다. (BITHUMB, UPBIT)")
            ExchangeType exchangeType,
            @NotNull(message = "충전할 금액은 필수입니다.")
            Long amount,
            @NotNull(message = "인증서 타입은 필수입니다. (KAKAO, NAVER, HANA)")
            MFAType MFA
    ){}

    // 특정 주문 확인하기
    public record GetOrder(
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeType,
            @NotNull(message = "UUID는 필수입니다.")
            @NotBlank(message = "UUID가 빈칸일 수 없습니다.")
            String uuid,
            @NotNull(message = "거래 타입은 필수입니다.")
            DepositType depositType
    ){}

    // 입금 주소 생성하기
    public record CreateDepositAddress(
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeType,
            @NotNull(message = "코인 타입은 필수입니다.")
            List<String> coinType,
            @NotNull(message = "네트워크 타입은 필수입니다.")
            List<String> netType
    ){}
}
