package com.example.scoi.domain.charge.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChargeErrorCode implements BaseErrorCode {

    EXCHANGE_BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "CHARGE400_1",
            "거래소에 요청을 처리하지 못했습니다."),
    WRONG_EXCHANGE_TYPE(HttpStatus.BAD_REQUEST,
            "CHARGE400_2",
            "잘못된 거래소 타입입니다."),
    WRONG_DEPOSIT_TYPE(HttpStatus.BAD_REQUEST,
            "CHARGE400_3",
            "잘못된 입금 주문 타입입니다."),
    TWO_FACTOR_AUTH_REQUIRED(HttpStatus.BAD_REQUEST,
            "CHARGE400_3",
            "2차 인증이 필요한 작업입니다."),
    MINIMUM_DEPOSIT_BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "CHARGE400_4",
            "최소 5000원 이상 입금해주세요."),
    INVALIDED_TWO_FACTOR_AUTH(HttpStatus.BAD_REQUEST,
            "CHARGE400_5",
            "해당 거래소에선 지원하지 않는 인증서입니다."),
    EXCHANGE_FORBIDDEN(HttpStatus.FORBIDDEN,
            "CHARGE403_1",
            "거래소의 API키 권한이 부족합니다."),
    EXCHANGE_API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "CHARGE404_1",
            "거래소의 퍼블릭키, 시크릿키가 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "CHARGE404_2",
            "주문 내역을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
