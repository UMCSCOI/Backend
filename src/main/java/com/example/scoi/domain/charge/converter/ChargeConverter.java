package com.example.scoi.domain.charge.converter;

import com.example.scoi.domain.charge.dto.ChargeResDTO;

public class ChargeConverter {

    // 원화 충전 요청하기
    public static ChargeResDTO.ChargeKrw toChargeKrw(
            String uuid,
            String txid
    ){
        return ChargeResDTO.ChargeKrw.builder()
                .currency("KRW")
                .uuid(uuid)
                .txid(txid)
                .build();
    }

    // 입금 주소 조회하기
    public static ChargeResDTO.GetDepositAddress toGetDepositAddress(
            String coinType,
            String address
    ){
        return ChargeResDTO.GetDepositAddress.builder()
                .address(address)
                .coinType(coinType)
                .build();
    }
}
