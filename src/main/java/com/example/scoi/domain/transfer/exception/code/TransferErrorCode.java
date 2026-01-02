package com.example.scoi.domain.transfer.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TransferErrorCode implements BaseErrorCode {

    // 도메인별 실패 코드 정의
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
