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
            "성공적으로 요청을 처리했습니다."),

    ORDER_AVAILABLE(HttpStatus.OK,
            "INVEST200_2",
            "주문이 가능합니다."),

    ORDER_UNAVAILABLE(HttpStatus.OK,
            "INVEST200_3",
            "주문이 불가능합니다. 잔고 또는 수량이 부족합니다."),

    ORDER_SUCCESS(HttpStatus.OK,
            "INVEST200_4",
            "주문이 성공적으로 생성되었습니다."),//확인 필요요

    ORDER_CANCEL_SUCCESS(HttpStatus.OK,
            "INVEST200_5",
            "성공적으로 주문을 취소했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
