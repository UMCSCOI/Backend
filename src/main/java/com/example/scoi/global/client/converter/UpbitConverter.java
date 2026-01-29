package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.global.client.dto.UpbitReqDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;

// 업비트 API 응답 -> BalanceResDTO

public class UpbitConverter {

    // 원화 입금
    public static UpbitReqDTO.ChargeKrw toChargeKrw(
            ChargeReqDTO.ChargeKrw dto
    ){
        return UpbitReqDTO.ChargeKrw.builder()
                .amount(dto.amount().toString())
                .two_factor_type(dto.MFA().name())
                .build();
    }

    // 업비트 계정 잔고 조회 응답 -> BalanceDTO로 변환
    public static BalanceResDTO.BalanceDTO toBalanceDTO(UpbitResDTO.BalanceResponse[] responses) {
        // 필요 시 BTC, USDT 등 다른 화폐도 추가 가능
        if (responses != null) {
            for (UpbitResDTO.BalanceResponse response : responses) {
                if ("KRW".equals(response.getCurrency())) {
                    return new BalanceResDTO.BalanceDTO(
                            response.getCurrency(),
                            response.getBalance(),
                            response.getLocked()
                    );
                }
            }
        }

        // KRW가 없으면 빈 값 반환
        return new BalanceResDTO.BalanceDTO(
                "KRW",
                "0",
                "0"
        );
    }
}
