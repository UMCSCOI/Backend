package com.example.scoi.domain.invest.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
public class InvestResDTO {
    // 응답 DTO 정의
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceDTO {
        private String currency; 
        private String balance;   
        private String locked;  

