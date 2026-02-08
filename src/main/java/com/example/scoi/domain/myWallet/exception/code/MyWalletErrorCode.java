package com.example.scoi.domain.myWallet.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyWalletErrorCode implements BaseErrorCode {

    // 400 에러
    EXCHANGE_API_ERROR(HttpStatus.BAD_REQUEST,
            "MYWALLET400_1",
            "거래소 API 요청을 처리하지 못했습니다."),

    INVALID_EXCHANGE_TYPE(HttpStatus.BAD_REQUEST,
            "MYWALLET400_2",
            "잘못된 거래소 타입입니다."),

    INVALID_ORDER_PARAM(HttpStatus.BAD_REQUEST,
            "MYWALLET400_3",
            "정렬 순서는 'desc'(최신순) 또는 'asc'(과거순)만 가능합니다."),

    INVALID_DETAIL_PARAM(HttpStatus.BAD_REQUEST,
            "MYWALLET400_4",
            "입출금 상세 조회 시 remitType(DEPOSIT 또는 WITHDRAW)은 필수입니다."),

    INVALID_UUID_PARAM(HttpStatus.BAD_REQUEST,
            "MYWALLET400_5",
            "uuid는 필수 파라미터입니다."),

    // 401 에러
    INSUFFICIENT_API_PERMISSION(HttpStatus.UNAUTHORIZED,
            "MYWALLET401_1",
            "거래소 API 키의 권한이 부족합니다."),

    // 404 에러
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MYWALLET404_1",
            "거래소 API 키가 등록되어 있지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MYWALLET404_2",
            "사용자를 찾을 수 없습니다."),

    // 429 에러
    EXCHANGE_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS,
            "MYWALLET429_1",
            "거래소 API 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // 500 에러
    EXCHANGE_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "MYWALLET500_1",
            "거래소 서버에 일시적인 오류가 발생했습니다."),

    EXCHANGE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,
            "MYWALLET504_1",
            "거래소 API 응답 시간이 초과되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
