package com.example.scoi.domain.invest.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InvestSuccessCode implements BaseSuccessCode {
    
    MAX_ORDER_INFO_SUCCESS(HttpStatus.OK,
            "INVEST200_1",
            "성공적으로 요청을 처리했습니다.");
            
    ORDER_AVAILABLE(HttpStatus.OK,
            "INVEST200_2",
            "주문이 가능합니다.");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
}