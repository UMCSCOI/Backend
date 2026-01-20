package com.example.scoi.domain.transfer.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TransferErrorCode implements BaseErrorCode {

    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "TRANSFER400_1", "cursor가 올바르지 않습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
