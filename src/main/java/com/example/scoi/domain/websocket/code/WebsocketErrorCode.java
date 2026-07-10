package com.example.scoi.domain.websocket.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WebsocketErrorCode implements BaseErrorCode {

    UPBIT_WEBSOCKET_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "WEBSOCKET500_1",
            "웹소켓 연결 도중 에러가 발생했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
