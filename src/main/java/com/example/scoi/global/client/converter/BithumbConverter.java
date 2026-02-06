package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.global.client.dto.BithumbReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;


//빗썸 API 응답을 BalanceResDTO로 변환
public class BithumbConverter {

    // 원화 입금
    public static BithumbReqDTO.ChargeKrw toChargeKrw(
            ChargeReqDTO.ChargeKrw dto
    ){
        return BithumbReqDTO.ChargeKrw.builder()
                .amount(dto.amount().toString())
                .two_factor_type(dto.MFA().name().toLowerCase())
                .build();
    }

    // 빗썸 전체 계좌 조회 -> BalanceDTO 리스트
    public static List<BalanceResDTO.BalanceDTO> toBalanceDTOList(@NotNull BithumbResDTO.BalanceResponse[] responses) {
        List<BalanceResDTO.BalanceDTO> result = new ArrayList<>();
        
        for (BithumbResDTO.BalanceResponse response : responses) {
            try {
                // 잔고가 0보다 큰 자산만 추가
                double balance = Double.parseDouble(response.balance());
                double locked = Double.parseDouble(response.locked());
                
                if (balance > 0 || locked > 0) {
                    result.add(BalanceResDTO.BalanceDTO.builder()
                            .currency(response.currency())
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
