package com.example.scoi.global.apiPayload.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ScoiException extends RuntimeException {

    private final BaseErrorCode code;

    public ScoiException(BaseErrorCode code) {
        this.code = code;
    }
}
