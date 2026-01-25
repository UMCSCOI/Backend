package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;

/**
 * 빗썸 API 응답을 ChargeResDTO로 변환하는 Converter
 * 
 * 역할:
 * - 빗썸 API의 원본 응답 형식을 표준화된 ChargeResDTO.BalanceDTO로 변환
 * - 거래소별 응답 형식 차이를 여기서 흡수
 * - Adapter에서 호출하여 사용
 */
public class BithumbConverter {

    // 빗썸 전체 계좌 조회 -> BalanceDTO

    public static ChargeResDTO.BalanceDTO toBalanceDTO(BithumbResDTO.BalanceResponse[] responses) {
 
        if (responses != null) {
            for (BithumbResDTO.BalanceResponse response : responses) {
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