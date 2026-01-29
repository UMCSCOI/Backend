package com.example.scoi.global.client.converter;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.global.client.dto.BithumbReqDTO;

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
}
