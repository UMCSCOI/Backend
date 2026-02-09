package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.global.client.dto.UpbitReqDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

// 업비트 API 응답 -> BalanceResDTO

public class UpbitConverter {

    // 원화 입금
    public static UpbitReqDTO.ChargeKrw toChargeKrw(
            ChargeReqDTO.ChargeKrw dto
    ){
        return UpbitReqDTO.ChargeKrw.builder()
                .amount(dto.amount().toString())
                .two_factor_type(dto.MFA().name().toLowerCase())
                .build();
    }

    // 입금 주소 생성 요청
    public static UpbitReqDTO.CreateDepositAddress toCreateDepositAddress(
            String currency,
            String netType
    ){
        return UpbitReqDTO.CreateDepositAddress.builder()
                .currency(currency)
                .net_type(netType)
                .build();
    }

    // 업비트 계정 잔고 조회 응답 -> BalanceDTO 리스트로 변환
    // USDC, USDT, KRW만 반환
    public static List<BalanceResDTO.BalanceDTO> toBalanceDTOList(@NotNull UpbitResDTO.BalanceResponse[] responses) {
        List<BalanceResDTO.BalanceDTO> result = new ArrayList<>();
        
        // 허용된 통화 목록
        List<String> allowedCurrencies = List.of("USDC", "USDT", "KRW");

        for (UpbitResDTO.BalanceResponse response : responses) {
            try {
                // USDC, USDT, KRW만 필터링
                String currency = response.currency();
                if (!allowedCurrencies.contains(currency)) {
                    continue;
                }

                // 잔고가 0보다 큰 자산만 추가
                double balance = Double.parseDouble(response.balance());
                double locked = Double.parseDouble(response.locked());
                
                if (balance > 0 || locked > 0) {
                    result.add(BalanceResDTO.BalanceDTO.builder()
                            .currency(currency)
                            .balance(response.balance())
                            .locked(response.locked())
                            .build());
                }
            } catch (NumberFormatException e) {
                // 파싱 실패 시 해당 자산은 제외
                continue;
            }
        }
        
        return result;
    }
}
