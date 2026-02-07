package com.example.scoi.domain.invest.dto;

/**
 * 최대 주문 정보 DTO
 */
public record MaxOrderInfoDTO(
        String balance,      // 보유 자산 
        String maxQuantity  // 최대 주문 가능 수량
) {}
