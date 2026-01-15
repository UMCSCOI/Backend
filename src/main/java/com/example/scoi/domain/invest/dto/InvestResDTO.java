package com.example.scoi.domain.invest.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
public class InvestResDTO {
    // 응답 DTO 정의

     // 보유 자산 조회 응답
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceDTO {
        private String coinType;           // 코인 타입
        private Long availableBalance;     // 사용 가능한 잔액
        private Long lockedBalance;        // 주문에 묶인 잔액
        private Long totalBalance;         // 총 잔액 (available + locked)
    }

    // 보유 자산 목록 조회 응답
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceListDTO {
        private List<BalanceDTO> balances;  // 잔액 목록
        private Integer totalCount;        // 총 개수
    }
}
