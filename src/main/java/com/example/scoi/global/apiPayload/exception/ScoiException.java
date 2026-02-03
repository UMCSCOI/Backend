package com.example.scoi.global.apiPayload.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

import java.util.Map;

@Getter
public class ScoiException extends RuntimeException {

    private final BaseErrorCode code;
    private final Map<String, String> bind;

    public ScoiException(BaseErrorCode code) {
        this.code = code;
        this.bind = null;
    }

    public ScoiException(BaseErrorCode code, Map<String, String> bind) {
        this.code = code;
        this.bind = bind;
    }
}
