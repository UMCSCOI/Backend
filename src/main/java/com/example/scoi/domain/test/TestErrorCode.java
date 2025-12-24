package com.example.scoi.domain.test;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TestErrorCode implements BaseErrorCode {

    TEST(HttpStatus.BAD_REQUEST,
            "test",
            "test"),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
