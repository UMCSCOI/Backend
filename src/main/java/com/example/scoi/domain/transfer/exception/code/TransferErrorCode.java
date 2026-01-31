package com.example.scoi.domain.transfer.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TransferErrorCode implements BaseErrorCode {

    // 400
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "TRANSFER400_1", "cursor가 올바르지 않습니다."),
    EXIST_FAVORITE_RECIPIENT(HttpStatus.BAD_REQUEST, "TRANSFER400_2", "이미 즐겨찾기로 등록된 사용자입니다."),
    INVALID_RECIPIENT_INFORMATION(HttpStatus.BAD_REQUEST, "TRANSFER400_3", "수취인 입력 정보가 부족합니다."),
    INVALID_WALLET_ADDRESS(HttpStatus.BAD_REQUEST, "TRANSFER400_4", "지갑 주소 형식이 올바르지 않습니다."),
    INVALID_NETWORK_TYPE(HttpStatus.BAD_REQUEST, "TRANSFER400_5", "네트워크 타입이 잘못되었습니다."),
    EXCHANGE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "TRANSFER400_6", "거래소에서 요청을 처리하지 못했습니다."),
    UNSUPPORTED_EXCHANGE(HttpStatus.BAD_REQUEST, "TRANSFER400_7", "지원하지 않는 거래소입니다."),

    // 403
    EXCHANGE_FORBIDDEN(HttpStatus.FORBIDDEN, "TRANSFER403_1", "거래소의 API 키 권한이 부족합니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSFER404_1", "memberId에 해당하는 사용자를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
