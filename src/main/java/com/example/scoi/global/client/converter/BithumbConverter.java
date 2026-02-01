package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.global.client.dto.BithumbReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import jakarta.validation.constraints.NotNull;

/**
 * 빗썸 API 응답을 BalanceResDTO로 변환하는 Converter
 *
 * 역할:
 * - 빗썸 API의 원본 응답 형식을 표준화된 BalanceResDTO.BalanceDTO로 변환
 * - 거래소별 응답 형식 차이를 여기서 흡수
 * - Adapter에서 호출하여 사용
 */
public class BithumbConverter {

    // 원화 입금
    public static BithumbReqDTO.ChargeKrw toChargeKrw(
            ChargeReqDTO.ChargeKrw dto
    ){
        return BithumbReqDTO.ChargeKrw.builder()
                .amount(dto.amount().toString())
                .two_factor_type(dto.MFA().name())
                .build();
    }

    // 빗썸 전체 계좌 조회 -> BalanceDTO
    public static BalanceResDTO.BalanceDTO toBalanceDTO(@NotNull BithumbResDTO.BalanceResponse[] responses) {
        for (BithumbResDTO.BalanceResponse response : responses) {
            if ("KRW".equals(response.getCurrency())) {
                return BalanceResDTO.BalanceDTO.builder()
                        .currency(response.getCurrency())
                        .balance(response.getBalance())
                        .locked(response.getLocked())
                        .build();
            }
        }

        // KRW가 없으면 빈 값 반환
        return BalanceResDTO.BalanceDTO.builder()
                .currency("KRW")
                .balance("0")
                .locked("0")
                .build();
    }
}
