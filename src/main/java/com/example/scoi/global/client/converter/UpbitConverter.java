package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.global.client.dto.UpbitReqDTO;

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
}
