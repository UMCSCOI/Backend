package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;

// 업비트 API 응답 ->  ChargeResDTO
 
public class UpbitConverter {

    // 업비트 계정 잔고 조회 응답 -> BalanceDTO로 변환
     

    public static ChargeResDTO.BalanceDTO toBalanceDTO(UpbitResDTO.BalanceResponse[] responses) {
        // 필요 시 BTC, USDT 등 다른 화폐도 추가 가능
        if (responses != null) {
            for (UpbitResDTO.BalanceResponse response : responses) {
                if ("KRW".equals(response.getCurrency())) {
                    return ChargeResDTO.BalanceDTO.builder()
                            .currency(response.getCurrency())
                            .balance(response.getBalance())
                            .locked(response.getLocked())
                            .build();
                }
            }
        }
        
        // KRW가 없으면 빈 값 반환
        return ChargeResDTO.BalanceDTO.builder()
                .currency("KRW")
                .balance("0")
                .locked("0")
                .build();
    }
}