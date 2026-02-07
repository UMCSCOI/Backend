package com.example.scoi.domain.charge.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

import java.util.Map;

public class ChargeException extends ScoiException {
    public ChargeException(BaseErrorCode code) {
        super(code);
    }

    public ChargeException(BaseErrorCode code, Map<String, String> bind){
        super(code,bind);
    }
}
