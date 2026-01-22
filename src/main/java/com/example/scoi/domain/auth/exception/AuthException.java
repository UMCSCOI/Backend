package com.example.scoi.domain.auth.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class AuthException extends ScoiException {
    public AuthException(BaseErrorCode code) {
        super(code);
    }
}