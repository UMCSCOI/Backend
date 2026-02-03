package com.example.scoi.domain.invest.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InvestErrorCode implements BaseErrorCode {

    // 도메인별 실패 코드 정의
     // 400 에러
    EXCHANGE_API_ERROR(HttpStatus.BAD_REQUEST,
            "INVEST400_1",
            "거래소에 요청을 처리하지 못했습니다."),

    INVALID_EXCHANGE_TYPE(HttpStatus.BAD_REQUEST,
            "INVEST400_2",
            "잘못된 거래소 타입입니다."),

 INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST,
            "INVEST400_3",
            "계좌에 잔고가 부족합니다."),

    INSUFFICIENT_COIN_AMOUNT(HttpStatus.BAD_REQUEST,
            "INVEST400_4",
            "보유 수량을 초과할 수 없습니다."),

    // 401 에러
    INSUFFICIENT_API_PERMISSION(HttpStatus.UNAUTHORIZED,
            "INVEST401_1",
            "거래소의 API키의 권한이 부족합니다."),

    // 404 에러
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "INVEST404_1",
            "거래소의 퍼블릭 키, 시크릿 키가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
