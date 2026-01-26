package com.example.scoi.domain.charge.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChargeSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "CHARGE200_1",
            "성공적으로 요청을 처리했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
