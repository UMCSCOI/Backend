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

    // 업비트 계정 잔고 조회 응답 -> BalanceDTO 리스트로 변환
    public static List<BalanceResDTO.BalanceDTO> toBalanceDTOList(@NotNull UpbitResDTO.BalanceResponse[] responses) {
        List<BalanceResDTO.BalanceDTO> result = new ArrayList<>();
        
        for (UpbitResDTO.BalanceResponse response : responses) {
            try {
                // 잔고가 0보다 큰 자산만 추가
                double balance = Double.parseDouble(response.getBalance());
                double locked = Double.parseDouble(response.getLocked());
                
                if (balance > 0 || locked > 0) {
                    result.add(BalanceResDTO.BalanceDTO.builder()
                            .currency(response.getCurrency())
                            .balance(response.getBalance())
                            .locked(response.getLocked())
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
