package com.example.scoi.domain.invest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaxOrderInfoDTO {
    private String balance;      // 보유 자산 
    private String maxQuantity; // 최대 주문 가능 수량
}
