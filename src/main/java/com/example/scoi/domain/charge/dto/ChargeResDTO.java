package com.example.scoi.domain.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChargeResDTO {
    // 응답 DTO 정의

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceDTO {
        private String currency;  
        private String balance;  
        private String locked;    // 출금이나 주문을 못하는 잔액
    }

}
