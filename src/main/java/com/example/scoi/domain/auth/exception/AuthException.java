package com.example.scoi.domain.auth.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

import java.util.Map;

public class AuthException extends ScoiException {
    public AuthException(BaseErrorCode code) {
        super(code);
    }

    public AuthException(BaseErrorCode code, Map<String, String> bind) {
        super(code, bind);
    }
}